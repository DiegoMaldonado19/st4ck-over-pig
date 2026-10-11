package com.dmaldonado.st4ck.over.pig.model.ast.statement;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.ArrayList;
import java.util.List;

/** x = 5;   arr[0] = 5;   persona.edad = 5; */
public class Assignment extends AstNode
{
    private final Expression target;
    private final Expression value;

    public Assignment(Expression target, Expression value, int line, int column)
    {
        super(line, column);
        this.target = target;
        this.value  = value;
    }

    public Expression getTarget()
    {
        return target;
    }

    public Expression getValue()
    {
        return value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitAssignment(this);
    }

    @Override
    public String getLabel()
    {
        return "=";
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>();
        if (target != null)
        {
            children.add(target);
        }
        if (value != null)
        {
            children.add(value);
        }
        return children;
    }
}
