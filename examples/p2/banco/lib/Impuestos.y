// Impuestos.y
// Comisiones y bitacora de movimientos (lenguaje Y?)

%estructuras
estructura Bitacora:
    entero conteo
    flotante montos[10]

%funciones

definir comision(flotante monto) -> flotante:
    si(monto > 1000.0) entonces
        retornar monto * 0.01
    sino (monto > 100.0) entonces
        retornar 1.5
    contrario
        retornar 0.0

definir anotar({} Bitacora b, flotante monto):
    si(b.conteo < 10) entonces
        b.montos[b.conteo] = monto
        b.conteo++

definir totalBitacora({} Bitacora b) -> flotante:
    flotante total = 0.0
    para(entero i = 0; i < b.conteo; i++):
        total = total + b.montos[i]
    retornar total

definir imprimirBitacora({} Bitacora b):
    imprimir("===== BITACORA =====")
    imprimir("Movimientos anotados: " + b.conteo)
    para(entero i = 0; i < b.conteo; i++):
        imprimir("  #" + i + ": " + b.montos[i])
    imprimir("Total movido: " + totalBitacora(b))
