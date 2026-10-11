package com.dmaldonado.st4ck.over.pig.model.symbols;

import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A function declared with {@code actio} (no return) or {@code ratio}, a Y?
 * {@code definir}, or a Zetariano constructor or method. A method is named
 * after its class, Pila.apilar, which keeps it apart from a free function
 * and from the methods of other classes with the same name.
 */
public class FunctionSymbol extends Symbol
{
    private final List<VariableSymbol> parameters;
    private final boolean              returnsValue;
    /** Name of the structura when the return type is ESTRUCTURA. */
    private final String               returnStructName;
    /** Class of a constructor or method; null for a free function. */
    private final String               ownerClass;
    private final boolean              constructor;

    /** Stack slots of its frame: return value, parameters and locals. Known once its body is analyzed. */
    private int                        frameSize;

    public FunctionSymbol(String name, DataType returnType, String typeText, String scopeName,
                          List<VariableSymbol> parameters, boolean returnsValue,
                          String returnStructName, String ownerClass, boolean constructor,
                          int line, int column)
    {
        super(name, returnType, typeText, ownerClass == null ? SymbolCategory.FUNCTION
                : constructor ? SymbolCategory.CONSTRUCTOR : SymbolCategory.METHOD, scopeName, line, column);
        this.parameters       = parameters == null ? new ArrayList<>() : parameters;
        this.returnsValue     = returnsValue;
        this.returnStructName = returnStructName;
        this.ownerClass       = ownerClass;
        this.constructor      = constructor;
    }

    public String getOwnerClass()
    {
        return ownerClass;
    }

    /** A constructor or method: the object travels as a hidden first argument. */
    public boolean isMethod()
    {
        return ownerClass != null;
    }

    public boolean isConstructor()
    {
        return constructor;
    }

    public List<VariableSymbol> getParameters()
    {
        return parameters;
    }

    public boolean returnsValue()
    {
        return returnsValue;
    }

    public String getReturnStructName()
    {
        return returnStructName;
    }

    public int getParameterCount()
    {
        return parameters.size();
    }

    public int getFrameSize()
    {
        return frameSize;
    }

    public void setFrameSize(int frameSize)
    {
        this.frameSize = frameSize;
    }

    /**
     * Two overloads clash when their parameters have the same types, compared
     * by DataType and not by text: f(entero) in Y? and f(numerus) in PigLatin
     * are the same function.
     */
    public boolean hasSameParameters(FunctionSymbol other)
    {
        if (parameters.size() != other.parameters.size())
        {
            return false;
        }
        for (int i = 0; i < parameters.size(); i++)
        {
            VariableSymbol mine   = parameters.get(i);
            VariableSymbol theirs = other.parameters.get(i);

            if (mine.getType() != theirs.getType() || mine.isArray() != theirs.isArray()
                    || !Objects.equals(mine.getStructName(), theirs.getStructName()))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Signature used in error messages, calcularPoder(numerus, [] entero), and
     * by the generator to give every overload its own C name.
     */
    public String getSignature()
    {
        return getName() + "(" + parameters.stream()
                .map(parameter -> (parameter.isArray() ? "[] " : "") + parameter.getTypeText())
                .collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public String getDetail()
    {
        return getSignature() + (frameSize > 0 ? ", marco = " + frameSize : "");
    }
}
