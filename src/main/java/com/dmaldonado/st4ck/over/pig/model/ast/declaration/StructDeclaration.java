package com.dmaldonado.st4ck.over.pig.model.ast.declaration;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import java.util.ArrayList;
import java.util.List;

/** structura Persona { esto nombre : textum; esto edad : numerus; } finis; */
public class StructDeclaration extends AstNode
{
    private final String            name;
    private final List<StructField> fields;

    public StructDeclaration(String name, List<StructField> fields, int line, int column)
    {
        super(line, column);
        this.name   = name;
        this.fields = fields;
    }

    public String getName()
    {
        return name;
    }

    public List<StructField> getFields()
    {
        return fields;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitStructDeclaration(this);
    }

    @Override
    public String getLabel()
    {
        return "structura " + name;
    }

    @Override
    public List<AstNode> getChildren()
    {
        return new ArrayList<>(fields);
    }
}
