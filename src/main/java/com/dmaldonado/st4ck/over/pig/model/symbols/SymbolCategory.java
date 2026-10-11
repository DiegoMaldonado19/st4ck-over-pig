package com.dmaldonado.st4ck.over.pig.model.symbols;

/**
 * What a symbol IS, beyond its type.
 *
 * The class name is not enough: VariableSymbol is reused for plain variables,
 * function parameters and structura attributes, which the symbol table graph
 * has to tell apart in its own column.
 */
public enum SymbolCategory
{
    VARIABLE("Variable"),
    PARAMETER("Parametro"),
    ARRAY("Arreglo"),
    FUNCTION("Funcion"),
    METHOD("Metodo"),
    CONSTRUCTOR("Constructor"),
    STRUCT("Estructura"),
    CLASS("Clase"),
    ATTRIBUTE("Atributo");

    private final String label;

    SymbolCategory(String label)
    {
        this.label = label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
