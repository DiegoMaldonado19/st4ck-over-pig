package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.ArrayList;
import java.util.List;

/**
 * A { ... } literal, in its two flavours:
 *
 *   named       { nombre: "Yennifer", edad: 999 }   to build a structura
 *   positional  { 1, 1 }                            to fill an array
 *
 * fieldNames is null for the positional flavour; otherwise fieldNames.get(i)
 * names values.get(i). Structures need the names because the enunciado says
 * the order of the attributes does not matter.
 */
public class CompositeLiteralExpression extends Expression
{
    private final List<String>     fieldNames;
    private final List<Expression> values;

    public CompositeLiteralExpression(List<String> fieldNames, List<Expression> values,
                                      int line, int column)
    {
        super(line, column);
        this.fieldNames = fieldNames;
        this.values     = values;
    }

    public boolean isNamed()
    {
        return fieldNames != null;
    }

    /** Null when the literal is positional. */
    public List<String> getFieldNames()
    {
        return fieldNames;
    }

    public List<Expression> getValues()
    {
        return values;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitCompositeLiteralExpression(this);
    }

    /**
     * The field names are listed here, in the same order as the children, so
     * the AST graph does not lose them: the values are separate child nodes.
     */
    @Override
    public String getLabel()
    {
        return isNamed() ? "LITERAL { " + String.join(", ", fieldNames) + " }" : "LITERAL { }";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return new ArrayList<>(values);
    }
}
