// Estadisticas.y
// Marcador del ahorcado (lenguaje Y?)
// Prueba: estructura con arreglo, bool, elegir, operadores ! && ||, caracteres

%estructuras
estructura Marcador:
    entero partidas
    entero ganadas
    entero perdidas
    entero racha
    entero mejorRacha
    entero puntajes[5]

%funciones

definir validarLetra(caracter c) -> bool:
    retornar !(c < 'a' || c > 'z')

definir calcularPuntaje(entero longitud, entero errores, entero maxErrores) -> entero:
    entero base = longitud * 10
    entero penalizacion = errores * 5
    entero bono = 0
    si(errores == 0) entonces
        bono = 50
    entero total = base - penalizacion + bono
    si(total < 0) entonces
        total = 0
    retornar total

definir registrarResultado({} Marcador m, bool gano, entero puntaje):
    si(m.partidas < 5) entonces
        m.puntajes[m.partidas] = puntaje
    m.partidas++
    si(gano == verdadero) entonces
        m.ganadas++
        m.racha++
        si(m.racha > m.mejorRacha) entonces
            m.mejorRacha = m.racha
    contrario
        m.perdidas++
        m.racha = 0

definir sumarPuntajes({} Marcador m) -> entero:
    entero total = 0
    para(entero i = 0; i < m.partidas; i++):
        total = total + m.puntajes[i]
    retornar total

definir porcentajeVictorias({} Marcador m) -> flotante:
    si(m.partidas == 0) entonces
        retornar 0.0
    retornar (m.ganadas * 100.0) / m.partidas

definir titulo(entero total) -> cadena:
    entero tramo = total / 100
    cadena t = "Novato"
    elegir(tramo) :
        caso 0:
            t = "Novato"
            romper
        caso 1:
            t = "Aprendiz"
            romper
        siempre:
            t = "Maestro del ahorcado"
            romper
    retornar t

definir mostrarMarcador({} Marcador m):
    imprimir("----- MARCADOR -----")
    imprimir("Partidas: " + m.partidas + " | Ganadas: " + m.ganadas + " | Perdidas: " + m.perdidas)
    imprimir("Racha actual: " + m.racha + " | Mejor racha: " + m.mejorRacha)
    imprimir("Victorias: " + porcentajeVictorias(m) + "%")
