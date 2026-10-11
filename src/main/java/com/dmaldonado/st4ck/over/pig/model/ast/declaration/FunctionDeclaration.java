package com.dmaldonado.st4ck.over.pig.model.ast.declaration;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.Block;
import com.dmaldonado.st4ck.over.pig.model.symbols.FunctionSymbol;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.ArrayList;
import java.util.List;

/**
 * actio atacarCerdos(...) { ... } finis;  and
 * ratio numerus calcularPoder(...) { ... } finis;
 *
 * localVariables holds the declarations written in the VARIABILES[ ] section,
 * kept apart from the body so the semantic analyzer can tell a variable
 * declared where the language allows it from one declared mid function.
 *
 * A Zetariano constructor or method is a function too, with its ownerClass:
 * it receives the object in the first slot of its frame.
 */
public class FunctionDeclaration extends AstNode
{
    private final String          name;
    private final String          returnTypeText;
    private final DataType        returnType;
    private final List<Parameter> parameters;
    private final List<AstNode>   localVariables;
    private final Block           body;
    private final boolean         returnsValue;
    /** Class a constructor or method belongs to; null for a free function. */
    private final String          ownerClass;
    private final boolean         constructor;

    /** Set by the semantic analyzer: null when the signature was rejected as a duplicate. */
    private FunctionSymbol symbol;

    public FunctionDeclaration(String name, String returnTypeText, List<Parameter> parameters,
                               List<AstNode> localVariables, Block body, boolean returnsValue,
                               int line, int column)
    {
        this(name, returnTypeText, parameters, localVariables, body, returnsValue, null, false,
                line, column);
    }

    public FunctionDeclaration(String name, String returnTypeText, List<Parameter> parameters,
                               List<AstNode> localVariables, Block body, boolean returnsValue,
                               String ownerClass, boolean constructor, int line, int column)
    {
        super(line, column);
        this.name           = name;
        this.returnTypeText = returnTypeText;
        this.returnType     = returnsValue ? DataType.fromText(returnTypeText) : DataType.VOID;
        this.parameters     = parameters;
        this.localVariables = localVariables;
        this.body           = body;
        this.returnsValue   = returnsValue;
        this.ownerClass     = ownerClass;
        this.constructor    = constructor;
    }

    public String getName()
    {
        return name;
    }

    public String getReturnTypeText()
    {
        return returnTypeText;
    }

    public DataType getReturnType()
    {
        return returnType;
    }

    public List<Parameter> getParameters()
    {
        return parameters;
    }

    /** Declarations of the VARIABILES[ ] section; empty when there is none. */
    public List<AstNode> getLocalVariables()
    {
        return localVariables;
    }

    public Block getBody()
    {
        return body;
    }

    public boolean returnsValue()
    {
        return returnsValue;
    }

    public String getOwnerClass()
    {
        return ownerClass;
    }

    public boolean isConstructor()
    {
        return constructor;
    }

    public FunctionSymbol getSymbol()
    {
        return symbol;
    }

    public void setSymbol(FunctionSymbol symbol)
    {
        this.symbol = symbol;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitFunctionDeclaration(this);
    }

    @Override
    public String getLabel()
    {
        if (ownerClass != null)
        {
            return constructor ? "constructor " + name : "metodo " + returnTypeText + " " + name;
        }
        return (returnsValue ? "ratio " + returnTypeText + " " : "actio ") + name;
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>(parameters);
        children.addAll(localVariables);
        if (body != null)
        {
            children.add(body);
        }
        return children;
    }
}
