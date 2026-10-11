package com.dmaldonado.st4ck.over.pig.model.ast.statement;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.ArrayList;
import java.util.List;

/** per (esto i : numerus 0; i &lt; 10; i++) { } */
public class ForStatement extends AstNode
{
    private final AstNode    initialization;
    private final Expression condition;
    private final AstNode    update;
    private final Block      body;

    public ForStatement(AstNode initialization, Expression condition, AstNode update,
                        Block body, int line, int column)
    {
        super(line, column);
        this.initialization = initialization;
        this.condition      = condition;
        this.update         = update;
        this.body           = body;
    }

    public AstNode getInitialization()
    {
        return initialization;
    }

    public Expression getCondition()
    {
        return condition;
    }

    public AstNode getUpdate()
    {
        return update;
    }

    public Block getBody()
    {
        return body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitForStatement(this);
    }

    @Override
    public String getLabel()
    {
        return "PER";
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>();
        if (initialization != null)
        {
            children.add(initialization);
        }
        if (condition != null)
        {
            children.add(condition);
        }
        if (update != null)
        {
            children.add(update);
        }
        if (body != null)
        {
            children.add(body);
        }
        return children;
    }
}
