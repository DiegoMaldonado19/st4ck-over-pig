package com.dmaldonado.st4ck.over.pig.model.ast.statement;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import java.util.ArrayList;
import java.util.List;

/**
 * A { ... } block.
 *
 * Holds AstNode and not a narrower type because the instruccion rule of the
 * grammar also admits declarations, not only statements.
 */
public class Block extends AstNode
{
    private final List<AstNode> statements;

    public Block(List<AstNode> statements, int line, int column)
    {
        super(line, column);
        this.statements = statements;
    }

    public List<AstNode> getStatements()
    {
        return statements;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitBlock(this);
    }

    @Override
    public String getLabel()
    {
        return "BLOQUE";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return new ArrayList<>(statements);
    }
}
