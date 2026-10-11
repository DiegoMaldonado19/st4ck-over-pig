parser grammar YParser;

options { tokenVocab = YLexer; }

/*
 * Y? delimita los bloques por sangria: YLexer convierte la sangria en INDENT /
 * DEDENT y oculta los saltos de linea. Por eso ninguna regla ve un fin de
 * linea: una instruccion termina donde ya no puede continuar, y el punto y
 * coma es opcional.
 *
 * Reglas ya decididas y que no hay que volver a discutir:
 *   - %estructuras es opcional, %funciones es obligatoria
 *   - las estructuras SOLO se declaran con 'estructura Nombre:' y campos
 *     indentados (aclaracion de la auxiliar, 5/09/2026); la forma tipo C se
 *     reconoce solo para reportarla con un mensaje propio
 *   - los arreglos se pasan como '[] entero arr' y las estructuras como
 *     '{} MiEstructura s', ambos por referencia
 *
 * Limites conocidos, que ningun ejemplo del enunciado toca: una '}' sangrada
 * al nivel de los 'caso', y una linea que empieza con '(' justo despues de una
 * expresion (se leeria como llamada). Tampoco hay ++ prefijo: pegado a la linea
 * anterior cambiaria su significado. Un literal { } si puede partirse en
 * varias lineas: YLexer no cuenta la sangria dentro de el.
 */

@parser::members {
    /** retornar solo lleva valor si este empieza en su misma linea: el salto no es un token. */
    private boolean sameLine()
    {
        return _input.LT(1).getLine() == _input.LT(-1).getLine();
    }

    /**
     * Un + o - al inicio de una linea continua la expresion de la anterior solo
     * dentro de un ( [ { abierto, como en Python: afuera, "-b" es otra instruccion.
     */
    private boolean continues()
    {
        if (sameLine())
        {
            return true;
        }

        ParserRuleContext statement = _ctx;

        while (!(statement instanceof InstruccionContext) && statement.getParent() != null)
        {
            statement = statement.getParent();
        }

        int open = 0;

        for (int i = statement.getStart().getTokenIndex(); i < _input.LT(1).getTokenIndex(); i++)
        {
            int type = _input.get(i).getType();

            open += type == PAR_IZQ || type == COR_IZQ || type == LLAVE_IZQ ? 1
                  : type == PAR_DER || type == COR_DER || type == LLAVE_DER ? -1 : 0;
        }
        return open > 0;
    }
}

/* =====================================================================
 * 1. ESTRUCTURA DEL ARCHIVO
 * ===================================================================== */
programa : seccionEstructuras? seccionFunciones EOF ;

seccionEstructuras : SECCION_ESTRUCTURAS declaracionEstructura* ;
seccionFunciones   : SECCION_FUNCIONES declaracionFuncion* ;

/* =====================================================================
 * 2. ESTRUCTURAS
 * ===================================================================== */
declaracionEstructura
    : ESTRUCTURA ID DOS_PUNTOS INDENT campoEstructura+ DEDENT               # estructuraIndentada
    | kw=ESTRUCTURA LLAVE_IZQ INDENT? campoEstructura* DEDENT? LLAVE_DER ID PUNTO_COMA?
      { notifyErrorListeners($kw, "Las estructuras en Y? se declaran con 'estructura Nombre:' y campos indentados.", null); }
                                                                             # estructuraEstiloC
    ;

// entero miArray[ 10 ]  -- dentro de una estructura el tamano es constante
campoEstructura : tipo ID ( COR_IZQ ENTERO COR_DER )? PUNTO_COMA? ;

tipo : ENTERO_T | CADENA_T | FLOTANTE_T | CARACTER_T | BOOL_T | ID ;

/* =====================================================================
 * 3. FUNCIONES
 * ===================================================================== */
declaracionFuncion
    : DEFINIR ID PAR_IZQ listaParametros? PAR_DER ( FLECHA tipo )? DOS_PUNTOS bloque
    ;

listaParametros : parametro ( COMA parametro )* ;

// [] entero miArray  -> arreglo por referencia;  {} Persona p -> estructura por referencia
parametro : ( COR_IZQ COR_DER | LLAVE_IZQ LLAVE_DER )? tipo ID ;

bloque : INDENT instruccion+ DEDENT ;

/* =====================================================================
 * 4. INSTRUCCIONES
 * La declaracion va antes que la expresion suelta: "Persona p" tambien
 * podria leerse como dos expresiones.
 * ===================================================================== */
instruccion
    : ( declaracionEstructura
      | declaracionVariable
      | asignacion
      | instruccionIncremento
      | instruccionSi
      | instruccionElegir
      | instruccionPara
      | instruccionMientras
      | instruccionHacer
      | instruccionRetorno
      | instruccionRomper
      | instruccionContinuar
      | instruccionImprimir
      | instruccionExpresion
      | bloqueInesperado
      ) PUNTO_COMA?
    ;

// Una linea sangrada que nada abre: se reporta una vez y el bloque se lee igual,
// en vez de dejar que ANTLR se tope con su INDENT y luego con su DEDENT.
bloqueInesperado
    : kw=INDENT instruccion+ DEDENT
      { notifyErrorListeners($kw, "Sangria inesperada: la linea anterior no abre ningun bloque.", null); }
    ;

declaracionVariable
    : tipo ID ( COR_IZQ expresion COR_DER )+ ( ASIGNACION literalCompuesto )?   # declaracionArreglo
    | tipo ID ( ASIGNACION expresion )?                                     # declaracionSimple
    ;

asignacion : destino ASIGNACION expresion ;

destino : ID sufijoDestino* ;

sufijoDestino
    : COR_IZQ expresion COR_DER   # accesoIndice
    | PUNTO ID                    # accesoAtributo
    ;

instruccionIncremento : destino ( INCREMENTO | DECREMENTO ) ;

instruccionSi
    : SI PAR_IZQ expresion PAR_DER ENTONCES bloque
      sinoCondicional*
      ( CONTRARIO bloque )?
    ;

sinoCondicional : SINO PAR_IZQ expresion PAR_DER ENTONCES bloque ;

// Un caso sin cuerpo cae al siguiente, como en C.
instruccionElegir
    : ELEGIR PAR_IZQ expresion PAR_DER LLAVE_IZQ INDENT? casoElegir* casoSiempre? DEDENT? LLAVE_DER
    ;

casoElegir  : CASO expresion DOS_PUNTOS bloque? ;
casoSiempre : SIEMPRE DOS_PUNTOS bloque? ;

// Reglas propias para la cabecera: con 'instruccion' su ';' opcional se comeria el separador.
instruccionPara
    : PARA PAR_IZQ inicializacionPara PUNTO_COMA expresion PUNTO_COMA actualizacionPara PAR_DER
      DOS_PUNTOS bloque
    ;

inicializacionPara
    : tipo ID ASIGNACION expresion   # inicioDeclaracion
    | destino ASIGNACION expresion   # inicioAsignacion
    ;

actualizacionPara
    : destino ( INCREMENTO | DECREMENTO )   # actualizacionUnaria
    | destino ASIGNACION expresion          # actualizacionAsignacion
    ;

instruccionMientras : MIENTRAS PAR_IZQ expresion PAR_DER HACER bloque ;

instruccionHacer : HACER DOS_PUNTOS bloque MIENTRAS PAR_IZQ expresion PAR_DER ;

instruccionRetorno   : RETORNAR ( {sameLine()}? expresion )? ;
instruccionRomper    : ROMPER ;
instruccionContinuar : CONTINUAR ;
instruccionImprimir  : IMPRIMIR PAR_IZQ listaExpresiones? PAR_DER ;

// Cualquier expresion: el semantico decide cual puede ir sola.
instruccionExpresion : expresion ;

/* =====================================================================
 * 5. EXPRESIONES  -- misma cadena de precedencia que PigLatin
 * ===================================================================== */
expresion : expresionOr ;

expresionOr             : expresionAnd ( OR expresionAnd )* ;
expresionAnd            : expresionIgualdad ( AND expresionIgualdad )* ;
expresionIgualdad       : expresionRelacional ( ( IGUALDAD | DIFERENTE ) expresionRelacional )* ;
expresionRelacional     : expresionAditiva ( ( MENOR | MAYOR | MENOR_IGUAL | MAYOR_IGUAL ) expresionAditiva )* ;
// Sin fin de instruccion, "-b" en su propia linea se pegaria a la anterior: ver continues().
expresionAditiva        : expresionMultiplicativa ( {continues()}? ( MAS | MENOS ) expresionMultiplicativa )* ;
expresionMultiplicativa : expresionUnaria ( ( POR | DIVISION ) expresionUnaria )* ;

expresionUnaria
    : NEGACION expresionUnaria   # unariaNegacion
    | MENOS expresionUnaria      # unariaNegativo
    | expresionSufijo            # unariaSufijoDelegado
    ;

expresionSufijo : expresionPrimaria sufijoExpresion* ;

sufijoExpresion
    : COR_IZQ expresion COR_DER   # sufijoIndice
    | PUNTO ID                    # sufijoAtributo
    ;

expresionPrimaria
    : ENTERO                                  # primariaEntero
    | DECIMAL                                 # primariaDecimal
    | TEXTO                                   # primariaTexto
    | CARACTER                                # primariaCaracter
    | VERDADERO                               # primariaVerdadero
    | FALSO                                   # primariaFalso
    | LEER PAR_IZQ PAR_DER                    # primariaLeer
    | ID PAR_IZQ listaExpresiones? PAR_DER    # primariaLlamada
    | ID                                      # primariaIdentificador
    | PAR_IZQ expresion PAR_DER               # primariaAgrupacion
    | literalCompuesto                        # primariaLiteral
    ;

// Y? solo tiene la forma posicional: Punto p1 = { 10, 20, 85.5 }
literalCompuesto : LLAVE_IZQ listaExpresiones? LLAVE_DER ;

listaExpresiones : expresion ( COMA expresion )* ;
