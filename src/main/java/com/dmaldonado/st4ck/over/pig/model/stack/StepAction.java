package com.dmaldonado.st4ck.over.pig.model.stack;

/**
 * Operaciones del panel de la pila
 */
public enum StepAction {
    SHIFT("SHIFT"),
    PUSH("PUSH (regla activa)"),
    REDUCE("REDUCE / REPLACE"),
    ACCEPT("ACCEPT"),
    ERROR("ERROR");

    private final String label;

    StepAction(String label) {
        this.label = label;
    }

    /** What a ListView cell shows without any extra formatting. */
    @Override
    public String toString() {
        return label;
    }
}
