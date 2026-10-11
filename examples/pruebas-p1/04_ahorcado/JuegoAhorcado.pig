##
    Proyecto de prueba 3: Ahorcado
    Usa clases .z (Palabra, Diccionario, Ahorcado) y funciones .y (Estadisticas)
##
import clases.Palabra.z
import clases.Diccionario.z
import clases.Ahorcado.z
import lib.Estadisticas.y

VARIABILES>
esto nombre : textum "Jugador";
esto indice : numerus 0;
esto seguir : numerus 1;
esto letra : littera 'a';
esto resultado : numerus 0;
esto puntaje : numerus 0;
esto total : numerus 0;
esto gano : bool falsus;
esto diccionario : novus Diccionario();
esto juego : novus Ahorcado(diccionario.obtener(0), 6);
series puntajesIniciales[5] : numerus {0, 0, 0, 0, 0};
esto marcador : Marcador {0, 0, 0, 0, 0, puntajesIniciales};

MAIOR>
>> "==============================" ;
>> "     AHORCADO ZETARIANO       " ;
>> "==============================" ;
>> "Ingresa tu nombre:" ;
nombre <<

facere {
    >> "Elige una palabra (0 a 4):" ;
    indice <<
    juego = novus Ahorcado(diccionario.obtener(indice), 6);
    >> "Pista: " >> juego.palabra.pista ;

    dum (juego.terminoJuego() == falsus) {
        juego.dibujar();
        juego.mostrarEstado();
        >> "Ingresa una letra minuscula:" ;
        letra <<

        si (validarLetra(letra) == falsus) {
            >> "Entrada invalida, solo letras de la a a la z" ;
            perge;
        } finis;

        resultado = juego.intentar(letra);
        si (resultado == 0) {
            >> "Ya usaste la letra " >> letra ;
        } aliter (resultado == 1) {
            >> "Bien! La letra " >> letra >> " esta en la palabra" ;
        } aliter {
            >> "Fallaste! La letra " >> letra >> " no esta" ;
        } finis;
    } finis;

    // Se determina el resultado ANTES de revelar la palabra completa
    puntaje = 0;
    si (juego.gano()) {
        gano = verum;
        puntaje = calcularPuntaje(juego.palabra.longitud, juego.errores, 6);
        >> "GANASTE! La palabra era:" ;
    } aliter {
        gano = falsus;
        >> "PERDISTE. La palabra era:" ;
    } finis;

    juego.dibujar();
    juego.palabra.revelarTodo();
    juego.palabra.imprimirProgreso();
    >> "Puntaje de la partida: " >> puntaje ;

    registrarResultado(marcador, gano, puntaje);
    mostrarMarcador(marcador);

    >> "Jugar otra vez? (1 = si, 0 = no)" ;
    seguir <<
} dum (seguir == 1);

total = sumarPuntajes(marcador);
>> "Puntaje total: " >> total ;
>> "Titulo obtenido: " >> titulo(total) ;
>> "Gracias por jugar, " >> nombre ;
FINIS;
