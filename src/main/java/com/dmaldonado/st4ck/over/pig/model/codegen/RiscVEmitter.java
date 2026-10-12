package com.dmaldonado.st4ck.over.pig.model.codegen;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 */
public class RiscVEmitter
{
    /** RARS gives sbrk about 3.75 MiB: 3 MiB are 786 432 cells. */
    private static final int                 HEAP_BYTES  = 3 * 1024 * 1024;
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

    private final StringBuilder        text      = new StringBuilder();
    /** Every distinct string literal and its first cell: cell 0 is null, which reads as "". */
    private final Map<String, Integer> pool      = new LinkedHashMap<>();
    /** The label of each global in .data, H among them. */
    private final Map<String, String>  globals   = new LinkedHashMap<>();
    private final Map<String, String>  functions = new HashMap<>();
    /** Where each value of the function being translated lives, relative to fp. */
    private final Map<String, Integer> offsets   = new HashMap<>();

    private int poolEnd = 1;
    private int skipCount;

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
        // The pool is complete only now: the data and the startup go first in the file.
        return data() + startup() + text;
    }

    /**
     * One function. The caller pushed its parameters, the first one deepest:
     * parameter i of n is at fp + 4 * (n - 1 - i). Below fp go ra, the
     * caller's fp, and one slot for each variable and temporary.
     */
    private void function(List<Quadruple> body)
    {
        Quadruple    header     = body.get(0);
        List<String> parameters = header.arg1().isEmpty() ? List.of() : List.of(header.arg1().split(","));

        offsets.clear();
        for (int i = 0; i < parameters.size(); i++)
        {
            offsets.put(parameters.get(i), 4 * (parameters.size() - 1 - i));
        }
        for (Quadruple quadruple : body)
        {
            for (String operand : values(quadruple))
            {
                if (isLocal(operand) && !offsets.containsKey(operand))
                {
                    offsets.put(operand, -12 - 4 * (offsets.size() - parameters.size()));
                }
            }
        }

        text.append('\n');
        comment(header);
        label(functions.get(header.result()));
        instruction("sw   ra, -4(sp)");
        instruction("sw   fp, -8(sp)");
        instruction("mv   fp, sp");
        instruction("li   t6, " + (8 + 4 * (offsets.size() - parameters.size())));
        instruction("sub  sp, sp, t6");

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
            case "param"      ->
            {
                load("t0", q.arg1());
                instruction("addi sp, sp, -4");
                instruction("sw   t0, 0(sp)");
            }
            case "call"       ->
            {
                int arguments = Integer.parseInt(q.arg2());

                instruction("jal  ra, " + functions.get(q.arg1()));
                if (arguments > 0)
                {
                    instruction("addi sp, sp, " + 4 * arguments);
                }
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
                epilogue();
            }
            case "end_func"   -> epilogue();
            case "print_int"  -> systemCall(q.arg1(), 1);
            case "print_char" -> systemCall(q.arg1(), 11);
            case "halt"       -> systemCall("", 10);
            default           -> operation(q);
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
