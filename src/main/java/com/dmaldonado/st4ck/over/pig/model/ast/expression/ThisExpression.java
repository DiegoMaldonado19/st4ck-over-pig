package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.List;

/**
 * The object a method runs on: this.x, this.m() or this alone.
 *
 * The builder also creates one to run the attribute initializers inside a
 * constructor, where a parameter could shadow the attribute.
 */
public class ThisExpression extends Expression
{
    public ThisExpression(int line, int column)
    {
        super(line, column);
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
