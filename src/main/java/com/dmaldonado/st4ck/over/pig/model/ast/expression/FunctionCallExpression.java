package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import com.dmaldonado.st4ck.over.pig.model.symbols.FunctionSymbol;
import java.util.ArrayList;
import java.util.List;

/** calcularPoder(10, 0.5) */
public class FunctionCallExpression extends Expression
{
    private final String           name;
    private final List<Expression> arguments;

    /** The overload the call resolved to. Set by the semantic analyzer. */
    private FunctionSymbol function;

    public FunctionCallExpression(String name, List<Expression> arguments, int line, int column)
    {
        super(line, column);
        this.name      = name;
        this.arguments = arguments;
    }

    public String getName()
    {
        return name;
    }

    public List<Expression> getArguments()
    {
        return arguments;
    }

    public FunctionSymbol getFunction()
    {
        return function;
    }

    public void setFunction(FunctionSymbol function)
    {
        this.function = function;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitFunctionCallExpression(this);
    }

    @Override
    public String getLabel()
    {
        return name + "()";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return new ArrayList<>(arguments);
    }
}
