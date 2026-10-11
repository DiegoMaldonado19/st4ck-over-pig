package com.dmaldonado.st4ck.over.pig.model.ast;

import com.dmaldonado.st4ck.over.pig.model.types.DataType;

/**
 * Nodo que produce un valor: separa lo que puede ir a la derecha de una
 * asignacion de lo que no.
 *
 * Los dos campos mutables los llena el analizador semantico, nunca el
 * constructor del AST: computedType es el tipo inferido, y structName dice
 * CUAL structura, que DataType.ESTRUCTURA por si solo no distingue.
 */
public abstract class Expression extends AstNode
{
    private DataType computedType = DataType.ERROR;
    private String   structName;

    protected Expression(int line, int column)
    {
        super(line, column);
    }

    public DataType getComputedType()
    {
        return computedType;
    }

    public void setComputedType(DataType computedType)
    {
        this.computedType = computedType;
    }

    public String getStructName()
    {
        return structName;
    }

    public void setStructName(String structName)
    {
        this.structName = structName;
    }
}
