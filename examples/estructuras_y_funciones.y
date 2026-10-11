// Ejemplos del enunciado. Sirve para verificar INDENT/DEDENT.
%estructuras
estructura MiEstructura:
    cadena nombre

estructura Persona:
    entero edad
    cadena nombre
    flotante promedio   // numero con decimales
    caracter letra
    /* la expresion para definir arreglos
       obligatoriamente debe ser constante */
    entero miArray[ 10 ]
    MiEstructura miEstructura

%funciones
definir funcionSinRetorno(entero miEntero):
    miEntero = 90 * 10

definir funcionConRetorno(entero miEntero) -> entero :
    miEntero = 10 + 10
    si(miEntero > 18) entonces
        imprimir("es mayor de edad")
        si(miEntero == 20) entonces
            imprimir("anidado")
    retornar 160

definir conArreglo([] entero miArray, {} Persona quien):
    imprimir(quien.nombre)
