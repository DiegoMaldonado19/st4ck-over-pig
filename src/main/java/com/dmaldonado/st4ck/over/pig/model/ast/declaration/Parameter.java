package com.dmaldonado.st4ck.over.pig.model.ast.declaration;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.List;

/**
 * esto fuerza : numerus, inside a function signature.
 *
 * array marks the Y? form "[] entero miArray": the function receives the
 * address of the caller's array, never a copy. structMarker is the Y? form
 * "{} Persona p", which only a structure may carry.
 */
public class Parameter extends AstNode
{
    private final String   name;
    private final String   typeText;
    private final DataType type;
    private final boolean  array;
    private final boolean  structMarker;

    public Parameter(String name, String typeText, boolean array, boolean structMarker, int line, int column)
    {
        super(line, column);
        this.name         = name;
        this.typeText     = typeText;
        this.type         = DataType.fromText(typeText);
        this.array        = array;
        this.structMarker = structMarker;
    }

    public boolean isArray()
    {
        return array;
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
        return "param " + name + " : " + (array ? "[] " : "") + typeText;
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
