package com.dmaldonado.st4ck.over.pig.model.codegen;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cuartetas -> C que compila con gcc. Cada cuarteta es una linea de C, asi la
 * correspondencia se lee de un vistazo; el control de flujo es goto y
 * etiquetas, como en el codigo de tres direcciones.
 *
 * El prologo trae la memoria (stack, heap, P, H) y las funciones nativas. Cada
 * bloque function ... end de las cuartetas es una funcion C, con sus
 * temporales declarados como locales: eso es lo que hace segura la recursion.
 */
public final class CEmitter
{
    private static final Pattern TEMPORARY = Pattern.compile("t\\d+");
    private static final Pattern INTEGER   = Pattern.compile("-?\\d+");

    private static final Set<String> JUMPS = Set.of("goto", "if", "ifFalse");

    /** The natives take and return C values, as Docs/03 3.4 specifies. */
    private static final String RUNTIME = """
            #include <float.h>
            #include <stdint.h>
            #include <stdio.h>
            #include <stdlib.h>
            #include <string.h>

            #define MEMORY_SIZE   1000000
            /* Ningun marco llega a ese tamano: el chequeo del stack deja ese margen. */
            #define FRAME_MARGIN  1000
            /* Cada llamada del programa tambien es una llamada de C, con sus temporales en el
               stack de C: se detiene antes de agotar los 8 MB que Linux le da por defecto. */
            #define C_STACK_LIMIT (7 * 1024 * 1024)

            double stack[MEMORY_SIZE];   /* marcos de las funciones; P es la base del actual   */
            double heap[MEMORY_SIZE];    /* cadenas, arreglos y estructuras; H, la celda libre */
            double P;
            double H;
            uintptr_t stackBase;        /* donde empezaba el stack de C al entrar a main       */

            /* ------------------------------------------------------------------
             * Nativas: el lenguaje las usa sin declararlas. Una cadena es una
             * secuencia de bytes UTF-8 en el heap, terminada en -1.
             * ------------------------------------------------------------------ */

            void checkMemory(void)
            {
                char      here;
                uintptr_t current = (uintptr_t) &here;
                uintptr_t used    = stackBase > current ? stackBase - current : current - stackBase;

                if (H > MEMORY_SIZE || P + FRAME_MARGIN > MEMORY_SIZE || used > C_STACK_LIMIT)
                {
                    printf("\\nError: memoria agotada (stack o heap).\\n");
                    exit(1);
                }
            }

            /* Un numerus desbordado (2147483647 + 1) vale como en Java: sus 32 bits bajos. Pasado 2^63 no
               cabe ni en long long (convertirlo seria indefinido en C) y ya no guarda bits exactos: 0.
               ponytail: + - * no se envuelven al calcularse, solo al imprimirse, dividirse o en un residuo;
               es exacto hasta 2^53. Para el int de Java en todo, envolver cada resultado numerus en
               QuadrupleGenerator.binary() e increment(), con * en long long. */
            int wrapInt(double value)
            {
                return value > -9.2e18 && value < 9.2e18 ? (int) (long long) value : 0;
            }

            /* Un objeto o arreglo sin crear es null (0): usarlo detiene el programa. */
            void checkNull(double pointer)
            {
                if (pointer == 0)
                {
                    printf("\\nError: referencia nula (objeto o arreglo sin crear).\\n");
                    exit(1);
                }
            }

            /* Un arreglo guarda su largo en la celda anterior a la primera: salirse de el detiene el programa.
               En una matriz es la celda aplanada, revisada despues de cada indice interno. */
            void checkIndex(double array, double index)
            {
                checkNull(array);
                if (index < 0 || index >= heap[(int) array - 1])
                {
                    printf("\\nError: posicion %.0f fuera de un arreglo de %d posiciones.\\n", index,
                           (int) heap[(int) array - 1]);
                    exit(1);
                }
            }

            void checkBound(double index, double size)
            {
                if (index < 0 || index >= size)
                {
                    printf("\\nError: indice %.0f fuera de una dimension de %d posiciones.\\n", index,
                           (int) size);
                    exit(1);
                }
            }

            /* Un tamano negativo haria retroceder a H sobre lo que ya vive en el heap. */
            void checkSize(double size)
            {
                if (size < 0)
                {
                    printf("\\nError: tamano de arreglo negativo (%.0f).\\n", size);
                    exit(1);
                }
            }

            /* Division y residuo enteros: en C, dividir un int entre cero aborta el programa, y tambien
               -2147483648 / -1 en int; en long long ese cociente cabe. */
            void checkDivisor(double right)
            {
                if (wrapInt(right) == 0)
                {
                    printf("\\nError: division entre cero.\\n");
                    exit(1);
                }
            }

            double intDivide(double left, double right)
            {
                checkDivisor(right);
                return (double) ((long long) wrapInt(left) / (long long) wrapInt(right));
            }

            double intModulo(double left, double right)
            {
                checkDivisor(right);
                return (double) ((long long) wrapInt(left) % (long long) wrapInt(right));
            }

            /* Cada lenguaje escribe sus booleanos: 0 PigLatin, 1 Y?, 2 Zetariano. */
            const char *TRUE_WORDS[]  = { "verum", "verdadero", "true" };
            const char *FALSE_WORDS[] = { "falsus", "falso", "false" };

            double reserve(int size)
            {
                double start = H;

                H = H + size;
                checkMemory();
                return start;
            }

            /* Copia un texto de C al heap y devuelve su direccion. */
            double textFromBytes(const char *text)
            {
                double start = reserve((int) strlen(text) + 1);
                int   k     = (int) start;

                for (int i = 0; text[i] != '\\0'; i++)
                {
                    heap[k++] = (unsigned char) text[i];
                }
                heap[k] = -1;
                return start;
            }

            /* Un littera es un code point; en la consola va en UTF-8. */
            void encodeUtf8(int code, char *text)
            {
                if (code < 0x80)
                {
                    text[0] = (char) code;
                    text[1] = '\\0';
                }
                else if (code < 0x800)
                {
                    text[0] = (char) (0xC0 | (code >> 6));
                    text[1] = (char) (0x80 | (code & 0x3F));
                    text[2] = '\\0';
                }
                else if (code < 0x10000)
                {
                    text[0] = (char) (0xE0 | (code >> 12));
                    text[1] = (char) (0x80 | ((code >> 6) & 0x3F));
                    text[2] = (char) (0x80 | (code & 0x3F));
                    text[3] = '\\0';
                }
                else
                {
                    text[0] = (char) (0xF0 | (code >> 18));
                    text[1] = (char) (0x80 | ((code >> 12) & 0x3F));
                    text[2] = (char) (0x80 | ((code >> 6) & 0x3F));
                    text[3] = (char) (0x80 | (code & 0x3F));
                    text[4] = '\\0';
                }
            }

            void printInt(double value)
            {
                printf("%d", wrapInt(value));
            }

            /* Un decimal con las cifras que su double guarda: 12345.75, 0.1, 2.0. Desde 1e16 sus cifras enteras
               ya no son exactas, y lo muy pequeno no cabe en 17 decimales: esos van con exponente. */
            void floatText(double value, char *text, size_t size)
            {
                if (value != value || value > DBL_MAX || value < -DBL_MAX)
                {
                    snprintf(text, size, "%s", value != value ? "NaN" : value > 0 ? "Infinity" : "-Infinity");
                    return;
                }
                for (int decimals = 1; decimals <= 17 && value < 1e16 && value > -1e16; decimals++)
                {
                    snprintf(text, size, "%.*f", decimals, value);
                    if (strtod(text, NULL) == value)
                    {
                        return;
                    }
                }
                for (int digits = 1; digits <= 17; digits++)
                {
                    snprintf(text, size, "%.*g", digits, value);
                    if (strtod(text, NULL) == value)
                    {
                        return;
                    }
                }
            }

            void printFloat(double value)
            {
                char text[64];

                floatText(value, text, sizeof text);
                fputs(text, stdout);
            }

            void printChar(double value)
            {
                char text[5];

                encodeUtf8((int) value, text);
                fputs(text, stdout);
            }

            void printBool(double value, double language)
            {
                fputs(value != 0 ? TRUE_WORDS[(int) language] : FALSE_WORDS[(int) language], stdout);
            }

            void printString(double pointer)
            {
                for (int i = (int) pointer; heap[i] != -1; i++)
                {
                    putchar((int) heap[i]);
                }
            }

            void printLine(void)
            {
                putchar('\\n');
            }

            /* Lo que no cupo en el buffer se descarta: si no, seria la siguiente lectura. */
            void discardLine(void)
            {
                int character;

                do
                {
                    character = getchar();
                }
                while (character != '\\n' && character != EOF);
            }

            /* Cada lectura toma una linea completa, aunque este en blanco: lo que sobre de ella no pasa a la siguiente.
               Sin mas entrada se detiene, como el Scanner de Java: leer 0 para siempre dejaria un menu en un ciclo sin fin. */
            void readLine(char *text, int size)
            {
                size_t length;

                if (fgets(text, size, stdin) == NULL)
                {
                    printf("\\nError: no hay mas datos en la entrada.\\n");
                    exit(1);
                }
                length = strcspn(text, "\\r\\n");
                if (text[length] == '\\0' && !feof(stdin))
                {
                    discardLine();
                }
                text[length] = '\\0';
            }

            /* Lo que no es un numero lee 0. */
            double readInt(void)
            {
                char text[1024];

                readLine(text, sizeof text);
                return (double) strtol(text, NULL, 10);
            }

            double readFloat(void)
            {
                char text[1024];

                readLine(text, sizeof text);
                return (double) strtod(text, NULL);
            }

            /* El primer simbolo de la linea, decodificado de UTF-8: una ñ es un solo caracter. */
            double readChar(void)
            {
                char           text[1024];
                unsigned char *first;
                int            extra;
                int            code;

                readLine(text, sizeof text);
                first = (unsigned char *) text + strspn(text, " \\t");
                extra = *first >= 0xF0 ? 3 : *first >= 0xE0 ? 2 : *first >= 0xC0 ? 1 : 0;
                code  = extra == 0 ? *first : *first & (0x3F >> extra);

                for (int i = 1; i <= extra && first[i] != '\\0'; i++)
                {
                    code = (code << 6) | (first[i] & 0x3F);
                }
                return (double) code;
            }

            /* verum, verdadero o true (la palabra de cualquiera de los tres lenguajes), o un numero no 0. */
            double readBool(void)
            {
                char text[1024];

                readLine(text, sizeof text);
                for (int i = 0; i < 3; i++)
                {
                    if (strcmp(text, TRUE_WORDS[i]) == 0)
                    {
                        return 1;
                    }
                }
                return strtol(text, NULL, 10) != 0;
            }

            double readString(void)
            {
                char text[1024];

                readLine(text, sizeof text);
                return textFromBytes(text);
            }

            int stringLength(double pointer)
            {
                int length = 0;

                for (int i = (int) pointer; heap[i] != -1; i++)
                {
                    length++;
                }
                return length;
            }

            double concat(double left, double right)
            {
                double start = reserve(stringLength(left) + stringLength(right) + 1);
                int   k     = (int) start;

                for (int i = (int) left; heap[i] != -1; i++)
                {
                    heap[k++] = heap[i];
                }
                for (int i = (int) right; heap[i] != -1; i++)
                {
                    heap[k++] = heap[i];
                }
                heap[k] = -1;
                return start;
            }

            /* Por contenido, no por direccion: dos literales iguales pueden vivir en celdas distintas.
               Un textum sin valor (0) lee heap[0], la cadena vacia, como al imprimirlo o concatenarlo. */
            double stringEquals(double left, double right)
            {
                int i = (int) left;
                int j = (int) right;

                while (heap[i] != -1 && heap[i] == heap[j])
                {
                    i++;
                    j++;
                }
                return heap[i] == heap[j];
            }

            /* Zetariano tiene null: null solo es igual a null, y "" es otra cadena. */
            double stringEqualsOrNull(double left, double right)
            {
                if (left == 0 || right == 0)
                {
                    return left == right;
                }
                return stringEquals(left, right);
            }

            double intToString(double value)
            {
                char text[16];

                snprintf(text, sizeof text, "%d", wrapInt(value));
                return textFromBytes(text);
            }

            double floatToString(double value)
            {
                char text[64];

                floatText(value, text, sizeof text);
                return textFromBytes(text);
            }

            double charToString(double value)
            {
                char text[5];

                encodeUtf8((int) value, text);
                return textFromBytes(text);
            }

            double boolToString(double value, double language)
            {
                return textFromBytes(value != 0 ? TRUE_WORDS[(int) language] : FALSE_WORDS[(int) language]);
            }
            """;

    private CEmitter()
    {
    }

    public static String emit(List<Quadruple> code)
    {
        StringBuilder c = new StringBuilder(RUNTIME);

        c.append("\n/* ------------------------------------------------------------------\n")
         .append(" * El programa\n")
         .append(" * ------------------------------------------------------------------ */\n\n");

        // Prototipos: una funcion puede llamar a otra declarada despues, o a si misma.
        for (Quadruple quadruple : code)
        {
            if ("function".equals(quadruple.op()) && !"main".equals(quadruple.result()))
            {
                c.append("void ").append(quadruple.result()).append("(void);\n");
            }
        }

        int start = 0;

        for (int i = 0; i < code.size(); i++)
        {
            if ("end".equals(code.get(i).op()))
            {
                emitFunction(code.subList(start + 1, i), code.get(start).result(), c);
                start = i + 1;
            }
        }
        return c.toString();
    }

    /**
     * One C function. An unreferenced label is left out, so -Wall stays quiet;
     * a label that closes the function gets an empty statement, which C
     * requires after a label.
     */
    private static void emitFunction(List<Quadruple> body, String name, StringBuilder c)
    {
        Set<String> temporaries = new TreeSet<>(Comparator.comparingInt(t -> Integer.parseInt(t.substring(1))));
        Set<String> targets     = new HashSet<>();

        for (Quadruple quadruple : body)
        {
            Stream.of(quadruple.arg1(), quadruple.arg2(), quadruple.result())
                  .filter(operand -> TEMPORARY.matcher(operand).matches())
                  .forEach(temporaries::add);

            if (JUMPS.contains(quadruple.op()))
            {
                targets.add(quadruple.result());
            }
        }

        c.append('\n').append("main".equals(name) ? "int main(void)" : "void " + name + "(void)")
         .append("\n{\n");

        if ("main".equals(name))
        {
            c.append("    char base;\n");
        }
        if (!temporaries.isEmpty())
        {
            c.append("    double ").append(String.join(", ", temporaries)).append(";\n");
        }
        if ("main".equals(name))
        {
            c.append("\n    stackBase = (uintptr_t) &base;\n");
        }
        c.append('\n');

        for (int i = 0; i < body.size(); i++)
        {
            Quadruple quadruple = body.get(i);

            if (!"label".equals(quadruple.op()))
            {
                c.append("    ").append(translate(quadruple)).append('\n');
            }
            else if (targets.contains(quadruple.result()))
            {
                c.append(quadruple.result()).append(i == body.size() - 1 ? ": ;" : ":").append('\n');
            }
        }

        if ("main".equals(name))
        {
            c.append("    return 0;\n");
        }
        c.append("}\n");
    }

    private static String translate(Quadruple q)
    {
        return switch (q.op())
        {
            case "="                                             -> q.result() + " = " + q.arg1() + ";";
            case "+", "-", "*", "/" ->
                    q.result() + " = " + number(q.arg1()) + " " + q.op() + " " + number(q.arg2()) + ";";
            case "<", ">", "<=", ">=", "==", "!=" ->
                    q.result() + " = " + q.arg1() + " " + q.op() + " " + q.arg2() + ";";
            case "div"     -> q.result() + " = intDivide(" + q.arg1() + ", " + q.arg2() + ");";
            case "mod"     -> q.result() + " = intModulo(" + q.arg1() + ", " + q.arg2() + ");";
            case "=[]"     -> q.result() + " = " + q.arg1() + "[" + index(q.arg2()) + "];";
            case "[]="     -> q.result() + "[" + index(q.arg1()) + "] = " + q.arg2() + ";";
            case "goto"    -> "goto " + q.result() + ";";
            case "if"      -> "if (" + q.arg1() + ") goto " + q.result() + ";";
            case "ifFalse" -> "if (!" + q.arg1() + ") goto " + q.result() + ";";
            case "call"    -> q.result() + "();";
            case "return"  -> "return;";
            default        -> nativeCall(q);
        };
    }

    /** Any other op names a native: result = op(arg1, arg2), leaving out the empty parts. */
    private static String nativeCall(Quadruple q)
    {
        String arguments = Stream.of(q.arg1(), q.arg2())
                                 .filter(argument -> !argument.isEmpty())
                                 .collect(Collectors.joining(", "));
        String call      = q.op() + "(" + arguments + ");";

        return q.result().isEmpty() ? call : q.result() + " = " + call;
    }

    /** 65536 * 65536 would be int arithmetic in C, and overflow: every value is a double, constants too. */
    private static String number(String operand)
    {
        return INTEGER.matcher(operand).matches() ? operand + ".0" : operand;
    }

    /** An array subscript has to be an int in C; a constant already is one. */
    private static String index(String operand)
    {
        return INTEGER.matcher(operand).matches() ? operand : "(int) " + operand;
    }
}
