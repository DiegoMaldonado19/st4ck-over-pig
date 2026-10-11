package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import com.dmaldonado.st4ck.over.pig.model.symbols.FunctionSymbol;
import java.util.ArrayList;
import java.util.List;

/** novus Persona(12, "Profesor") -- an instance of a class defined in a .z file. */
public class NewExpression extends Expression
{
    private final String           className;
    private final List<Expression> arguments;

    /** The constructor overload the call resolved to. Set by the semantic analyzer. */
    private FunctionSymbol constructor;

    public NewExpression(String className, List<Expression> arguments, int line, int column)
    {
        super(line, column);
        this.className = className;
        this.arguments = arguments;
    }

    public String getClassName()
    {
        return className;
    }

    public List<Expression> getArguments()
    {
        return arguments;
    }

    public FunctionSymbol getConstructor()
    {
        return constructor;
    }

    public void setConstructor(FunctionSymbol constructor)
    {
        this.constructor = constructor;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitNewExpression(this);
    }

    @Override
    public String getLabel()
    {
        return "novus " + className + "()";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return new ArrayList<>(arguments);
    }
}
