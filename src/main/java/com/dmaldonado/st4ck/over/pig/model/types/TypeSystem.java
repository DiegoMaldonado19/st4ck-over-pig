package com.dmaldonado.st4ck.over.pig.model.types;

/**
 * Reglas de compatibilidad de tipos. Todo metodo devuelve ERROR cuando la
 * combinacion es invalida, y el analizador sigue caminando con ese ERROR.
 *
 * La tabla completa esta en docs/MANUAL_TECNICO.docx, "Tabla de compatibilidad de tipos".
 */
public final class TypeSystem
{
    private TypeSystem()
    {
    }

    /* ================= Arithmetic: + - * / ================= */

    public static DataType arithmeticResult(DataType left, DataType right, String operator)
    {
        // A whole structure or a void call is no value to operate with; textum
        // + Persona would have nothing to turn Persona into.
        if (!left.isPrimitive() || !right.isPrimitive())
        {
            return DataType.ERROR;
        }

        // textum only takes part through '+' (concatenation), with any primitive.
        if (left == DataType.TEXTUM || right == DataType.TEXTUM)
        {
            return "+".equals(operator) ? DataType.TEXTUM : DataType.ERROR;
        }
        if (left == DataType.BOOLEANO || right == DataType.BOOLEANO)
        {
            return DataType.ERROR;
        }
        if (!left.isNumeric() || !right.isNumeric())
        {
            return DataType.ERROR;
        }
        // Zetariano's % is the integer remainder: a decimal operand has none.
        if ("%".equals(operator))
        {
            return left == DataType.DECIMALIS || right == DataType.DECIMALIS
                    ? DataType.ERROR : DataType.NUMERUS;
        }

        // Two littera make a numerus, as in Java: 'a' + 'b' is 195, not a character.
        if (left == DataType.LITTERA && right == DataType.LITTERA)
        {
            return DataType.NUMERUS;
        }
        // Highest rank wins: littera < numerus < decimalis
        return left.getRank() >= right.getRank() ? left : right;
    }

    /* ================= Relational: == != < > <= >= ================= */

    public static DataType relationalResult(DataType left, DataType right, String operator)
    {
        boolean isEquality = "==".equals(operator) || "!=".equals(operator);

        // Objects compare by identity, and anything that may hold null against it: p1 != p2, s == null.
        if (isEquality && (isReference(left) && isReference(right)
                || left == DataType.NULO && isAssignable(right, DataType.NULO)
                || right == DataType.NULO && isAssignable(left, DataType.NULO)))
        {
            return DataType.BOOLEANO;
        }
        if (!left.isPrimitive() || !right.isPrimitive())
        {
            return DataType.ERROR;
        }

        // == and != work between equal types, textum and bool included.
        if (isEquality && left == right)
        {
            return DataType.BOOLEANO;
        }
        // < > <= >= only between numeric types.
        if (left.isNumeric() && right.isNumeric())
        {
            return DataType.BOOLEANO;
        }
        return DataType.ERROR;
    }

    /** Held by address in the heap: an object, a structura, or the null that fits them. */
    private static boolean isReference(DataType type)
    {
        return type == DataType.ESTRUCTURA || type == DataType.NULO;
    }

    /* ================= Logical: && || non ================= */

    public static DataType logicalResult(DataType left, DataType right)
    {
        return (left == DataType.BOOLEANO && right == DataType.BOOLEANO)
                ? DataType.BOOLEANO : DataType.ERROR;
    }

    public static DataType negationResult(DataType operand)
    {
        return operand == DataType.BOOLEANO ? DataType.BOOLEANO : DataType.ERROR;
    }

    /** -'a' is a number, not a character: there is no negative character. */
    public static DataType unaryMinusResult(DataType operand)
    {
        if (operand == DataType.LITTERA)
        {
            return DataType.NUMERUS;
        }
        return operand.isNumeric() ? operand : DataType.ERROR;
    }

    /** ++ and -- only apply to numerus / decimalis. */
    public static DataType incrementResult(DataType operand)
    {
        return (operand == DataType.NUMERUS || operand == DataType.DECIMALIS)
                ? operand : DataType.ERROR;
    }

    /* ================= Assignment ================= */

    /**
     * Puede guardarse un valor {@code source} en un destino {@code target}?
     * Solo se ensancha (littera -> numerus -> decimalis), nunca se estrecha.
     *
     * Dos structuras distintas son ambas ESTRUCTURA aqui: comparar el NOMBRE le
     * toca a SemanticAnalyzer.assignable, que es quien los conoce.
     */
    public static boolean isAssignable(DataType target, DataType source)
    {
        if (target == DataType.ERROR || source == DataType.ERROR)
        {
            return true;   // an error was already reported: do not duplicate it
        }
        if (target == source)
        {
            return true;
        }
        if (source == DataType.NULO)
        {
            return target == DataType.ESTRUCTURA || target == DataType.TEXTUM;
        }
        if (target == DataType.DECIMALIS)
        {
            return source == DataType.NUMERUS || source == DataType.LITTERA;
        }
        if (target == DataType.NUMERUS)
        {
            return source == DataType.LITTERA;
        }
        return false;
    }
}
