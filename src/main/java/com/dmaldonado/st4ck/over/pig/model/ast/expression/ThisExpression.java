package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.List;

/**
 * The object a method runs on: this.x, this.m() or this alone.
 *
 * super is the same object seen as its parent class: super.m() runs the
 * parent's version of m, without dispatch.
 *
 * The builder also creates one to run the attribute initializers inside a
 * constructor, where a parameter could shadow the attribute.
 */
public class ThisExpression extends Expression
{
    private final boolean asParent;

    public ThisExpression(int line, int column)
    {
        this(false, line, column);
    }

    public ThisExpression(boolean asParent, int line, int column)
    {
        super(line, column);
        this.asParent = asParent;
    }

    /** Written super: the object as an instance of the class it extends. */
    public boolean isSuper()
    {
        return asParent;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitThisExpression(this);
    }

    @Override
    public String getLabel()
    {
        return asParent ? "super" : "this";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
