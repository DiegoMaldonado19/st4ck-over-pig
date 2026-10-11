// ============================================================
//  objetos/main.pig - los ejemplos del enunciado de Zetariano.
//  Persona.z es la clase del enunciado (mas obtenerEdad, que el
//  enunciado usa sin declarar); Demo.z corre cada fragmento:
//  expresiones, += -= *=, %, ternario, arreglos, matrices, objetos,
//  null, if sin llaves, dangling else, switch con fallthrough,
//  for ( ; ; ), while, do-while, readln y recursividad.
//
//  Entrada: "Zetariano" y "41". Salida esperada:
//    suma=13 resta=7 mult=30 div=3 mod=1
//    a=11 b=2
//    esMayor=true esIgual=false and=true or=true not=true
//    x=12
//    Es mayor de edad
//    1
//    1.75 A 5.5
//    resultado=30 numeros[2]=30
//    Ana 100 7
//    cubo=6 3
//    55 Adulto 2001
//    Es posible comparar si un objeto es nulo
//    ¡Hola! Me llamo Sin nombre y tengo 0 años.
//    ¡Hola! Me llamo Carlos y tengo 20 años.
//    ¡Hola! Me llamo Ana y tengo 45 años.
//    2001
//    true
//    ¡Justo tiene 18 años!
//    Es adulto
//    hola
//    Opción 2 seleccionada
//    Opción no válida
//    Iteración número: 0 ... Iteración número: 4
//    Este bucle nunca termina a menos que use un break...
//    Contador while: 0, 1, 2
//    Intento número: 1, 2
//    factorial(5) = 120
//    contador = 2
//    Nombre: Hola Zetariano, el proximo anio tendras 42
// ============================================================
import Persona.z
import Demo.z

VARIABILES>
esto demo : novus Demo();

MAIOR>
demo.expresiones();
demo.arreglos();
demo.objetos();
demo.control();
demo.ciclos();
>> "factorial(5) = " >> demo.factorial(5);
demo.contar();
demo.contar();
>> "contador = " >> demo.contador;
demo.leer();
FINIS;
