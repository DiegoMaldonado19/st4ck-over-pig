package com.dmaldonado.st4ck.over.pig.model.ast;

import java.util.List;

/**
 * Raiz del AST (patron Composite). Cada nodo conoce su posicion para reportar
 * linea:columna y acepta un visitante. getLabel() y getChildren() permiten
 * graficarlo sin conocer un solo tipo concreto.
 */
public abstract class AstNode
{
    private final int line;
    private final int column;

    protected AstNode(int line, int column)
    {
        this.line   = line;
        this.column = column;
    }

    public int getLine()
    {
        return line;
    }

    public int getColumn()
    {
        return column;
    }

    /** Entry point of the Visitor pattern. */
    public abstract <T> T accept(AstVisitor<T> visitor);

    /** Text drawn inside the node when the AST is graphed. */
    public abstract String getLabel();

    /** Direct children, in source order, for generic traversals. */
    public abstract List<AstNode> getChildren();

    @Override
    public String toString()
    {
        return getLabel();
    }
}
