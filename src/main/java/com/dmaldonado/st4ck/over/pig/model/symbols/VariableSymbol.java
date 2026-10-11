package com.dmaldonado.st4ck.over.pig.model.symbols;

import com.dmaldonado.st4ck.over.pig.model.types.DataType;

/**
 * A variable, a function parameter or a structura attribute.
 *
 * The array flag only matters for attributes: inside a structura a field is
 * declared "series animales : Animal" with NO size, and
 * the size arrives later, when the variable is created, as "animales:
 * Animal[7]".
 * Without this flag the analyzer cannot tell that form apart from a plain
 * value.
 */
public class VariableSymbol extends Symbol {
    private final boolean initialized;
    /** Name of the structura when the type is ESTRUCTURA. */
    private final String structName;
    private final boolean array;

    public VariableSymbol(String name, DataType type, String typeText, SymbolCategory category,
            String scopeName, boolean initialized, String structName,
            boolean array, int line, int column) {
        super(name, type, typeText, category, scopeName, line, column);
        this.initialized = initialized;
        this.structName = structName;
        this.array = array;
    }

    public String getStructName() {
        return structName;
    }

    public boolean isArray() {
        return array;
    }

    @Override
    public String getDetail() {
        String detail = initialized ? "inicializada" : "sin valor inicial";
        return (array ? "arreglo, " + detail : detail) + describeStorage();
    }
}
