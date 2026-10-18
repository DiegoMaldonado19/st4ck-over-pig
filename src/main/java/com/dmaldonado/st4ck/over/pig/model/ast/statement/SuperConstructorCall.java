package com.dmaldonado.st4ck.over.pig.model.ast.statement;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import com.dmaldonado.st4ck.over.pig.model.symbols.FunctionSymbol;
import java.util.ArrayList;
import java.util.List;

/**
 * super(nombre); -- the call to the parent's constructor, on the same object,
 * that a constructor of a subclass starts with.
 *
 * Where it writes none, Java calls the parent's constructor without
 * parameters: the builder adds that one, implicit, so every constructor of a
 * subclass starts with its call.
 */
public class SuperConstructorCall extends AstNode
{
    private final List<Expression> arguments;
    private final boolean          implicit;

    /** The parent's constructor overload the call resolved to. Set by the semantic analyzer. */
    private FunctionSymbol constructor;

    public SuperConstructorCall(List<Expression> arguments, boolean implicit, int line, int column)
    {
        super(line, column);
        this.arguments = arguments;
        this.implicit  = implicit;
    }

    public List<Expression> getArguments()
    {
        return arguments;
    }

    /** Added by the builder, not written. */
    public boolean isImplicit()
    {
        return implicit;
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
        return visitor.visitSuperConstructorCall(this);
    }

    @Override
    public String getLabel()
    {
        return "super()";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return new ArrayList<>(arguments);
    }
}
