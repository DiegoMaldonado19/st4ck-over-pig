package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import com.dmaldonado.st4ck.over.pig.model.symbols.Symbol;
import java.util.List;

/** Use of a variable by name. */
public class IdentifierExpression extends Expression
{
    private final String name;

    /**
     * The symbol the name resolved to, in the scope where it was used. Set by
     * the semantic analyzer so the generator never has to redo the scoping.
     */
    private Symbol symbol;

    public IdentifierExpression(String name, int line, int column)
    {
        super(line, column);
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public Symbol getSymbol()
    {
        return symbol;
    }

    public void setSymbol(Symbol symbol)
    {
        this.symbol = symbol;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitIdentifierExpression(this);
    }

    @Override
    public String getLabel()
    {
        return name;
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
