lexer grammar YLexer;

/*
 * Y? usa la sangria para delimitar bloques, como Python. ANTLR no trae eso:
 * NUEVA_LINEA consume el salto y la sangria de la linea siguiente, y desde su
 * accion se encolan los INDENT/DEDENT que el parser si vera.
 *
 * Estos tokens se declaran en 'tokens' porque no tienen regla propia: nacen
 * en codigo, no del texto.
 */
tokens { INDENT, DEDENT, SANGRIA_INVALIDA }

@lexer::header {
import java.util.ArrayDeque;
import java.util.Deque;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.IntStream;
}

@lexer::members {
    /** Tokens fabricados que aun no se han entregado al parser. */
    private final Deque<Token>   pending = new ArrayDeque<>();

    /** Anchos de sangria abiertos; la cima es el bloque actual. */
    private final Deque<Integer> indents = new ArrayDeque<>();

    /**
     * Cada '{' abierto: true si es un literal, false si es el cuerpo de un
     * elegir, que si usa la sangria para sus casos.
     */
    private final Deque<Boolean> braces  = new ArrayDeque<>();

    /** Parentesis, corchetes y literales abiertos: dentro, un salto de linea no es sangria. */
    private int                  joined;

    /** Tipo del ultimo token visible, para saber si una '{' abre un literal. */
    private int                  lastType;

    /**
     * Se entrega primero lo encolado. Cuando no hay nada encolado se pide el
     * siguiente token real: si es NUEVA_LINEA su accion habra llenado la cola,
     * y esos INDENT/DEDENT saldran en las llamadas siguientes, justo despues
     * del salto de linea que los provoco.
     */
    @Override
    public Token nextToken()
    {
        if (!pending.isEmpty())
        {
            return pending.poll();
        }

        Token token = super.nextToken();

        if (token.getChannel() == DEFAULT_TOKEN_CHANNEL)
        {
            lastType = token.getType();
        }
        if (token.getType() == EOF)
        {
            // Un archivo puede terminar dentro de varios bloques abiertos: hay
            // que cerrarlos todos antes del EOF o el parser nunca reduce.
            while (!indents.isEmpty())
            {
                indents.pop();
                pending.add(makeToken(DEDENT));
            }
            pending.add(token);
            return pending.poll();
        }
        return token;
    }

    /**
     * El tabulador avanza hasta el siguiente multiplo de 8, como Python. Mezclar
     * tabuladores y espacios en el mismo archivo sigue siendo mala idea, pero al
     * menos aqui la cuenta es la que el usuario espera.
     */
    private void handleNewLine()
    {
        int next = _input.LA(1);

        // 'entonces', 'hacer' o ':' nunca van dentro de un ( [ o literal: si hay uno
        // abierto se olvido cerrarlo, y el bloque de la linea siguiente si cuenta.
        if (lastType == ENTONCES || lastType == HACER || lastType == DOS_PUNTOS)
        {
            joined = 0;
        }

        // Linea en blanco o que solo lleva comentario: no abre ni cierra bloque.
        // Dentro de ( [ o de un literal { la linea continua, como en Python.
        if (next == '\r' || next == '\n' || next == IntStream.EOF || joined > 0 || commentOnly())
        {
            return;
        }

        String text  = getText();
        int    width = 0;

        for (int i = 0; i < text.length(); i++)
        {
            char character = text.charAt(i);

            if (character == ' ')
            {
                width++;
            }
            else if (character == '\t')
            {
                width += 8 - (width % 8);
            }
        }

        int current = indents.isEmpty() ? 0 : indents.peek();

        if (width > current)
        {
            indents.push(width);
            pending.add(makeToken(INDENT));
        }
        else
        {
            while (!indents.isEmpty() && indents.peek() > width)
            {
                indents.pop();
                pending.add(makeToken(DEDENT));
            }
            // Como Python: volver a una sangria que ningun bloque abierto tiene es un error.
            if (width > (indents.isEmpty() ? 0 : indents.peek()))
            {
                pending.add(makeToken(SANGRIA_INVALIDA));
            }
        }
    }

    /** Solo comentarios hasta el fin de la linea: bloques de comentario seguidos de nada o de un '//'. */
    private boolean commentOnly()
    {
        int k = 1;

        while (_input.LA(k) == '/' && _input.LA(k + 1) == '*')
        {
            for (k += 2; _input.LA(k) != IntStream.EOF && !(_input.LA(k) == '*' && _input.LA(k + 1) == '/'); k++)
            {
            }
            for (k += 2; _input.LA(k) == ' ' || _input.LA(k) == '\t'; k++)
            {
            }
        }

        int next = _input.LA(k);

        return (next == '/' && _input.LA(k + 1) == '/')
            || (k > 1 && (next == '\r' || next == '\n' || next == IntStream.EOF));
    }

    /** Un '{' tras '=', ',', '(', '{' o 'retornar' abre un literal; tras ')' es el cuerpo de un elegir. */
    private void openBrace()
    {
        boolean literal = lastType == ASIGNACION || lastType == COMA || lastType == PAR_IZQ
                || lastType == LLAVE_IZQ || lastType == RETORNAR;

        braces.push(literal);
        if (literal)
        {
            joined++;
        }
    }

    private void closeBrace()
    {
        if (!braces.isEmpty() && braces.pop())
        {
            joined--;
        }
    }

    private void closeGroup()
    {
        if (joined > 0)
        {
            joined--;
        }
    }

    /**
     * Token de ancho cero en la posicion actual: no tapa texto del archivo. Lleva
     * su fuente: al reparar un error, ANTLR la lee para fabricar el token que falta.
     */
    private Token makeToken(int type)
    {
        CommonToken token = new CommonToken(_tokenFactorySourcePair, type, DEFAULT_TOKEN_CHANNEL,
                getCharIndex(), getCharIndex() - 1);

        token.setText("");
        token.setLine(getLine());
        token.setCharPositionInLine(getCharPositionInLine());
        return token;
    }
}

/* ---------- 1. Marcadores de seccion --------------------------------- */
SECCION_ESTRUCTURAS : '%estructuras' ;   // opcional
SECCION_FUNCIONES   : '%funciones' ;     // obligatoria

/* ---------- 2. Palabras reservadas ---------------------------------- */
// Declaracion
ESTRUCTURA : 'estructura' ;
DEFINIR    : 'definir' ;
RETORNAR   : 'retornar' ;

// Tipos
ENTERO_T   : 'entero' ;
CADENA_T   : 'cadena' ;
FLOTANTE_T : 'flotante' ;
CARACTER_T : 'caracter' ;
BOOL_T     : 'bool' ;
VERDADERO  : 'verdadero' ;
FALSO      : 'falso' ;

// Control de flujo
SI         : 'si' ;
ENTONCES   : 'entonces' ;
SINO       : 'sino' ;
CONTRARIO  : 'contrario' ;
ELEGIR     : 'elegir' ;
CASO       : 'caso' ;
SIEMPRE    : 'siempre' ;
ROMPER     : 'romper' ;
CONTINUAR  : 'continuar' ;
PARA       : 'para' ;
MIENTRAS   : 'mientras' ;
HACER      : 'hacer' ;

// Funciones del sistema: no se declaran
IMPRIMIR   : 'imprimir' ;
LEER       : 'leer' ;

/* ---------- 3. Operadores de 2 caracteres --------------------------- */
FLECHA      : '->' ;                 // tipo de retorno: definir f() -> entero
INCREMENTO  : '++' ;
DECREMENTO  : '--' ;
IGUALDAD    : '==' ;
DIFERENTE   : '!=' ;
MENOR_IGUAL : '<=' ;
MAYOR_IGUAL : '>=' ;
AND         : '&&' ;
OR          : '||' ;

/* ---------- 4. Operadores de 1 caracter ----------------------------- */
MAS        : '+' ;
MENOS      : '-' ;
POR        : '*' ;
DIVISION   : '/' ;
MENOR      : '<' ;
MAYOR      : '>' ;
ASIGNACION : '=' ;
NEGACION   : '!' ;

/* ---------- 5. Signos de puntuacion --------------------------------- */
PAR_IZQ    : '(' { joined++; } ;
PAR_DER    : ')' { closeGroup(); } ;
LLAVE_IZQ  : '{' { openBrace(); } ;
LLAVE_DER  : '}' { closeBrace(); } ;
COR_IZQ    : '[' { joined++; } ;
COR_DER    : ']' { closeGroup(); } ;
PUNTO_COMA : ';' ;
DOS_PUNTOS : ':' ;
COMA       : ',' ;
PUNTO      : '.' ;

/* ---------- 6. Literales -------------------------------------------- */
DECIMAL   : DIGITO+ '.' DIGITO* ;    // antes que ENTERO; '36.' es un ejemplo del enunciado
ENTERO    : DIGITO+ ;
TEXTO     : '"' EN_TEXTO* '"' ;
CARACTER  : '\'' ( ESCAPE | ~['\\\r\n] ) '\'' ;

/* ---------- 7. Identificadores -------------------------------------- */
ID : ( LETRA | '_' ) ( LETRA | DIGITO | '_' )* ;

/* ---------- 8. Sangria y ruido ---------------------------------------
 * NUEVA_LINEA se queda con el salto Y con la sangria de la linea siguiente,
 * porque para decidir el INDENT hay que verlas juntas. Dentro de parentesis,
 * corchetes o un literal { } la sangria no cuenta: asi una matriz se puede
 * escribir fila por fila (aclaracion de la auxiliar, 21/09/2026).
 * -------------------------------------------------------------------- */
NUEVA_LINEA : ( '\r'? '\n' | '\r' ) [ \t]* { handleNewLine(); } -> channel(HIDDEN) ;

COMENTARIO_LINEA  : '//' ~[\r\n]* -> channel(HIDDEN) ;
COMENTARIO_BLOQUE : '/*' .*? '*/' -> channel(HIDDEN) ;
ESPACIOS          : [ \t\f]+      -> channel(HIDDEN) ;

/* ---------- 9. Errores lexicos --------------------------------------- */
TEXTO_SIN_CERRAR      : '"'  EN_TEXTO* ;
CARACTER_LARGO        : '\'' ( ESCAPE | ~['\\\r\n] ) ( ESCAPE | ~['\\\r\n] )+ '\'' ;
CARACTER_MAL_FORMADO  : '\'' ( '\\' ~[nrt"'\\\r\n] )? '\'' ;
CARACTER_SIN_CERRAR   : '\'' ( ESCAPE | ~['\\\r\n] )? ;
COMENTARIO_SIN_CERRAR : '/*' ( ~'*' | '*' ~'/' )* '*'? ;
CARACTER_INVALIDO     : . ;

/* ---------- Fragmentos ---------------------------------------------- */
fragment LETRA  : [a-zA-ZáéíóúÁÉÍÓÚñÑüÜ] ;
fragment DIGITO : [0-9] ;
fragment ESCAPE : '\\' [nrt"'\\] ;
// Inside a text any escape lexes: an unknown one, as in "C:\Users", is kept as written.
fragment EN_TEXTO : '\\' ~[\r\n] | ~["\\\r\n] ;
