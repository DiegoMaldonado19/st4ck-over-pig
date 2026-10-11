package com.dmaldonado.st4ck.over.pig.model.codegen;

import java.util.List;
import java.util.Set;

/**
 * The quadruples as the three address code written by hand: one line each,
 * labels and function headers at the margin, everything else indented.
 * The text and the quadruple table are the same list read two ways.
 */
public final class ThreeAddressPrinter
{
    private static final String      INDENT = "    ";
    private static final Set<String> BINARY = Set.of("+", "-", "*", "/", "%", "+f", "-f", "*f", "/f");

    private ThreeAddressPrinter()
    {
    }

    public static String print(List<Quadruple> code)
    {
        StringBuilder text = new StringBuilder();

        for (Quadruple quadruple : code)
        {
            // A blank line before each function, so each one reads as its own block.
            if ("func".equals(quadruple.op()) && !text.isEmpty())
            {
                text.append('\n');
            }
            text.append(line(quadruple)).append('\n');
        }
        return text.toString();
    }

    private static String line(Quadruple q)
    {
        String op = q.op();

        return switch (op)
        {
            case "label"    -> q.result() + ":";
            case "global"   -> "global " + q.result();
            case "func"     -> "func " + q.result() + "(" + q.arg1().replace(",", ", ") + "):";
            case "end_func" -> "end_func " + q.result();
            case "goto"     -> INDENT + "goto " + q.result();
            case "="        -> INDENT + q.result() + " = " + q.arg1();
            case "=[]"      -> INDENT + q.result() + " = heap[" + q.arg2() + "]";
            case "[]="      -> INDENT + "heap[" + q.arg1() + "] = " + q.arg2();
            case "call"     -> INDENT + (q.result().isEmpty() ? "" : q.result() + " = ")
                               + "call " + q.arg1() + ", " + q.arg2();
            default         -> INDENT + other(q);
        };
    }

    /** if a < b goto L, x = a + b, x = uminus a, print_int a, x = read_int, return, halt... */
    private static String other(Quadruple q)
    {
        String op = q.op();

        if (op.startsWith("iff"))
        {
            return "if " + q.arg1() + " " + op.substring(3) + "f " + q.arg2() + " goto " + q.result();
        }
        if (op.startsWith("if"))
        {
            return "if " + q.arg1() + " " + op.substring(2) + " " + q.arg2() + " goto " + q.result();
        }
        if (BINARY.contains(op))
        {
            return q.result() + " = " + q.arg1() + " " + op + " " + q.arg2();
        }

        String instruction = (op + " " + q.arg1()).strip();

        return q.result().isEmpty() ? instruction : q.result() + " = " + instruction;
    }
}
