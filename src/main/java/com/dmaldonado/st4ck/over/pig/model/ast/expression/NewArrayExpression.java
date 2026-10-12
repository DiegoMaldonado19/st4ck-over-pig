package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.ArrayList;
import java.util.List;

/**
 * new int[f][c] as a value: "celdas = new int[f][c];". A declaration that
 * creates its array keeps its own form, ArrayDeclaration.
 */
public class NewArrayExpression extends Expression
{
    private final String           typeText;
    private final List<Expression> dimensions;

    public NewArrayExpression(String typeText, List<Expression> dimensions, int line, int column)
    {
        super(line, column);
        this.typeText   = typeText;
        this.dimensions = dimensions;
    }

    public String getTypeText()
    {
        return typeText;
    }

    public DataType getElementType()
    {
        return DataType.fromText(typeText);
    }

    /** One size per dimension, outermost first. */
    public List<Expression> getDimensions()
    {
        return dimensions;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitNewArrayExpression(this);
    }

    @Override
    public String getLabel()
    {
        return "new " + typeText + "[]".repeat(dimensions.size());
    }

    @Override
    public List<AstNode> getChildren()
    {
        return new ArrayList<>(dimensions);
    }
}
