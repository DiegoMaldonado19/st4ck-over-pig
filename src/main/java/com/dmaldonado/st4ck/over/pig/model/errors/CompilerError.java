package com.dmaldonado.st4ck.over.pig.model.errors;

/**
 * One reported error, with the line:column the statement requires, and the
 * file it belongs to: an import brings its own line numbers.
 * Immutable: an error is a fact about a compilation that already happened.
 */
public class CompilerError
{
    private final ErrorType type;
    private final String    description;
    private final String    lexeme;
    private final int       line;
    private final int       column;
    private final String    source;

    public CompilerError(ErrorType type, String description, String lexeme, int line, int column,
                         String source)
    {
        this.type        = type;
        this.description = description;
        this.lexeme      = lexeme == null ? "" : lexeme;
        this.line        = line;
        this.column      = column;
        this.source      = source == null ? "" : source;
    }

    public ErrorType getType()
    {
        return type;
    }

    public String getDescription()
    {
        return description;
    }

    public String getLexeme()
    {
        return lexeme;
    }

    public int getLine()
    {
        return line;
    }

    public int getColumn()
    {
        return column;
    }

    /** File name; empty for a source that was never saved. */
    public String getSource()
    {
        return source;
    }

    @Override
    public String toString()
    {
        return "[" + type + "] " + (source.isEmpty() ? "" : source + " ") + "linea " + line
                + ", columna " + column + ": " + description
                + (lexeme.isEmpty() ? "" : "  ('" + lexeme + "')");
    }
}
