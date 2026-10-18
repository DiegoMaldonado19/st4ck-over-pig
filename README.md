# st4ck-over-pig

Compilador de PigLatin, Y? y Zetariano a RISC-V para el simulador RARS.

## Ejemplos

Cada programa de `examples/` se compila desde su `.pig`. Junto a él están lo que se escribe en la consola,
`<programa>.entrada.txt` (solo si lee algo), y lo que imprime en RARS, `<programa>.salida.txt`. Los
`errores.pig` no compilan a propósito: su cabecera lista cada error que dan, con su archivo, línea y columna.

| Ejemplo | Lenguajes | Qué demuestra |
|---|---|---|
| `enunciado.pig` y `lib/Funciones.y` | PigLatin, Y? | Los fragmentos del enunciado de los dos lenguajes: declaraciones, arreglos, estructuras, funciones y ciclos. Lee de la consola. |
| `estructuras_y_funciones.y` | Y? | Las estructuras y funciones del enunciado de Y?, analizadas solas: los bloques por sangría. |
| `resistencia.pig` y `resistencia.y` | PigLatin, Y? | Funciones, recursividad, arreglos, estructuras, los tres ciclos, `interrumpe`/`perge` y `elegir`. Lee un número. |
| `matrices/` | PigLatin, Y? | Matrices con sus valores fila por fila, aplanadas en el heap. |
| `objetos/` | PigLatin, Zetariano | Los fragmentos del enunciado de Zetariano: expresiones, arreglos, matrices, objetos, `null`, `switch` con *fallthrough*, los ciclos, `readln` y recursividad. Lee un texto y un número. |
| `pila/` | los tres | La prueba de la auxiliar: una pila con `Nodo.z` y `Pila.z`, manejada desde un menú. |
| `pruebas-p1/00_hello_world/` | los tres | Hola mundo desde los tres lenguajes, y `Minimo.pig`. |
| `pruebas-p1/01_calculadora/` | PigLatin, Y? | Arreglos globales por referencia, un arreglo dentro de una estructura y recursividad. |
| `pruebas-p1/02_lista_doble/` | los tres | Una lista doblemente enlazada: referencias encadenadas, `null`, sobrecarga y `elegir :`. |
| `pruebas-p1/03_laberinto/` | los tres | Matrices como atributo con dimensiones variables, recursividad (*flood fill*) y `switch` con *fallthrough*. |
| `pruebas-p1/04_ahorcado/` | los tres | Arreglos `char[]` y `boolean[]` creados con `new` en el constructor, `elegir :` y el ternario. |
| `p2/operacion/` | PigLatin, Zetariano | El ejemplo de clase (`docs/Ejemplo.txt`): el método recursivo `sumar`. |
| `p2/encapsulamiento/` | PigLatin, Zetariano | `public`, `protected`, default y `private` en atributos, métodos y constructores, usados desde la misma clase, una clase hija, otra clase y el `.pig`. `errores.pig`: los 18 usos prohibidos. |
| `p2/herencia/` | PigLatin, Zetariano | `Animal` y `Perro` del enunciado: lo heredado, `super(...)` y el `super()` implícito, el orden de un constructor, tres niveles de herencia y `super.describir()`. `errores.pig`: un ciclo, un padre que no existe, `super(...)` fuera de lugar o faltante, un `private` del padre y un `Animal` en una variable `Perro`. |
| `p2/polimorfismo/` | PigLatin, Zetariano | `Impresora`, `ImpresoraPDF` e `ImpresoraExcel` del enunciado en un arreglo de `Impresora`: cada `imprimir()` responde la versión de la clase del objeto, también desde `this.imprimir()`. `errores.pig`: las cuatro reglas de `@Override`. |
| `p2/piglatin-objetos/` | PigLatin, Zetariano | `novus Profesor(..) : Persona` y `null : Persona` del enunciado: los métodos se buscan en el tipo explícito y responden por la clase del objeto. `errores.pig`: un tipo explícito que no es la clase del `novus` ni una clase padre suya, y `null` con un tipo primitivo. |
| `p2/integrador/` | los tres | La misión de la resistencia: herencia en tres niveles, un arreglo de objetos, una matriz, recursividad en Y? y en Zetariano, y un arreglo que llega a Y? por referencia. Lee un nombre y un número. |

Para correr uno en RARS por consola, con el `.asm` que descarga la interfaz:

```
java -Dstdout.encoding=UTF-8 -jar rars1_6.jar nc me sm se1 ae2 main.asm < main.entrada.txt
```
