##
    Proyecto de prueba 2: Laberinto de tesoros (matriz con objetos)
    Usa clases .z (Jugador, Tablero, Juego) y funciones .y (Utilidades)
##
import clases.Jugador.z
import clases.Tablero.z
import clases.Juego.z
import lib.Utilidades.y

VARIABILES>
esto nombre : textum "Cadete";
esto direccion : numerus 99;
esto turno : numerus 0;
esto maxTurnos : numerus 60;
esto juego : novus Juego("Temporal");
series historial[3] : numerus {0, 0, 0};
esto stats : Resumen {"Nadie", 0, historial};

MAIOR>
>> "=== LABERINTO DE TESOROS ===" ;
>> "Leyenda: P jugador, # pared, $ tesoro, X trampa, * gema" ;

per (esto ronda : numerus 1; ronda < 3; ronda++) {
    >> "\n" ;
    >> "=== RONDA \n ronda \n ===" ;
    >> "Ingresa tu nombre:" ;
    nombre <<

    juego = novus Juego(nombre);
    >> "Celdas accesibles desde el inicio: " >> juego.tablero.contarAccesibles(1, 1) ;
    >> "Tesoros en el tablero: " >> juego.tablero.contarTipo(2) ;
    >> "Camino libre a la derecha: " >> juego.tablero.distanciaAPared(juego.jugador.fila, juego.jugador.columna, 0, 1) ;

    direccion = 99;
    turno = 0;
    dum (juego.terminado == falsus) {
        juego.tablero.imprimir(juego.jugador.fila, juego.jugador.columna);
        juego.jugador.mostrarEstado();
        >> "Mover (1=arriba 2=abajo 3=izq 4=der, 0=salir):" ;
        direccion <<
        si (direccion == 0) {
            interrumpe;
        } finis;
        juego.mover(direccion);
        >> juego.mensaje ;
        turno = turno + 1;
        si (turno > maxTurnos) {
            >> "Limite de turnos alcanzado" ;
            interrumpe;
        } finis;
    } finis;

    >> "--- Fin de la ronda ---" ;
    juego.jugador.mostrarEstado();
    >> "Veredicto: " >> juego.obtenerVeredicto() ;
    >> "Calificacion: " >> calificar(juego.jugador.puntos, juego.jugador.movimientos) ;
    registrarPartida(stats, nombre, juego.jugador.puntos);
}

imprimirResumen(stats);
FINIS;
