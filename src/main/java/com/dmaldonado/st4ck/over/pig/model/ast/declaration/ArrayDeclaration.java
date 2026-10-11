package com.dmaldonado.st4ck.over.pig.model.ast.declaration;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.CompositeLiteralExpression;
import com.dmaldonado.st4ck.over.pig.model.symbols.Symbol;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.ArrayList;
import java.util.List;

/**
 * series mis_enteros[2] : numerus {1, 1};   entero m[3][2] = {{1, 2}, ...}   int[] a = new int[5];
 *
 * An array of any rank is flattened row major into one block of the heap:
 * m[i][j] lives at m + i * columns + j. With more than one dimension the
 * initial values arrive as nested literals, one per row, and getFlatValues()
 * lines them up in that same order.
 *
 * dimensions is empty only for a Zetariano "int[] a;", which starts as null.
 */
public class ArrayDeclaration extends AstNode
{
    private final String           name;
    private final List<Expression> dimensions;
    private final int              rank;
    private final String           typeText;
    private final DataType         elementType;
    private final List<Expression> initialValues;

    /** Zetariano "new double[2]": the element type it is created with, which has to be the declared one. */
    private final String           createdTypeText;

    /** The declared symbol, which knows its stack slot. Set by the semantic analyzer. */
    private Symbol symbol;

    public ArrayDeclaration(String name, List<Expression> dimensions, int rank, String typeText,
                            List<Expression> initialValues, int line, int column)
    {
        this(name, dimensions, rank, typeText, initialValues, typeText, line, column);
    }

    public ArrayDeclaration(String name, List<Expression> dimensions, int rank, String typeText,
                            List<Expression> initialValues, String createdTypeText, int line, int column)
    {
        super(line, column);
        this.createdTypeText = createdTypeText;
        this.name          = name;
        this.dimensions    = dimensions;
        this.rank          = rank;
        this.typeText      = typeText;
        this.elementType   = DataType.fromText(typeText);
        this.initialValues = initialValues;
    }

    public String getName()
    {
        return name;
    }

    /** One size per dimension, outermost first. */
    public List<Expression> getDimensions()
    {
        return dimensions;
    }

    public int getRank()
    {
        return rank;
    }

    public String getTypeText()
    {
        return typeText;
    }

    public String getCreatedTypeText()
    {
        return createdTypeText;
    }

    public DataType getElementType()
    {
        return elementType;
    }

    /** As written: nested literals, one per row, when the rank is above 1. Empty without values. */
    public List<Expression> getInitialValues()
    {
        return initialValues;
    }

    /** The values in the order they are stored: row after row. */
    public List<Expression> getFlatValues()
    {
        List<Expression> flat = new ArrayList<>();

        flatten(initialValues, rank - 1, flat);
        return flat;
    }

    private static void flatten(List<Expression> values, int depth, List<Expression> flat)
    {
        for (Expression value : values)
        {
            if (depth > 0 && value instanceof CompositeLiteralExpression row)
            {
                flatten(row.getValues(), depth - 1, flat);
            }
            else
            {
                flat.add(value);
            }
        }
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
        return visitor.visitArrayDeclaration(this);
    }

    @Override
    public String getLabel()
    {
        return "series " + name + "[]".repeat(rank) + " : " + (typeText == null ? "?" : typeText);
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>(dimensions);
        children.addAll(initialValues);
        return children;
    }
}
