// Utilidades.y
// Estadisticas del laberinto (lenguaje Y?)
// Prueba: estructura con arreglo, si / sino / contrario, retorno de cadena

%estructuras
estructura Resumen:
    cadena jugador
    entero partidas
    entero puntajes[3]

%funciones

definir calificar(entero puntos, entero movimientos) -> cadena:
    si(puntos > 199) entonces
        retornar "S - Leyenda zetariana"
    sino (puntos > 99) entonces
        retornar "A - Explorador experto"
    sino (puntos > 0) entonces
        retornar "B - Aprendiz"
    contrario
        retornar "C - Los cerdos te atraparon"

definir registrarPartida({} Resumen r, cadena jugador, entero puntos):
    si(r.partidas < 3) entonces
        r.puntajes[r.partidas] = puntos
    r.jugador = jugador
    r.partidas++

definir mejorPuntaje({} Resumen r) -> entero:
    entero mejor = 0
    para(entero i = 0; i < r.partidas; i++):
        si(r.puntajes[i] > mejor) entonces
            mejor = r.puntajes[i]
    retornar mejor

definir imprimirResumen({} Resumen r):
    imprimir("===== RESUMEN DE MISION =====")
    imprimir("Ultimo jugador: " + r.jugador)
    imprimir("Partidas jugadas: " + r.partidas)
    para(entero i = 0; i < r.partidas; i++):
        imprimir("Partida " + (i + 1) + ": " + r.puntajes[i] + " puntos")
    imprimir("Mejor puntaje: " + mejorPuntaje(r))
