package com.dmaldonado.st4ck.over.pig.model.types;

/**
 * Primitive types of the Latin language, plus the markers the compiler needs
 * for things that are not primitive.
 *
 * The statement defines an implicit conversion hierarchy:
 *   textum 5, decimalis 4, numerus 3, littera 2, booleano 1.
 * It is modelled as a rank so that the result of an operation is simply the
 * operand with the higher rank. TypeSystem is what applies that rule.
 */
public enum DataType
{
    TEXTUM("textum", 5),
    DECIMALIS("decimalis", 4),
    NUMERUS("numerus", 3),
    LITTERA("littera", 2),
    BOOLEANO("bool", 1),

    /** Return type of a function declared with actio. */
    VOID("void", 0),
    /** Type of an instance of a user defined structura, or of a Zetariano class. */
    ESTRUCTURA("structura", 0),
    /** Type of the null literal: it fits any object, structura or text, never a number. */
    NULO("null", 0),
    /** Error marker: keeps one real error from cascading into ten messages. */
    ERROR("error", -1);

    private final String latinName;
    private final int    rank;

    DataType(String latinName, int rank)
    {
        this.latinName = latinName;
        this.rank      = rank;
    }

    public String getLatinName()
    {
        return latinName;
    }

    public int getRank()
    {
        return rank;
    }

    /** The five value types: textum, decimalis, numerus, littera and bool. */
    public boolean isPrimitive()
    {
        return rank > 0;
    }

    /** numerus, decimalis and littera are the ones arithmetic accepts. */
    public boolean isNumeric()
    {
        return this == NUMERUS || this == DECIMALIS || this == LITTERA;
    }

    /**
     * Maps the type written in the source, in PigLatin, Y? or Zetariano, to its
     * DataType. "bool" is the explicit boolean type; "verum"/"falsus" are the
     * older form PigLatin still accepts as a type. Anything else is a user
     * defined structure or class.
     */
    public static DataType fromText(String text)
    {
        if (text == null)
        {
            return ERROR;
        }

        return switch (text)
        {
            case "numerus", "entero", "int"                -> NUMERUS;
            case "decimalis", "flotante", "double"         -> DECIMALIS;
            case "textum", "cadena", "String"              -> TEXTUM;
            case "littera", "caracter", "char"             -> LITTERA;
            case "bool", "verum", "falsus", "boolean"      -> BOOLEANO;
            case "void"                                    -> VOID;
            default                                        -> ESTRUCTURA;
        };
    }

    /** Error messages read "se recibio textum", not "se recibio TEXTUM". */
    @Override
    public String toString()
    {
        return latinName;
    }
}
