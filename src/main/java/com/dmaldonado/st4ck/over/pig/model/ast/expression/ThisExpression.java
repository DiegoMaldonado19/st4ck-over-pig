package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.List;

/**
 * The object a method runs on.
 *
 * Written by the user, 'this' belongs to Proyecto 2 and is reported. The
 * builder also creates it (implicit = true) to run the attribute initializers
 * inside a constructor, where a parameter could shadow the attribute.
 */
public class ThisExpression extends Expression
{
    private final boolean implicit;

    public ThisExpression(boolean implicit, int line, int column)
    {
        super(line, column);
        this.implicit = implicit;
    }

    public boolean isImplicit()
    {
        return implicit;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitThisExpression(this);
    }

    @Override
    public String getLabel()
    {
        return "this";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
