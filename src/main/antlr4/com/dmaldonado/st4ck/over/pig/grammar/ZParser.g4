parser grammar ZParser;

options { tokenVocab = ZLexer; }

/*
 * Zetariano: una clase por archivo, sintaxis de Java.
 *
 * Reglas ya decididas:
 *   - 'private' y 'this' se aceptan en la gramatica solo para que el semantico
 *     los reporte con un mensaje que sugiere quitarlos: el encapsulamiento es
 *     del Proyecto 2 (aclaracion de la auxiliar, 22/09/2026)
 *   - dangling else: IF ... instruccion ( ELSE instruccion )? es voraz, asi el
 *     'else' queda con el 'if' mas cercano, como en Java
 *   - 'switch' con fallthrough: 'break' es opcional
 *   - 'for ( ; ; )': las tres partes son opcionales
 *   - un arreglo se crea con 'new T[n]' o '{...}' solo al declararlo
 */

/* =====================================================================
 * 1. CLASE
 * ===================================================================== */
programa : declaracionClase EOF ;

declaracionClase : PUBLIC? CLASS ID LLAVE_IZQ miembro* LLAVE_DER ;

// La lectura adelantada distingue las tres: "Pila(" es constructor,
// "tipo nombre(" es metodo y "tipo nombre =" o "tipo nombre;" es atributo.
miembro
    : visibilidad? ID PAR_IZQ listaParametros? PAR_DER bloque                            # constructor
    | visibilidad? ( tipo | VOID ) ID PAR_IZQ listaParametros? PAR_DER bloque           # metodo
    | visibilidad? tipo ID ( ASIGNACION inicializador )? PUNTO_COMA                     # atributo
    ;

visibilidad : PUBLIC | PRIVATE ;

// int[][] -- cada par de corchetes es una dimension
tipo     : tipoBase ( COR_IZQ COR_DER )* ;
tipoBase : INT_T | DOUBLE_T | CHAR_T | BOOLEAN_T | STRING_T | ID ;

listaParametros : parametro ( COMA parametro )* ;
parametro       : tipo ID ;

/* =====================================================================
 * 2. INSTRUCCIONES
 * ===================================================================== */
bloque : LLAVE_IZQ instruccion* LLAVE_DER ;

instruccion
    : bloque                                                                   # instruccionBloque
    | declaracionLocal PUNTO_COMA                                              # instruccionDeclaracion
    | asignacion PUNTO_COMA                                                    # instruccionAsignacion
    | IF PAR_IZQ expresion PAR_DER instruccion ( ELSE instruccion )?           # instruccionSi
    | SWITCH PAR_IZQ expresion PAR_DER LLAVE_IZQ casoSwitch* LLAVE_DER         # instruccionSwitch
    | FOR PAR_IZQ inicioFor? PUNTO_COMA expresion? PUNTO_COMA actualizacionFor? PAR_DER
      instruccion                                                              # instruccionFor
    | WHILE PAR_IZQ expresion PAR_DER instruccion                              # instruccionWhile
    | DO instruccion WHILE PAR_IZQ expresion PAR_DER PUNTO_COMA                # instruccionDoWhile
    | RETURN expresion? PUNTO_COMA                                             # instruccionRetorno
    | BREAK PUNTO_COMA                                                         # instruccionBreak
    | CONTINUE PUNTO_COMA                                                      # instruccionContinue
    | ( PRINT | PRINTLN ) PAR_IZQ expresion? PAR_DER PUNTO_COMA                # instruccionImprimir
    | expresion PUNTO_COMA                                                     # instruccionExpresion
    | PUNTO_COMA                                                               # instruccionVacia
    ;

declaracionLocal : tipo ID ( ASIGNACION inicializador )? ;

// Un arreglo nace al declararse: con su tamano o con sus valores.
inicializador
    : NEW tipoBase ( COR_IZQ expresion COR_DER )+   # inicializadorNuevoArreglo
    | literalArreglo                                # inicializadorLiteral
    | expresion                                     # inicializadorExpresion
    ;

literalArreglo  : LLAVE_IZQ ( elementoArreglo ( COMA elementoArreglo )* )? LLAVE_DER ;
elementoArreglo : literalArreglo | expresion ;

asignacion : destino op=( ASIGNACION | MAS_IGUAL | MENOS_IGUAL | POR_IGUAL ) expresion ;

destino : ( THIS PUNTO )? ID sufijoDestino* ;

sufijoDestino
    : COR_IZQ expresion COR_DER   # accesoIndice
    | PUNTO ID                    # accesoAtributo
    ;

casoSwitch
    : CASE expresion DOS_PUNTOS instruccion*   # caso
    | DEFAULT DOS_PUNTOS instruccion*          # casoDefault
    ;

inicioFor        : declaracionLocal | asignacion ;
actualizacionFor : asignacion | expresion ;

/* =====================================================================
 * 3. EXPRESIONES  -- precedencia de Java, de menor a mayor
 * ===================================================================== */
expresion : expresionTernaria ;

expresionTernaria : expresionOr ( INTERROGA expresion DOS_PUNTOS expresionTernaria )? ;

expresionOr             : expresionAnd ( OR expresionAnd )* ;
expresionAnd            : expresionIgualdad ( AND expresionIgualdad )* ;
expresionIgualdad       : expresionRelacional ( ( IGUALDAD | DIFERENTE ) expresionRelacional )* ;
expresionRelacional     : expresionAditiva ( ( MENOR | MAYOR | MENOR_IGUAL | MAYOR_IGUAL ) expresionAditiva )* ;
expresionAditiva        : expresionMultiplicativa ( ( MAS | MENOS ) expresionMultiplicativa )* ;
expresionMultiplicativa : expresionUnaria ( ( POR | DIVISION | MODULO ) expresionUnaria )* ;

expresionUnaria
    : NEGACION expresionUnaria                       # unariaNegacion
    | MENOS expresionUnaria                          # unariaNegativo
    | ( INCREMENTO | DECREMENTO ) expresionUnaria    # unariaPrefija
    | expresionSufijo                                # unariaSufijoDelegado
    ;

expresionSufijo : expresionPrimaria sufijoExpresion* ;

sufijoExpresion
    : COR_IZQ expresion COR_DER                    # sufijoIndice
    | PUNTO ID PAR_IZQ listaExpresiones? PAR_DER   # sufijoMetodo
    | PUNTO ID                                     # sufijoAtributo
    | INCREMENTO                                   # sufijoIncremento
    | DECREMENTO                                   # sufijoDecremento
    ;

expresionPrimaria
    : ENTERO                                      # primariaEntero
    | DECIMAL                                     # primariaDecimal
    | TEXTO                                       # primariaTexto
    | CARACTER                                    # primariaCaracter
    | TRUE                                        # primariaVerdadero
    | FALSE                                       # primariaFalso
    | NULL                                        # primariaNulo
    | THIS                                        # primariaThis
    | READLN PAR_IZQ PAR_DER                      # primariaLeer
    | NEW ID PAR_IZQ listaExpresiones? PAR_DER    # primariaNuevo
    | ID PAR_IZQ listaExpresiones? PAR_DER        # primariaLlamada
    | ID                                          # primariaIdentificador
    | PAR_IZQ expresion PAR_DER                   # primariaAgrupacion
    ;

listaExpresiones : expresion ( COMA expresion )* ;
