// Ejemplos del enunciado para Y?, importados por examples/enunciado.pig.
// Las normalizaciones estan listadas en el encabezado de ese archivo.
%estructuras
estructura Direccion:
    cadena calle
    entero numero

estructura Persona:
    cadena nombre
    entero edad
    Direccion domicilio        // estructura anidada

estructura Estudiante:
    cadena nombre
    entero calificaciones[3]   // un arreglo dentro de la estructura

%funciones
definir calcularPoder(entero fuerza) -> entero :
    retornar fuerza * 10

definir ejemplos():
    // Declaracion de variables
    entero sinInicializacion
    entero edadUsuario = 25
    flotante temperatura = 36.5
    caracter inicial = 'A'
    bool activo = verdadero
    cadena saludos = "Saludos zetarianos"
    entero numeros[ 5 ] = { 10 , 20 , 30 , 40 , 50 }
    entero resultado
    resultado = numeros[ 0 ] + numeros[ 1 ]   // 10 + 20 = 30
    numeros[ 2 ] = numeros[ 0 ] * 3            // numeros[2] ahora vale 30
    Persona alumno
    alumno.nombre = "Yennifer"

    // Se pueden declarar estructuras dentro de las funciones
    estructura Punto:
        entero x
        entero y
        flotante promedio
    Punto p1 = { 10 , 20 , 85.5 }
    Punto p2 = { 5 , 15 , 90.0 }
    flotante sumaPromedios
    sumaPromedios = p1.promedio + p2.promedio
    Punto p3
    p3 = p1

    // Estructuras condicionales
    entero edad = 18
    bool condicion = verdadero
    si(edad > 18 ) entonces
        imprimir("Codigo si es mayor de edad")
        si(condicion == verdadero) entonces
            imprimir("Otra condicion")
        imprimir("Esto siempre se imprime")
    sino (edad == 18 ) entonces
        imprimir("Codigo si tiene exactamente 18")
    contrario
        imprimir("Codigo si es menor de edad")

    entero opcion = 2
    entero x
    elegir(opcion) {
        caso 1 :
            // Codigo para la opcion 1
            x = 10
            romper
        caso 2 :
            x = 20
            romper
        siempre:
            // Codigo por defecto
            x = 30
            romper
    }

    // Ciclos
    para(entero i = 0 ; i < 10 ; i++):
        si(i == 3 ) entonces
            continuar   // Salta esta iteracion cuando i es 3
        si(i == 8 ) entonces
            romper      // Sale del ciclo por completo cuando i es 8
    entero contador = 0
    mientras(contador < 5 ) hacer
        contador++;
        si(contador == 2 ) entonces
            continuar;  // Salta el resto del ciclo y vuelve a evaluar
    entero intentos = 0
    hacer:
        intentos++
        si(intentos == 4 ) entonces
            romper      // Rompe el ciclo inmediatamente
    mientras(intentos < 10 )

    // Funciones especiales
    imprimir("Imprimir")
    leer()
    cadena texto = leer()
    texto = leer()
