lexer grammar PigLexer;

/* ---------- 1. Marcadores de seccion --------------------------------- */
VARIABILES       : 'VARIABILES>' ;
VARIABILES_LOCAL : 'VARIABILES[' ;   // seccion de variables dentro de una funcion
MUNERA           : 'MUNERA>' ;
MAIOR            : 'MAIOR>' ;
FIN_PROGRAMA     : 'FINIS' ;         // mayusculas: cierra el programa

/* ---------- 2. Palabras reservadas ---------------------------------- */
// Importacion de otros lenguajes: import carpeta.Objeto1.z
IMPORT     : 'import' ;

// Declaracion
ESTO       : 'esto' ;
SERIES     : 'series' ;
STRUCTURA  : 'structura' ;
FINIS      : 'finis' ;               // minusculas: cierra un bloque

// Tipos
NUMERUS    : 'numerus' ;
DECIMALIS  : 'decimalis' ;
TEXTUM     : 'textum' ;
LITTERA    : 'littera' ;
BOOL       : 'bool' ;
VERUM      : 'verum' ;
FALSUS     : 'falsus' ;

// Control de flujo
SI         : 'si' ;
ALITER     : 'aliter' ;
DUM        : 'dum' ;
FACERE     : 'facere' ;
PER        : 'per' ;
PERGE      : 'perge' ;
INTERRUMPE : 'interrumpe' ;

// Funciones
ACTIO      : 'actio' ;
RATIO      : 'ratio' ;
REDDERE    : 'reddere' ;

// Instanciacion de un objeto definido en un archivo .z
NOVUS      : 'novus' ;

// Negacion logica
NON        : 'non' ;

/* ---------- 3. Operadores de 2 caracteres ------ */
ENTRADA     : '<<' ;                 // lectura de consola
SALIDA      : '>>' ;                 // impresion en consola
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

/* ---------- 5. Signos de puntuacion --------------------------------- */
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

/* ---------- 6. Literales -------------------------------------------- */
DECIMAL   : DIGITO+ '.' DIGITO+ ;    // antes que ENTERO
ENTERO    : DIGITO+ ;
TEXTO     : '"' EN_TEXTO* '"' ;
CARACTER  : '\'' ( ESCAPE | ~['\\\r\n] ) '\'' ;

/* ---------- 7. Identificadores -------------------------------------- */
ID : ( LETRA | '_' ) ( LETRA | DIGITO | '_' )* ;

/* ---------- 8. Ignorados por el parser -------------------------------
 * channel(HIDDEN) y no skip: el parser los ignora igual, pero siguen
 * disponibles para que el coloreado pinte los comentarios.
 * -------------------------------------------------------------------- */
COMENTARIO_LINEA  : '//' ~[\r\n]* -> channel(HIDDEN) ;
COMENTARIO_BLOQUE : '/*' .*? '*/' -> channel(HIDDEN) ;

// El enunciado delimita los bloques de comentario con ## ... ##, y lo usa
// para encabezar la seccion de importaciones. Sin esta regla cada '#' cae
// en CARACTER_INVALIDO y el archivo ni siquiera llega al parser.
COMENTARIO_HASH   : '##' .*? '##' -> channel(HIDDEN) ;
ESPACIOS          : [ \t\r\n\f]+  -> channel(HIDDEN) ;

/* ---------- 9. Errores lexicos -------- */
TEXTO_SIN_CERRAR      : '"'  EN_TEXTO* ;
CARACTER_LARGO        : '\'' ( ESCAPE | ~['\\\r\n] ) ( ESCAPE | ~['\\\r\n] )+ '\'' ;
CARACTER_MAL_FORMADO  : '\'' ( '\\' ~[nrt"'\\\r\n] )? '\'' ;
CARACTER_SIN_CERRAR   : '\'' ( ESCAPE | ~['\\\r\n] )? ;
COMENTARIO_SIN_CERRAR : '/*' ( ~'*' | '*' ~'/' )* '*'? ;
COMENTARIO_HASH_SIN_CERRAR : '##' ( ~'#' | '#' ~'#' )* '#'? ;
CARACTER_INVALIDO     : . ;

/* ---------- Fragmentos ---------------------------------------------- */
fragment LETRA  : [a-zA-ZáéíóúÁÉÍÓÚñÑüÜ] ;
fragment DIGITO : [0-9] ;
fragment ESCAPE : '\\' [nrt"'\\] ;
// Inside a text any escape lexes: an unknown one, as in "C:\Users", is kept as written.
fragment EN_TEXTO : '\\' ~[\r\n] | ~["\\\r\n] ;
