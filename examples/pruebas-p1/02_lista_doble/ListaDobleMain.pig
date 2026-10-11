##
    Proyecto de prueba 4: Lista doblemente enlazada
    Clases .z: Nodo y ListaDoble. Un unico .y: Utilidades
##
import clases.Nodo.z
import clases.ListaDoble.z
import lib.Utilidades.y

VARIABILES>
esto opcion : numerus 0;
esto n : numerus 0;
esto pos : numerus 0;
esto indice : numerus 0;
esto ok : bool falsus;
esto lista : novus ListaDoble();
esto otra : novus ListaDoble(7);
esto stats : Registro {0, 0, 0, 0};

MAIOR>
>> "=== LISTA DOBLEMENTE ENLAZADA ===" ;

// Demo: dos listas independientes
per (esto i : numerus 1; i < 6; i++) {
    lista.insertarFinal(i * 10);
}
otra.insertarFinal(8, 9);
>> "Lista inicial (adelante):" ;
lista.imprimirAdelante();
>> "Lista inicial (atras):" ;
lista.imprimirAtras();
>> "Otra lista:" ;
otra.imprimirAdelante();

facere {
    >> "" ;
    >> "1. Insertar al inicio    2. Insertar al final    3. Insertar en posicion" ;
    >> "4. Eliminar inicio       5. Eliminar final       6. Eliminar posicion" ;
    >> "7. Eliminar por valor    8. Buscar valor         9. Invertir" ;
    >> "10. Ordenar              11. Imprimir            12. Estadisticas" ;
    >> "13. Vaciar               0. Salir" ;
    opcion <<

    si (opcion == 1) {
        >> "Valor:" ;
        n <<
        lista.insertarInicio(n);
        registrar(stats, 1);
    } aliter (opcion == 2) {
        >> "Valor:" ;
        n <<
        lista.insertarFinal(n);
        registrar(stats, 1);
    } aliter (opcion == 3) {
        >> "Posicion:" ;
        pos <<
        >> "Valor:" ;
        n <<
        ok = lista.insertarEn(pos, n);
        si (ok == verum) {
            >> "Insertado en la posicion " >> pos ;
            registrar(stats, 1);
        } aliter {
            >> "Posicion invalida" ;
        } finis;
    } aliter (opcion == 4) {
        si (lista.eliminarInicio()) {
            >> "Elemento eliminado del inicio" ;
            registrar(stats, 2);
        } aliter {
            >> "Lista vacia" ;
        } finis;
    } aliter (opcion == 5) {
        si (lista.eliminarFinal()) {
            >> "Elemento eliminado del final" ;
            registrar(stats, 2);
        } aliter {
            >> "Lista vacia" ;
        } finis;
    } aliter (opcion == 6) {
        >> "Posicion:" ;
        pos <<
        ok = lista.eliminarEn(pos);
        si (ok == verum) {
            >> "Eliminado el elemento de la posicion " >> pos ;
            registrar(stats, 2);
        } aliter {
            >> "Posicion invalida" ;
        } finis;
    } aliter (opcion == 7) {
        >> "Valor a eliminar:" ;
        n <<
        ok = lista.eliminarValor(n);
        si (ok == verum) {
            >> "Valor " >> n >> " eliminado" ;
            registrar(stats, 2);
        } aliter {
            >> "El valor no existe" ;
        } finis;
    } aliter (opcion == 8) {
        >> "Valor a buscar:" ;
        n <<
        indice = lista.buscar(n);
        si (indice == -1) {
            >> "No encontrado" ;
        } aliter {
            >> "Encontrado en el indice " >> indice ;
            registrar(stats, 3);
        } finis;
    } aliter (opcion == 9) {
        lista.invertir();
        >> "Lista invertida" ;
        lista.imprimirAdelante();
    } aliter (opcion == 10) {
        lista.ordenar();
        >> "Lista ordenada" ;
        lista.imprimirAdelante();
    } aliter (opcion == 11) {
        >> "Adelante:" ;
        lista.imprimirAdelante();
        >> "Atras:" ;
        lista.imprimirAtras();
    } aliter (opcion == 12) {
        si (lista.estaVacia()) {
            >> "La lista esta vacia" ;
        } aliter {
            >> "Tamano: " >> lista.tamanio >> " (" >> paridad(lista.tamanio) >> ")" ;
            >> "Suma: " >> lista.suma() ;
            >> "Maximo: " >> lista.maximo() ;
            >> "Primero: " >> lista.cabeza.dato >> " | Ultimo: " >> lista.cola.dato ;
        } finis;
    } aliter (opcion == 13) {
        lista.vaciar();
        >> "Lista vaciada" ;
    } aliter (opcion == 0) {
        >> "Saliendo..." ;
    } aliter {
        >> "Opcion invalida" ;
    } finis;
} dum (opcion != 0);

imprimirRegistro(stats);
FINIS;
