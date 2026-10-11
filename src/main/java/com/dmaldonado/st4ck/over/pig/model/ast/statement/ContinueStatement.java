package com.dmaldonado.st4ck.over.pig.model.ast.statement;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import java.util.List;

/** perge; */
public class ContinueStatement extends AstNode
{
    public ContinueStatement(int line, int column)
    {
        super(line, column);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitContinueStatement(this);
    }

    @Override
    public String getLabel()
    {
        return "PERGE";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
