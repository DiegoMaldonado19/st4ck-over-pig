package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.List;

/**
 * leer() -- a value read from the console.
 *
 * Its type is the one its destination expects (entero n = leer() reads an
 * integer), so the semantic analyzer sets it; alone it reads a line of text.
 */
public class ReadExpression extends Expression
{
    public ReadExpression(int line, int column)
    {
        super(line, column);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitReadExpression(this);
    }

    @Override
    public String getLabel()
    {
        return "leer()";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
