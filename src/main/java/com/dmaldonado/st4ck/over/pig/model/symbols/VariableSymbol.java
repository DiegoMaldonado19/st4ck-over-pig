package com.dmaldonado.st4ck.over.pig.model.symbols;

import com.dmaldonado.st4ck.over.pig.model.types.DataType;

/**
 * A variable, a function parameter or a structura attribute.
 *
 * The rank only matters for attributes and parameters: inside a structura a
 * field is declared "series animales : Animal" with NO size, and the size
 * arrives later, when the variable is created, as "animales: Animal[7]"; an
 * attribute "int[][] celdas" gets its sizes from new. Without the rank the
 * analyzer could not tell those apart from a plain value, nor index them.
 */
public class VariableSymbol extends Symbol {
    private final boolean initialized;
    /** Name of the structura when the type is ESTRUCTURA. */
    private final String structName;
    private final int rank;

    public VariableSymbol(String name, DataType type, String typeText, SymbolCategory category,
            String scopeName, boolean initialized, String structName,
            int rank, int line, int column) {
        super(name, type, typeText, category, scopeName, line, column);
        this.initialized = initialized;
        this.structName = structName;
        this.rank = rank;
    }

    public String getStructName() {
        return structName;
    }

    public boolean isArray() {
        return rank > 0;
    }

    /** Dimensions of an array attribute or parameter; 0 for a value. */
    public int getRank() {
        return rank;
    }

    @Override
    public String getDetail() {
        String detail = initialized ? "inicializada" : "sin valor inicial";
        return isArray() ? "arreglo, " + detail : detail;
    }
}
