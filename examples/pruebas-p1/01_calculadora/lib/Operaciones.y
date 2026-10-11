%estructuras
estructura Historial:
    entero cantidad
    flotante valores[10]

%funciones

/* ---------- Operaciones basicas ---------- */

definir sumar(flotante a, flotante b) -> flotante:
    retornar a + b

definir restar(flotante a, flotante b) -> flotante:
    retornar a - b

definir multiplicar(flotante a, flotante b) -> flotante:
    retornar a * b

definir dividir(flotante a, flotante b) -> flotante:
    si(b == 0) entonces
        imprimir("Error: division entre cero, se devuelve 0")
        retornar 0.0
    retornar a / b

// Despacha las operaciones 1-4 usando elegir
definir aplicar(entero op, flotante a, flotante b) -> flotante:
    flotante r = 0.0
    si (op == 0) entonces
        r = sumar(a, b)
    si (op == 2) entonces
        r = restar(a, b)
    si (op == 3) entonces
        r = multiplicar(a, b)
    si (op == 4) entonces
        r = dividir(a, b)
    sino (op > 4) entonces
        imprimir("Operacion desconocida")
    retornar r

/* ---------- Matematicas ---------- */

definir potencia(flotante base, entero exp) -> flotante:
    flotante r = 1.0
    entero e = exp
    si(e < 0) entonces
        e = 0 - e
    para(entero i = 0; i < e; i++):
        r = r * base
    si(exp < 0) entonces
        retornar 1.0 / r
    retornar r

// Recursividad
definir factorial(entero n) -> entero:
    si(n < 2) entonces
        retornar 1
    retornar n * factorial(n - 1)

// Recursividad doble
definir fibonacci(entero n) -> entero:
    si(n < 2) entonces
        retornar n
    retornar fibonacci(n - 1) + fibonacci(n - 2)

definir esPrimo(entero n) -> bool:
    si(n < 2) entonces
        retornar falso
    entero d = 2
    mientras(d * d < n + 1) hacer
        si(n - (n / d) * d == 0) entonces
            retornar falso
        d++
    retornar verdadero

definir mcd(entero a, entero b) -> entero:
    entero x = a
    entero y = b
    entero temp = 0
    mientras(y != 0) hacer
        temp = y
        y = x - (x / y) * y
        x = temp
    retornar x

definir contarDigitos(entero n) -> entero:
    entero c = 0
    entero v = n
    si(v < 0) entonces
        v = 0 - v
    hacer:
        v = v / 10
        c++
    mientras(v > 0)
    retornar c

// continuar y romper: suma los impares hasta limite, se detiene si la suma pasa de 1000
definir sumaImpares(entero limite) -> entero:
    entero suma = 0
    para(entero i = 1; i < limite + 1; i++):
        si(i - (i / 2) * 2 == 0) entonces
            continuar
        si(suma > 1000) entonces
            romper
        suma = suma + i
    retornar suma

/* ---------- Arreglos (por referencia) ---------- */

definir sumarArreglo([] flotante datos, entero n) -> flotante:
    flotante s = 0.0
    para(entero i = 0; i < n; i++):
        s = s + datos[i]
    retornar s

definir maximoArreglo([] flotante datos, entero n) -> flotante:
    flotante m = datos[0]
    para(entero i = 1; i < n; i++):
        si(datos[i] > m) entonces
            m = datos[i]
    retornar m

/* ---------- Historial (estructura por referencia) ---------- */

definir registrar({} Historial h, flotante valor):
    si(h.cantidad < 10) entonces
        h.valores[h.cantidad] = valor
        h.cantidad++
    contrario
        para(entero i = 1; i < 10; i++):
            h.valores[i - 1] = h.valores[i]
        h.valores[9] = valor

definir mostrarHistorial({} Historial h):
    si(h.cantidad == 0) entonces
        imprimir("Historial vacio")
    contrario
        imprimir("--- Historial (" + h.cantidad + " resultados) ---")
        para(entero i = 0; i < h.cantidad; i++):
            imprimir("#" + i + ": " + h.valores[i])

definir promedioHistorial({} Historial h) -> flotante:
    si(h.cantidad == 0) entonces
        retornar 0.0
    flotante suma = 0.0
    para(entero i = 0; i < h.cantidad; i++):
        suma = suma + h.valores[i]
    retornar suma / h.cantidad

/* ---------- Entrada / salida ---------- */

definir pedirNumero(cadena mensaje) -> flotante:
    imprimir(mensaje)
    flotante v = leer()
    retornar v

definir imprimirMenu():
    imprimir("")
    imprimir("=========== CALCULADORA ZETARIANA ===========")
    imprimir(" 1. Sumar             2. Restar")
    imprimir(" 3. Multiplicar       4. Dividir")
    imprimir(" 5. Potencia          6. Factorial")
    imprimir(" 7. Serie Fibonacci   8. Es primo?")
    imprimir(" 9. MCD              10. Lista (suma/max)")
    imprimir("11. Historial        12. Digitos y suma de impares")
    imprimir(" 0. Salir")
    imprimir("=============================================")
