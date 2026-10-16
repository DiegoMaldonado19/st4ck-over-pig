package com.dmaldonado.st4ck.over.pig.model.codegen;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The quadruples in RISC-V for RARS. Each one becomes a few instructions
 * that load its operands into scratch registers, operate and store the
 * result, under a comment with its three address code, so the assembly is
 * read side by side with the code it comes from.
 *
 * Memory: the parameters, variables and temporaries of a function live in
 * its activation record, relative to fp; the globals and H in .data; the
 * heap is a block taken with sbrk, its base in s11, and cell i is at
 * s11 + 4 * i. Every value takes one word: a decimalis is a 32 bit float.
 * Calls follow the RISC-V convention: arguments in a0-a7, the result in a0.
 */
public class RiscVEmitter
{
    /** RARS gives sbrk about 3.75 MiB: 3 MiB are 786 432 cells. */
    private static final int                 HEAP_BYTES  = 3 * 1024 * 1024;
    /** a0-a7 carry the arguments of a call. */
    private static final int                 ARGUMENTS   = 8;
    private static final String              HEAP        = "heap";
    private static final Pattern             INTEGER     = Pattern.compile("-?\\d+");
    private static final Pattern             DECIMAL     = Pattern.compile("-?\\d+\\.\\d*");
    private static final Map<String, String> INTEGER_OP  = Map.of("+", "add", "-", "sub", "*", "mul", "/", "div",
                                                                  "%", "rem");
    private static final Map<String, String> FLOAT_OP    = Map.of("+f", "fadd.s", "-f", "fsub.s", "*f", "fmul.s",
                                                                  "/f", "fdiv.s");
    /** The branch that skips the jump: a conditional branch reaches 4 KiB, j reaches 1 MiB. */
    private static final Map<String, String> SKIP_BRANCH = Map.of("<", "bge", "<=", "bgt", ">", "ble", ">=", "blt",
                                                                  "==", "bne", "!=", "beq");

    private static final int                 BUFFER_BYTES  = 1024;
    private static final String              READ_INT      = "_rt_read_int";
    private static final String              READ_CHAR     = "_rt_read_char";
    private static final String              READ_STRING   = "_rt_read_str";
    /** ReadString of one console line into _buffer, its '\n' included: how every read routine starts. */
    private static final String              READ_LINE     = """
                la   a0, _buffer
                li   a1, %d
                li   a7, 8
                ecall
            """.formatted(BUFFER_BYTES);
    /**
     * What each read routine does with the line in _buffer, the only code in
     * assembly: the system call works with bytes and the heap with a character
     * per cell. A line is read whole, so what is left of it never reaches the
     * next read. read_int takes what strtol would, and 0 when there is no
     * number: ReadInt would stop the program on "x".
     */
    private static final Map<String, String> READ_ROUTINES = Map.of(
            READ_STRING, """
                la   t0, _buffer
                la   t1, _H
                lw   t2, 0(t1)
                mv   a0, t2
                slli t3, t2, 2
                add  t3, t3, s11
            _rt_read_str_loop:
                lbu  t4, 0(t0)
                beqz t4, _rt_read_str_end
                li   t5, 10
                beq  t4, t5, _rt_read_str_end
                sw   t4, 0(t3)
                addi t0, t0, 1
                addi t3, t3, 4
                addi t2, t2, 1
                j    _rt_read_str_loop
            _rt_read_str_end:
                sw   zero, 0(t3)
                addi t2, t2, 1
                sw   t2, 0(t1)
                ret
            """,
            READ_CHAR, """
                la   t0, _buffer
                lbu  a0, 0(t0)
                li   t1, 10
                bne  a0, t1, _rt_read_char_end
                li   a0, 0
            _rt_read_char_end:
                ret
            """,
            READ_INT, """
                la   t0, _buffer
                li   a0, 0
                li   t2, 0
            _rt_read_int_blank:
                lbu  t1, 0(t0)
                li   t3, 32
                beq  t1, t3, _rt_read_int_skip
                li   t3, 9
                bne  t1, t3, _rt_read_int_sign
            _rt_read_int_skip:
                addi t0, t0, 1
                j    _rt_read_int_blank
            _rt_read_int_sign:
                li   t3, 45
                bne  t1, t3, _rt_read_int_plus
                li   t2, 1
                addi t0, t0, 1
                j    _rt_read_int_digit
            _rt_read_int_plus:
                li   t3, 43
                bne  t1, t3, _rt_read_int_digit
                addi t0, t0, 1
            _rt_read_int_digit:
                lbu  t1, 0(t0)
                addi t1, t1, -48
                bltz t1, _rt_read_int_done
                li   t3, 9
                bgt  t1, t3, _rt_read_int_done
                li   t3, 10
                mul  a0, a0, t3
                add  a0, a0, t1
                addi t0, t0, 1
                j    _rt_read_int_digit
            _rt_read_int_done:
                beqz t2, _rt_read_int_end
                neg  a0, a0
            _rt_read_int_end:
                ret
            """);

    private final StringBuilder        text      = new StringBuilder();
    /** Every distinct string literal and its first cell: cell 0 is null, which reads as "". */
    private final Map<String, Integer> pool      = new LinkedHashMap<>();
    /** The label of each global in .data, H among them. */
    private final Map<String, String>  globals   = new LinkedHashMap<>();
    private final Map<String, String>  functions = new HashMap<>();
    /** Where each value of the function being translated lives, relative to fp. */
    private final Map<String, Integer> offsets   = new HashMap<>();
    /** The read routines the program used: they go after its functions. */
    private final Set<String>          routines  = new LinkedHashSet<>();

    private int poolEnd = 1;
    private int skipCount;

    /** The params already loaded for the call that comes next. */
    private int    argumentCount;
    /** Where every return of the function being translated goes: its one epilogue. */
    private String exitLabel;

    public String emit(List<Quadruple> code)
    {
        globals.put("H", "_H");

        // Every label first: a call may come before the function it calls.
        for (Quadruple quadruple : code)
        {
            if ("global".equals(quadruple.op()))
            {
                globals.put(quadruple.result(), unique("g_" + sanitize(quadruple.result()), globals.values()));
            }
            else if ("func".equals(quadruple.op()))
            {
                functions.put(quadruple.result(), unique("fn_" + sanitize(quadruple.result()), functions.values()));
            }
        }

        int start = 0;

        for (int i = 0; i < code.size(); i++)
        {
            if ("func".equals(code.get(i).op()))
            {
                start = i;
            }
            else if ("end_func".equals(code.get(i).op()))
            {
                function(code.subList(start, i + 1));
            }
        }
        for (String routine : routines)
        {
            text.append('\n').append(routine).append(":\n").append(READ_LINE).append(READ_ROUTINES.get(routine));
        }
        // The pool is complete only now: the data and the startup go first in the file.
        return data() + startup() + text;
    }

    /**
     * One function. Its parameters arrive in a0-a7 and the prologue copies
     * them into its frame, so a call it makes can reuse those registers. Below
     * fp go ra, the caller's fp, and one slot for each parameter, variable and
     * temporary. Every return jumps to the one epilogue, at end_func.
     */
    private void function(List<Quadruple> body)
    {
        Quadruple    header     = body.get(0);
        List<String> parameters = header.arg1().isEmpty() ? List.of() : List.of(header.arg1().split(","));

        if (parameters.size() > ARGUMENTS)
        {
            throw tooManyArguments();
        }

        offsets.clear();
        for (String parameter : parameters)
        {
            offsets.put(parameter, -12 - 4 * offsets.size());
        }
        for (Quadruple quadruple : body)
        {
            for (String operand : values(quadruple))
            {
                if (isLocal(operand) && !offsets.containsKey(operand))
                {
                    offsets.put(operand, -12 - 4 * offsets.size());
                }
            }
        }
        exitLabel = "_exit_" + functions.get(header.result());

        text.append('\n');
        comment(header);
        label(functions.get(header.result()));
        instruction("sw   ra, -4(sp)");
        instruction("sw   fp, -8(sp)");
        instruction("mv   fp, sp");
        instruction("li   t6, " + (8 + 4 * offsets.size()));
        instruction("sub  sp, sp, t6");
        for (int i = 0; i < parameters.size(); i++)
        {
            store("a" + i, parameters.get(i));
        }

        for (Quadruple quadruple : body.subList(1, body.size()))
        {
            translate(quadruple);
        }
    }

    private void translate(Quadruple q)
    {
        String op = q.op();

        if ("label".equals(op))
        {
            label(q.result());
            return;
        }
        comment(q);

        switch (op)
        {
            case "goto"       -> instruction("j    " + q.result());
            case "="          ->
            {
                load("t0", q.arg1());
                store("t0", q.result());
            }
            case "=[]"        ->
            {
                load("t0", q.arg2());
                cell("t0");
                instruction("lw   t1, 0(t0)");
                store("t1", q.result());
            }
            case "[]="        ->
            {
                load("t0", q.arg1());
                load("t1", q.arg2());
                cell("t0");
                instruction("sw   t1, 0(t0)");
            }
            case "uminus"     -> unary(q, "neg  t2, t0");
            case "uminusf"    -> unary(q, "fmv.w.x ft0, t0", "fsgnjn.s ft1, ft0, ft0", "fmv.x.w t2, ft1");
            case "itof"       -> unary(q, "fcvt.s.w ft0, t0", "fmv.x.w t2, ft0");
            case "ftoi"       -> unary(q, "fmv.w.x ft0, t0", "fcvt.w.s t2, ft0, rtz");
            case "param"      -> argument(q.arg1());
            case "call"       ->
            {
                instruction("jal  ra, " + functions.get(q.arg1()));
                argumentCount = 0;
                if (!q.result().isEmpty())
                {
                    store("a0", q.result());
                }
            }
            case "return"     ->
            {
                if (!q.arg1().isEmpty())
                {
                    load("a0", q.arg1());
                }
                instruction("j    " + exitLabel);
            }
            case "end_func"   ->
            {
                label(exitLabel);
                epilogue();
            }
            case "print_int"  -> systemCall(q.arg1(), 1);
            case "print_char" -> systemCall(q.arg1(), 11);
            case "halt"       -> systemCall("", 10);
            case "read_int"   -> read(READ_INT, q.result());
            case "read_char"  -> read(READ_CHAR, q.result());
            case "read_str"   -> read(READ_STRING, q.result());
            case "read_float" ->
            {
                // ponytail: ReadFloat stops the program on a line that is not a number, unlike read_int.
                instruction("li   a7, 6");
                instruction("ecall");
                instruction("fmv.x.w t0, fa0");
                store("t0", q.result());
            }
            default           -> operation(q);
        }
    }

    /** jal to the read routine, which leaves the value in a0; a read alone drops it. */
    private void read(String routine, String result)
    {
        routines.add(routine);
        instruction("jal  ra, " + routine);
        if (!result.isEmpty())
        {
            store("a0", result);
        }
    }

    /** x = a op b, and the conditional jumps. */
    private void operation(Quadruple q)
    {
        String op = q.op();

        if (INTEGER_OP.containsKey(op))
        {
            load("t0", q.arg1());
            load("t1", q.arg2());
            instruction(INTEGER_OP.get(op) + " t2, t0, t1");
            store("t2", q.result());
        }
        else if (FLOAT_OP.containsKey(op))
        {
            load("t0", q.arg1());
            load("t1", q.arg2());
            instruction("fmv.w.x ft0, t0");
            instruction("fmv.w.x ft1, t1");
            instruction(FLOAT_OP.get(op) + " ft2, ft0, ft1");
            instruction("fmv.x.w t2, ft2");
            store("t2", q.result());
        }
        else if (op.startsWith("iff"))
        {
            floatJump(op.substring(3), q);
        }
        else if (op.startsWith("if"))
        {
            String skip = newSkip();

            load("t0", q.arg1());
            load("t1", q.arg2());
            instruction(SKIP_BRANCH.get(op.substring(2)) + " t0, t1, " + skip);
            instruction("j    " + q.result());
            label(skip);
        }
        else
        {
            throw new IllegalArgumentException("Cuarteta sin traduccion a RISC-V: " + op);
        }
    }

    /** flt.s / fle.s / feq.s leave 1 or 0 in t2; a > b is b < a. */
    private void floatJump(String relation, Quadruple q)
    {
        String skip = newSkip();

        load("t0", q.arg1());
        load("t1", q.arg2());
        instruction("fmv.w.x ft0, t0");
        instruction("fmv.w.x ft1, t1");
        instruction(switch (relation)
        {
            case "<"  -> "flt.s t2, ft0, ft1";
            case "<=" -> "fle.s t2, ft0, ft1";
            case ">"  -> "flt.s t2, ft1, ft0";
            case ">=" -> "fle.s t2, ft1, ft0";
            default   -> "feq.s t2, ft0, ft1";
        });
        instruction(("!=".equals(relation) ? "bnez" : "beqz") + " t2, " + skip);
        instruction("j    " + q.result());
        label(skip);
    }

    private void unary(Quadruple q, String... instructions)
    {
        load("t0", q.arg1());
        for (String instruction : instructions)
        {
            instruction(instruction);
        }
        store("t2", q.result());
    }

    /** a7 picks the call: 1 print an int, 10 exit, 11 print a character. */
    private void systemCall(String argument, int number)
    {
        if (!argument.isEmpty())
        {
            load("a0", argument);
        }
        instruction("li   a7, " + number);
        instruction("ecall");
    }

    private void epilogue()
    {
        instruction("lw   ra, -4(fp)");
        instruction("lw   t0, -8(fp)");
        instruction("mv   sp, fp");
        instruction("mv   fp, t0");
        instruction("ret");
    }

    /** param k of a call goes into a(k-1): the params of a call always come together, right before it. */
    private void argument(String value)
    {
        if (argumentCount == ARGUMENTS)
        {
            throw tooManyArguments();
        }
        load("a" + argumentCount++, value);
    }

    // ponytail: a0-a7 only; a ninth argument would need the stack, and no program passes more than 7 plus this.
    private static UnsupportedOperationException tooManyArguments()
    {
        return new UnsupportedOperationException("Una llamada lleva a lo sumo " + ARGUMENTS
                + " argumentos (a0-a7), contando el objeto de un metodo.");
    }

    /* =================================================================
     * Operands
     * ================================================================= */

    /** An operand into a register: a literal by li, a global through its label, the rest from its slot. */
    private void load(String register, String operand)
    {
        if (INTEGER.matcher(operand).matches())
        {
            instruction("li   " + register + ", " + operand);
        }
        else if (DECIMAL.matcher(operand).matches())
        {
            instruction("li   " + register + ", " + Float.floatToIntBits(Float.parseFloat(operand)) + "   # " + operand);
        }
        else if (operand.startsWith("'"))
        {
            instruction("li   " + register + ", " + decode(operand).codePointAt(0));
        }
        else if (operand.startsWith("\""))
        {
            instruction("li   " + register + ", " + intern(decode(operand)));
        }
        else if (globals.containsKey(operand))
        {
            instruction("la   t6, " + globals.get(operand));
            instruction("lw   " + register + ", 0(t6)");
        }
        else
        {
            instruction("lw   " + register + ", " + slot(operand));
        }
    }

    private void store(String register, String variable)
    {
        if (globals.containsKey(variable))
        {
            instruction("la   t6, " + globals.get(variable));
            instruction("sw   " + register + ", 0(t6)");
        }
        else
        {
            instruction("sw   " + register + ", " + slot(variable));
        }
    }

    /** off(fp), or 0(t6) with t6 = fp + off when the offset does not fit the 12 bits of lw and sw. */
    private String slot(String variable)
    {
        int offset = offsets.get(variable);

        if (offset >= -2048)
        {
            return offset + "(fp)";
        }
        instruction("li   t6, " + offset);
        instruction("add  t6, t6, fp");
        return "0(t6)";
    }

    /** A cell index into its address: s11 + 4 * index. */
    private void cell(String register)
    {
        instruction("slli " + register + ", " + register + ", 2");
        instruction("add  " + register + ", " + register + ", s11");
    }

    private boolean isLocal(String operand)
    {
        return !operand.isEmpty() && !HEAP.equals(operand) && !globals.containsKey(operand)
                && !INTEGER.matcher(operand).matches() && !DECIMAL.matcher(operand).matches()
                && !operand.startsWith("'") && !operand.startsWith("\"");
    }

    /** The fields of a quadruple that hold values: what it reads and what it writes. */
    private static List<String> values(Quadruple q)
    {
        return switch (q.op())
        {
            case "label", "goto", "func", "end_func", "global", "halt" -> List.of();
            case "call" -> List.of(q.result());
            default     -> q.op().startsWith("if") ? List.of(q.arg1(), q.arg2())
                                                   : List.of(q.arg1(), q.arg2(), q.result());
        };
    }

    /* =================================================================
     * Strings
     * ================================================================= */

    /** The first cell of the literal in the pool: equal texts share one copy, "" is the null cell. */
    private int intern(String text)
    {
        if (text.isEmpty())
        {
            return 0;
        }
        return pool.computeIfAbsent(text, key ->
        {
            int address = poolEnd;

            poolEnd += (int) key.codePoints().count() + 1;   // + the 0 that ends it
            return address;
        });
    }

    /** The text of a textum or littera literal: without its quotes, escapes resolved. */
    private static String decode(String quoted)
    {
        String        body = quoted.substring(1, quoted.length() - 1);
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < body.length(); i++)
        {
            char character = body.charAt(i);

            if (character == '\\' && i + 1 < body.length())
            {
                character = switch (body.charAt(++i))
                {
                    case 'n'             -> '\n';
                    case 't'             -> '\t';
                    case 'r'             -> '\r';
                    case '"', '\'', '\\' -> body.charAt(i);
                    default              -> body.charAt(--i);   // "C:\Users" keeps its backslash
                };
            }
            text.append(character);
        }
        return text.toString();
    }

    /* =================================================================
     * Sections
     * ================================================================= */
    private String data()
    {
        StringBuilder data = new StringBuilder(".data\n");

        for (String label : globals.values())
        {
            data.append(label).append(":\n    .word 0\n");
        }
        if (!pool.isEmpty())
        {
            data.append("_pool:\n");
        }
        for (String literal : pool.keySet())
        {
            String cells = literal.codePoints().mapToObj(String::valueOf).collect(Collectors.joining(", "));

            data.append("    .word ").append(cells).append(", 0\n");
        }
        if (!routines.isEmpty())
        {
            data.append("_buffer:\n    .space ").append(BUFFER_BYTES).append('\n');
        }
        return data.toString();
    }

    /** Takes the heap, copies the strings to heap[1..k] (heap[0] stays 0, null), sets H, runs main and exits. */
    private String startup()
    {
        int           cells   = poolEnd - 1;
        StringBuilder startup = new StringBuilder("\n.text\n.globl main\nmain:\n");

        startup.append("    li   a0, ").append(HEAP_BYTES).append("\n")
               .append("    li   a7, 9\n")
               .append("    ecall\n")
               .append("    mv   s11, a0\n");
        if (cells > 0)
        {
            startup.append("    la   t0, _pool\n")
                   .append("    addi t1, s11, 4\n")
                   .append("    li   t2, ").append(cells).append("\n")
                   .append("_pool_copy:\n")
                   .append("    beqz t2, _pool_done\n")
                   .append("    lw   t3, 0(t0)\n")
                   .append("    sw   t3, 0(t1)\n")
                   .append("    addi t0, t0, 4\n")
                   .append("    addi t1, t1, 4\n")
                   .append("    addi t2, t2, -1\n")
                   .append("    j    _pool_copy\n")
                   .append("_pool_done:\n");
        }
        startup.append("    li   t0, ").append(poolEnd).append("\n")
               .append("    la   t6, _H\n")
               .append("    sw   t0, 0(t6)\n")
               .append("    jal  ra, ").append(functions.get("main")).append("\n")
               .append("    li   a7, 10\n")
               .append("    ecall\n");
        return startup.toString();
    }

    /* =================================================================
     * Support
     * ================================================================= */
    /** One instruction, its operands in one column: "lw   t0, -12(fp)", "blt  t0, t1, _s1". */
    private void instruction(String instruction)
    {
        String[] parts = instruction.strip().split("\\s+", 2);

        text.append("    ").append(parts.length == 1 ? parts[0] : String.format("%-4s %s", parts[0], parts[1]))
            .append('\n');
    }

    private void label(String name)
    {
        text.append(name).append(":\n");
    }

    /** The quadruple as three address code, above the instructions it became. */
    private void comment(Quadruple quadruple)
    {
        text.append("    # ").append(ThreeAddressPrinter.line(quadruple).strip()).append('\n');
    }

    private String newSkip()
    {
        return "_s" + ++skipCount;
    }

    /** Only what an assembler takes in a label: a function or a global named Año gets A_o. */
    private static String sanitize(String name)
    {
        return name.replaceAll("[^A-Za-z0-9_]", "_");
    }

    private static String unique(String base, Collection<String> taken)
    {
        String name = base;

        for (int n = 2; taken.contains(name); n++)
        {
            name = base + "_" + n;
        }
        return name;
    }
}
