%estructuras
estructura Tablero:
    cadena nombre
    entero casillas[4]

%funciones
definir sumarMatriz() -> entero:
    entero matriz[3] [2] = {
      {1, 2},
      {3, 4},
      {5, 6}
    }
    entero total = 0
    para(entero i = 0; i < 3; i++):
        para(entero j = 0; j < 2; j++):
            total = total + matriz[i][j]
    retornar total

definir identidad():
    entero m[3][3]
    para(entero i = 0; i < 3; i++):
        m[i][i] = 1
    para(entero f = 0; f < 3; f++):
        imprimir(m[f][0], m[f][1], m[f][2])

definir elegirEnMatriz(entero opcion) -> cadena:
    elegir(opcion) {
        caso 1:
            retornar "uno"
        siempre:
            retornar "otro"
    }
