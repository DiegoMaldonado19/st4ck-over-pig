package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.ArrayList;
import java.util.List;

/** a + b, a &amp;&amp; b, a == b ... every level of the precedence chain folds here. */
public class BinaryExpression extends Expression
{
    private final Expression left;
    private final String     operator;
    private final Expression right;

    public BinaryExpression(Expression left, String operator, Expression right,
                            int line, int column)
    {
        super(line, column);
        this.left     = left;
        this.operator = operator;
        this.right    = right;
    }

    public Expression getLeft()
    {
        return left;
    }

    public String getOperator()
    {
        return operator;
    }

    public Expression getRight()
    {
        return right;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitBinaryExpression(this);
    }

    @Override
    public String getLabel()
    {
        return operator;
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>();
        if (left != null)
        {
            children.add(left);
        }
        if (right != null)
        {
            children.add(right);
        }
        return children;
    }
}
