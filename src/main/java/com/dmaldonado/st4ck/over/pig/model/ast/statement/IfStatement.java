package com.dmaldonado.st4ck.over.pig.model.ast.statement;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.ArrayList;
import java.util.List;

/**
 * si (c) { } aliter (c2) { } aliter { } finis;
 *
 * A chain of aliter with condition is desugared into nested IfStatement, so
 * elseBranch is either a Block (plain aliter) or another IfStatement (an
 * aliter that carried its own condition).
 */
public class IfStatement extends AstNode
{
    private final Expression condition;
    private final Block      thenBranch;
    private final AstNode    elseBranch;

    public IfStatement(Expression condition, Block thenBranch, AstNode elseBranch,
                       int line, int column)
    {
        super(line, column);
        this.condition  = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public Expression getCondition()
    {
        return condition;
    }

    public Block getThenBranch()
    {
        return thenBranch;
    }

    /** Block, nested IfStatement, or null when there is no aliter. */
    public AstNode getElseBranch()
    {
        return elseBranch;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitIfStatement(this);
    }

    @Override
    public String getLabel()
    {
        return "SI";
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>();
        if (condition != null)
        {
            children.add(condition);
        }
        if (thenBranch != null)
        {
            children.add(thenBranch);
        }
        if (elseBranch != null)
        {
            children.add(elseBranch);
        }
        return children;
    }
}
