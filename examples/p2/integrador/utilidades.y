%funciones

// Recursividad doble: las formas de subir n escalones de uno en uno o de dos
// en dos.
definir fibonacci(entero n) -> entero:
    si(n < 2) entonces
        retornar 1
    retornar fibonacci(n - 1) + fibonacci(n - 2)

// El arreglo llega por referencia desde PigLatin.
definir sumarArreglo([] entero datos, entero cantidad) -> entero:
    entero total = 0
    para(entero i = 0; i < cantidad; i++):
        total = total + datos[i]
    retornar total
