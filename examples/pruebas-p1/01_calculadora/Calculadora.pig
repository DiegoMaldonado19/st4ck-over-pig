##
    Proyecto de prueba 1: Calculadora
    Combina Pig Latin (.pig) con funciones y estructuras de Y? (.y)
##
import lib.Operaciones.y

##
    Variables globales
##
VARIABILES>
esto opcion : numerus 0;
esto n : numerus 0;
esto m : numerus 0;
esto contador : numerus 0;
esto cuenta : numerus 3;
esto a : decimalis 0.0;
esto b : decimalis 0.0;
esto resultado : decimalis 0.0;
esto nombre : textum "Cadete";
esto esprimo : bool falsus;
series lista[10] : decimalis {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
series valoresHist[10] : decimalis {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
esto hist : Historial {0, valoresHist};

##
    Funcion principal
##
MAIOR>
>> "Ingresa tu nombre:" ;
nombre <<
>> "Bienvenido " >> nombre ;

// Cuenta regresiva con ciclo simple
dum (cuenta > 0) {
    >> "Iniciando en " >> cuenta ;
    cuenta = cuenta - 1;
} finis;

// Ciclo principal (do-while)
facere {
    imprimirMenu();
    >> "Elige una opcion:" ;
    opcion <<

    si (opcion < 0 || opcion > 12) {
        >> "Opcion invalida, intenta de nuevo" ;
        perge;
    } finis;

    si (opcion >= 1 && opcion <= 4) {
        a = pedirNumero("Ingresa el primer numero:");
        b = pedirNumero("Ingresa el segundo numero:");
        resultado = aplicar(opcion, a, b);
        >> "Resultado: " >> resultado ;
        registrar(hist, resultado);
    } aliter (opcion == 5) {
        a = pedirNumero("Ingresa la base:");
        >> "Ingresa el exponente (entero):" ;
        n <<
        resultado = potencia(a, n);
        >> a >> " ^ " >> n >> " = " >> resultado ;
        registrar(hist, resultado);
    } aliter (opcion == 6) {
        >> "Ingresa n para calcular n!:" ;
        n <<
        si (n < 0) {
            >> "No existe el factorial de un negativo" ;
        } aliter {
            >> n >> "! = " >> factorial(n) ;
            registrar(hist, factorial(n));
        } finis;
    } aliter (opcion == 7) {
        >> "Cuantos terminos de Fibonacci? (1 a 20)" ;
        n <<
        si (n < 1 || n > 20) {
            >> "Valor fuera de rango" ;
        } aliter {
            per (esto i : numerus 0; i < n; i++) {
                >> "F(" >> i >> ") = " >> fibonacci(i) ;
            }
        } finis;
    } aliter (opcion == 8) {
        >> "Ingresa un entero:" ;
        n <<
        esprimo = esPrimo(n);
        si (esprimo == verum) {
            >> n >> " es primo" ;
        } aliter {
            >> n >> " NO es primo" ;
        } finis;
    } aliter (opcion == 9) {
        >> "Ingresa el primer entero:" ;
        n <<
        >> "Ingresa el segundo entero:" ;
        m <<
        >> "MCD(" >> n >> ", " >> m >> ") = " >> mcd(n, m) ;
    } aliter (opcion == 10) {
        contador = 0;
        >> "Ingresa hasta 10 numeros (0 para terminar):" ;
        per (esto i : numerus 0; i < 10; i++) {
            a <<
            si (a == 0) {
                interrumpe;
            } finis;
            lista[i] = a;
            contador = contador + 1;
        }
        si (contador == 0) {
            >> "No ingresaste numeros" ;
        } aliter {
            >> "Cantidad: " >> contador ;
            >> "Suma: " >> sumarArreglo(lista, contador) ;
            >> "Maximo: " >> maximoArreglo(lista, contador) ;
            >> "Promedio: " >> sumarArreglo(lista, contador) / contador ;
        } finis;
    } aliter (opcion == 11) {
        mostrarHistorial(hist);
        >> "Promedio del historial: " >> promedioHistorial(hist) ;
    } aliter (opcion == 12) {
        >> "Ingresa un entero:" ;
        n <<
        >> "Digitos: " >> contarDigitos(n) ;
        >> "Suma de impares hasta " >> n >> ": " >> sumaImpares(n) ;
    } aliter {
        >> "Saliendo de la calculadora..." ;
    } finis;

} dum (opcion != 0);

>> "Hasta luego, " >> nombre ;
FINIS;
