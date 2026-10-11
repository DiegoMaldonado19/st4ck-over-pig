// Estructuras y funciones que importa resistencia.pig.
%estructuras
estructura Persona:
    cadena nombre
    entero edad

%funciones
// Recursividad: cada llamada tiene su propio marco en el stack.
definir factorial(entero n) -> entero :
    si(n <= 1) entonces
        retornar 1
    retornar n * factorial(n - 1)

definir fibonacci(entero n) -> entero :
    si(n < 2) entonces
        retornar n
    retornar fibonacci(n - 1) + fibonacci(n - 2)

// Los arreglos llegan por referencia: el cambio se ve en el llamador.
definir duplicar([] entero valores, entero cantidad):
    para(entero i = 0; i < cantidad; i++):
        valores[i] = valores[i] * 2

// Las estructuras tambien: {} marca el paso por referencia.
definir cumplirAnios({} Persona quien):
    quien.edad = quien.edad + 1

definir rango(entero nivel) -> cadena :
    elegir(nivel) {
        caso 1 :
            retornar "cadete"
        caso 2 :
            retornar "teniente"
        siempre:
            retornar "comandante"
    }
