package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.List;

/** non x, -x */
public class UnaryExpression extends Expression
{
    private final String     operator;
    private final Expression operand;

    public UnaryExpression(String operator, Expression operand, int line, int column)
    {
        super(line, column);
        this.operator = operator;
        this.operand  = operand;
    }

    public String getOperator()
    {
        return operator;
    }

    public Expression getOperand()
    {
        return operand;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitUnaryExpression(this);
    }

    @Override
    public String getLabel()
    {
        return operator + " (unario)";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return operand == null ? List.of() : List.of(operand);
    }
}
