lexer grammar ZLexer;

/*
 * Zetariano: orientado a objetos, sintaxis de Java. El archivo se llama como la
 * clase que define, regla que se valida en el analisis semantico y no aqui.
 */

/* ---------- 1. Palabras reservadas ---------------------------------- */
// Estructura del programa
PUBLIC  : 'public' ;
PRIVATE : 'private' ;                // se reconoce para reportarlo: el encapsulamiento es del Proyecto 2
CLASS   : 'class' ;
VOID    : 'void' ;
RETURN  : 'return' ;
NEW     : 'new' ;
THIS    : 'this' ;

// Tipos
INT_T     : 'int' ;
DOUBLE_T  : 'double' ;
CHAR_T    : 'char' ;
BOOLEAN_T : 'boolean' ;
STRING_T  : 'String' ;

// Literales con nombre
TRUE  : 'true' ;
FALSE : 'false' ;
NULL  : 'null' ;

// Control de flujo
IF       : 'if' ;
ELSE     : 'else' ;
SWITCH   : 'switch' ;
CASE     : 'case' ;
DEFAULT  : 'default' ;
BREAK    : 'break' ;
CONTINUE : 'continue' ;
FOR      : 'for' ;
WHILE    : 'while' ;
DO       : 'do' ;

// Funciones del sistema: no se declaran
PRINTLN : 'println' ;
PRINT   : 'print' ;
READLN  : 'readln' ;

/* ---------- 2. Operadores de 2 caracteres --------------------------- */
INCREMENTO  : '++' ;
DECREMENTO  : '--' ;
MAS_IGUAL   : '+=' ;
MENOS_IGUAL : '-=' ;
POR_IGUAL   : '*=' ;
IGUALDAD    : '==' ;
DIFERENTE   : '!=' ;
MENOR_IGUAL : '<=' ;
MAYOR_IGUAL : '>=' ;
AND         : '&&' ;
OR          : '||' ;

/* ---------- 3. Operadores de 1 caracter ----------------------------- */
MAS        : '+' ;
MENOS      : '-' ;
POR        : '*' ;
DIVISION   : '/' ;
MODULO     : '%' ;
MENOR      : '<' ;
MAYOR      : '>' ;
ASIGNACION : '=' ;
NEGACION   : '!' ;
INTERROGA  : '?' ;                   // ternario: cond ? a : b

/* ---------- 4. Signos de puntuacion --------------------------------- */
PAR_IZQ    : '(' ;
PAR_DER    : ')' ;
LLAVE_IZQ  : '{' ;
LLAVE_DER  : '}' ;
COR_IZQ    : '[' ;
COR_DER    : ']' ;
PUNTO_COMA : ';' ;
DOS_PUNTOS : ':' ;
COMA       : ',' ;
PUNTO      : '.' ;

/* ---------- 5. Literales -------------------------------------------- */
DECIMAL   : DIGITO+ '.' DIGITO+ ;    // antes que ENTERO
ENTERO    : DIGITO+ ;
TEXTO     : '"' EN_TEXTO* '"' ;
CARACTER  : '\'' ( ESCAPE | ~['\\\r\n] ) '\'' ;

/* ---------- 6. Identificadores -------------------------------------- */
ID : ( LETRA | '_' ) ( LETRA | DIGITO | '_' )* ;

/* ---------- 7. Ignorados por el parser -------------------------------
 * channel(HIDDEN) y no skip: el coloreado lexa el documento completo y
 * necesita que el flujo de tokens cubra tambien los comentarios.
 * -------------------------------------------------------------------- */
COMENTARIO_LINEA  : '//' ~[\r\n]*  -> channel(HIDDEN) ;
COMENTARIO_BLOQUE : '/*' .*? '*/'  -> channel(HIDDEN) ;
ESPACIOS          : [ \t\r\n\f]+   -> channel(HIDDEN) ;

/* ---------- 8. Errores lexicos --------------------------------------- */
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
