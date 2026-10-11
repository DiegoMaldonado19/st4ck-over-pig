package com.dmaldonado.st4ck.over.pig.model.codegen;

/**
 * One three address instruction: result = arg1 op arg2.
 *
 * Unused slots hold an empty string, never null, so the table and the C
 * emitter print them without a check.
 */
public record Quadruple(String op, String arg1, String arg2, String result)
{
}
