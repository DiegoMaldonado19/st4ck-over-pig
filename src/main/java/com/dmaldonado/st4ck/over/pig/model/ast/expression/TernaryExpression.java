package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.List;

/** (edad >= 18) ? "Es mayor de edad" : "Es menor de edad" */
public class TernaryExpression extends Expression
{
    private final Expression condition;
    private final Expression whenTrue;
    private final Expression whenFalse;

    public TernaryExpression(Expression condition, Expression whenTrue, Expression whenFalse,
                             int line, int column)
    {
        super(line, column);
        this.condition = condition;
        this.whenTrue  = whenTrue;
        this.whenFalse = whenFalse;
    }

    public Expression getCondition()
    {
        return condition;
    }

    public Expression getWhenTrue()
    {
        return whenTrue;
    }

    public Expression getWhenFalse()
    {
        return whenFalse;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitTernaryExpression(this);
    }

    @Override
    public String getLabel()
    {
        return "? :";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of(condition, whenTrue, whenFalse);
    }
}
