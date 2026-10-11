package com.dmaldonado.st4ck.over.pig.model.ast.statement;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import java.util.List;

/** interrumpe; */
public class BreakStatement extends AstNode
{
    public BreakStatement(int line, int column)
    {
        super(line, column);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitBreakStatement(this);
    }

    @Override
    public String getLabel()
    {
        return "INTERRUMPE";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
