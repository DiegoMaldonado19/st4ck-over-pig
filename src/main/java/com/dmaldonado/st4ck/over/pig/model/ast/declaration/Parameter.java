package com.dmaldonado.st4ck.over.pig.model.ast.declaration;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.List;

/**
 * esto fuerza : numerus, inside a function signature.
 *
 * rank counts the dimensions of an array parameter, 1 for the Y? form
 * "[] entero miArray" and 2 for a Zetariano "int[][] m": the function receives
 * the address of the caller's array, never a copy. structMarker is the Y? form
 * "{} Persona p", which only a structure may carry.
 */
public class Parameter extends AstNode
{
    private final String   name;
    private final String   typeText;
    private final DataType type;
    private final int      rank;
    private final boolean  structMarker;

    public Parameter(String name, String typeText, int rank, boolean structMarker, int line, int column)
    {
        super(line, column);
        this.name         = name;
        this.typeText     = typeText;
        this.type         = DataType.fromText(typeText);
        this.rank         = rank;
        this.structMarker = structMarker;
    }

    public boolean isArray()
    {
        return rank > 0;
    }

    public int getRank()
    {
        return rank;
    }

    public boolean hasStructMarker()
    {
        return structMarker;
    }

    public String getName()
    {
        return name;
    }

    public String getTypeText()
    {
        return typeText;
    }

    public DataType getType()
    {
        return type;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitParameter(this);
    }

    @Override
    public String getLabel()
    {
        return "param " + name + " : " + (isArray() ? "[]".repeat(rank) + " " : "") + typeText;
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
