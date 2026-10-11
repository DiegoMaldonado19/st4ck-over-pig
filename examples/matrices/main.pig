// ============================================================
//  matrices/main.pig - arreglos de varias dimensiones, con los
//  literales que la auxiliar confirmo por Telegram (21/09/2026).
//  Se aplanan fila por fila en el heap: m[i][j] = m + i*columnas + j.
//  Matrices.y declara la suya partida en varias lineas.
//
//  Salida esperada:
//    matriz[1][2] = 6
//    matriz[0][1] = 40
//    cb
//    suma Y? = 21
//    100
//    010
//    001
//    uno otro
// ============================================================
import Matrices.y

VARIABILES>
series matriz[2] [3] : numerus {
  {3, 2, 1},
  {4, 5, 6}
};
series nombres[2][2] : textum {{"a", "b"}, {"c", "d"}};

MAIOR>
>> "matriz[1][2] = " >> matriz[1][2];
matriz[0][1] = matriz[1][0] * 10;
>> "matriz[0][1] = " >> matriz[0][1];
>> nombres[1][0] + nombres[0][1];
>> "suma Y? = " >> sumarMatriz();
identidad();
>> elegirEnMatriz(1) >> " " >> elegirEnMatriz(7);
FINIS;
