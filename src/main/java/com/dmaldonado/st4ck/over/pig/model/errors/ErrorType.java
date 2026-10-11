package com.dmaldonado.st4ck.over.pig.model.errors;

/** The three kinds of error the statement requires the compiler to report. */
public enum ErrorType
{
    LEXICAL("Lexico"),
    SYNTACTIC("Sintactico"),
    SEMANTIC("Semantico");

    private final String label;

    ErrorType(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
