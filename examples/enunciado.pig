// Ejemplos literales del enunciado para PigLatin, con lo minimo que hace falta
// para que compilen (el enunciado viene de un PDF y trae erratas):
//   - comillas curvas “Hola” -> "Hola"
//   - "verum || 1 = 1"                    -> "verum || 1 == 1"
//   - ">> "Tu poder es: >> " calcular..." -> le faltaba un '>>'
//   - "esto inicial : littera 'a'"         -> le faltaba el ';'
//   - mi_entero, mi_booleano, mi_cadena y mi_float se declaraban dos veces:
//     queda una declaracion; el segundo mis_enteros pasa a mis_enteros3
// En lib/Funciones.y:
//   - "36." -> "36.5" y "bool = verdadero" -> "bool activo = verdadero"
//   - "estructura Puntos:" -> "Punto", el nombre que usan sus variables
//   - "alumno1" -> "alumno" y "p3 = p" -> "p3 = p1", las variables declaradas
//   - las '}' sueltas del ejemplo de ciclos se quitan
// Los objetos (import de .z, novus, metodos) llegan con Zetariano en la Fase 4.
##
Importaciones de los otros lenguajes
##
import lib.Funciones.y
##
Seccion opcional de variables, puede no existir
En esta seccion solo se definen variables, arreglos
o estructuras globales
##
VARIABILES>
esto edad : numerus 20;
esto cifrado : falsus;
esto comandante : textum "Estudiante X";
esto fuerza : numerus 10;
esto poder : numerus 0;

// Declaracion de variables
esto mi_entero : numerus 10 ;
esto total : numerus fuerza * 2 ;
esto nombre : textum "Somos la resistencia";
esto gravedad : decimalis 9.81;
esto inicial : littera 'a';
esto mi_booleano : falsus;
esto mi_cadena : textum "Nothing here";
esto mi_float : decimalis 12.4;

// Arreglos
series mis_enteros[ 2 ] : numerus {1, 1};
series mis_enteros_[ 2 ] : numerus;
series nombres[ 2 ] : textum {"Hola", "Adios"};
series nombres_[ 2 ] : textum;

// Estructuras, declaradas en lib/Funciones.y
esto mi_direccion : Direccion {"Calle Real", 42 };
esto ciudadano : Persona {"Valeria", 25 , {"Avenida Central", 500 }};
series resistencia[3] : Persona;
series mis_enteros3[3] : numerus { 1 , 1, 1 };
esto alumno_ejemplo : Estudiante {"Carlos", mis_enteros3 };
##
Seccion de funcion principal
Esta seccion es obligatoria
##
MAIOR>
>> "Hola comandante!" ;
>> "Ingresa tu nombre por favor" ;
comandante <<
>> "Bienvenido" >> comandante ;
>> "Ingresa tu edad" ;
edad <<
si (edad >= 18) {
    cifrado = verum;
    fuerza = 12;
} finis ;
##
la siguiente funcion tuvo que estar definida en un archivo .y
##
>> "Tu poder es: " >> calcularPoder(fuerza);
>> "La puerta esta cifrada?" >> cifrado ;

// Asignaciones
mi_entero = 23;
mi_booleano = verum || 1 == 1;
mi_cadena = "Abajo el imperio porcino " + 100 + " .. no tenemos miedo";
mi_float = 17 / 2 ;
nombres[0] = "Capitan Esparragos";
nombres[1] = nombres[0] + " clon";
>> mi_cadena;
>> nombres[1] >> " / " >> mi_float;
>> ciudadano.nombre >> " vive en " >> ciudadano.domicilio.calle;

// Condicionales
esto x : numerus 11;
esto y : numerus 2;
si (x > 10 && y < 5 ) {
    >> "se cumple la condicion";
} finis;
si (x > 10 && y < 5 ) {
    >> "se cumple la condicion";
} aliter {
    >> "no se cumple la condicion";
} finis;
si (x > 10 && y < 5) {
    >> "se cumple la primera";
} aliter (x > 10) {
    >> "se cumple la segunda";
} aliter {
    >> "no se cumple ninguna";
} finis;

// Ciclos
dum (x < 100 ) {
    x = x + 1 ;
} finis;
facere {
    x = x - 1;
} dum (x > 10 );
per (esto i : numerus 0; i < 10; i++) {
    si (i == 3) {
        perge;
    } finis;
    si (i == 5) {
        interrumpe;
    } finis;
    >> i;
}

// Leer texto en consola sin guardarlo
<<
FINIS;
