package com.dmaldonado.st4ck.over.pig.model.symbols;

import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An array declared with {@code series}, or the Y? / Zetariano equivalents.
 *
 * dimensions is what makes the statement's rule possible: "Si la expresion se
 * puede evaluar al hacer la verificacion de semantica se debe lanzar un error
 * en caso de que se salga del rango declarado". A dimension is -1 when it is
 * not a constant the compiler can evaluate. They also give the strides of the
 * flattened layout: m[i][j] is m + i * dimensions[1] + j.
 *
 * elementStructName remembers that "series animales : Animal" holds Animal
 * instances, so that mi_selva.animales[1].nombre can be resolved.
 */
public class ArraySymbol extends Symbol
{
    private final List<Integer> dimensions;
    private final int           valueCount;
    private final String        elementStructName;
    /** Assigned a whole other array: its declared sizes no longer say how long it is. */
    private boolean             rebound;

    public ArraySymbol(String name, DataType elementType, String typeText, String scopeName,
                       List<Integer> dimensions, int valueCount, String elementStructName,
                       int line, int column)
    {
        super(name, elementType, typeText, SymbolCategory.ARRAY, scopeName, line, column);
        this.dimensions        = dimensions;
        this.valueCount        = valueCount;
        this.elementStructName = elementStructName;
    }

    /** One size per dimension, outermost first; -1 where it is not constant. */
    public List<Integer> getDimensions()
    {
        return dimensions;
    }

    public int getRank()
    {
        return dimensions.size();
    }

    public String getElementStructName()
    {
        return elementStructName;
    }

    public void markRebound()
    {
        rebound = true;
    }

    public boolean isRebound()
    {
        return rebound;
    }

    @Override
    public String getDetail()
    {
        String sizes = dimensions.stream()
                .map(size -> size < 0 ? "?" : String.valueOf(size))
                .collect(Collectors.joining(" x "));

        return "tamano = " + sizes + ", valores = " + valueCount;
    }
}
