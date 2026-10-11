// ============================================================
//  resistencia.pig - prueba de regresion general del compilador.
//  Ejercita funciones, recursividad, arreglos, estructuras, los
//  tres ciclos, interrumpe/perge, elegir y un import de Y?.
//
//  Salida esperada, con "33" en la entrada:
//    Poder calculado: 30
//    Quien anda ahi?
//    Salve, humano
//    Ana tiene 34
//    Ana ahora tiene 35
//    Atacando con fuerza: 30
//    Iteracion: 1
//    Iteracion: 2
//    Iteracion: 4
//    Iteracion: 5
//    2
//    2
//    5! = 120
//    fib(10) = 55
//    Rango: teniente
//    Gravedad: 9.81, inicial: a, bloqueado: falsus
//    Hola y Adios
//    Ingresa tu edad
//    Edad: 33
// ============================================================
import resistencia.y

VARIABILES>

esto edad : numerus 20;
esto nombre : textum "Resistencia";
esto gravedad : decimalis 9.81;
esto inicial : littera 'a';
esto activo : bool verum;
esto bloqueado : falsus;

series numeros[2] : numerus {1, 1};
series nombres[2] : textum {"Hola", "Adios"};

esto lider : Persona { "Ana", 34 };

MUNERA>

actio atacarCerdos(esto fuerza : numerus) {
    >> "Atacando con fuerza: " >> fuerza;
} finis;

ratio numerus calcularPoder(esto fuerza : numerus, esto nivel : numerus) {
    VARIABILES[
        esto total : numerus 0;
    ]
    per (esto i : numerus 0; i < nivel; i++) {
        total = total + fuerza;
    } finis;
    reddere total;
} finis;

ratio textum saludar(esto quien : textum) {
    si (quien == "cerdo") {
        reddere "Oink";
    } aliter (quien == "humano") {
        reddere "Salve, " + quien;
    } aliter {
        reddere "Quien anda ahi?";
    } finis;
} finis;

MAIOR>

esto poder : numerus 0;
esto contador : numerus 0;

poder = calcularPoder(10, 3);
>> "Poder calculado: " >> poder;
>> saludar(nombre);
>> saludar("humano");
>> lider.nombre >> " tiene " >> lider.edad;
cumplirAnios(lider);
>> lider.nombre + " ahora tiene " + lider.edad;

si (poder > 10 && activo) {
    atacarCerdos(poder);
} aliter {
    >> "Poder insuficiente";
} finis;

dum (contador < 5) {
    contador = contador + 1;
    si (contador == 3) {
        perge;
    } finis;
    >> "Iteracion: " >> contador;
} finis;

facere {
    contador = contador - 1;
} dum (contador > 0);

duplicar(numeros, 2);
per (esto i : numerus 0; i < 2; i++) {
    >> numeros[i];
    si (i == 1) {
        interrumpe;
    } finis;
} finis;

>> "5! = " >> factorial(5);
>> "fib(10) = " >> fibonacci(10);
>> "Rango: " >> rango(2);
>> "Gravedad: " >> gravedad >> ", inicial: " >> inicial >> ", bloqueado: " >> bloqueado;
>> nombres[0] + " y " + nombres[1];

>> "Ingresa tu edad";
edad <<
>> "Edad: " >> edad;

FINIS;
