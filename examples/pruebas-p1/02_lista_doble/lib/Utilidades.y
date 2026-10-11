// Utilidades.y
// Unico archivo .y del proyecto: contador de operaciones sobre la lista

%estructuras
estructura Registro:
    entero operaciones
    entero inserciones
    entero eliminaciones
    entero busquedasExitosas

%funciones

// tipo: 1 = insercion, 2 = eliminacion, 3 = busqueda exitosa
definir registrar({} Registro r, entero tipo):
    r.operaciones++
    elegir(tipo) :
        caso 1:
            r.inserciones++
            romper
        caso 2:
            r.eliminaciones++
            romper
        caso 3:
            r.busquedasExitosas++
            romper
        siempre:
            imprimir("Tipo de operacion desconocido")
            romper

definir esPar(entero n) -> bool:
    retornar n - (n / 2) * 2 == 0

definir paridad(entero n) -> cadena:
    si(esPar(n) == verdadero) entonces
        retornar "par"
    contrario
        retornar "impar"

definir imprimirRegistro({} Registro r):
    imprimir("===== REGISTRO DE OPERACIONES =====")
    imprimir("Operaciones totales: " + r.operaciones)
    imprimir("Inserciones: " + r.inserciones)
    imprimir("Eliminaciones: " + r.eliminaciones)
    imprimir("Busquedas exitosas: " + r.busquedasExitosas)
