// ============================================================
//  p2/integrador/main.pig - la mision de la resistencia: los tres
//  lenguajes juntos.
//    - Herencia y polimorfismo: Agente.z y tres clases hijas, una de
//      ellas en un tercer nivel (HackerElite.z).
//    - Arreglo de objetos: 'equipo' guarda un agente de cada clase, y
//      reporte() y poder() responden la version de cada una.
//    - Matriz: Mapa.z guarda los guardias del salon en int[][].
//    - Recursividad: fibonacci en utilidades.y, potencia en Hacker.z y
//      guardiasDesde en Mapa.z.
//    - Arreglo por referencia: 'puntajes' llega a sumarArreglo de Y?.
//
//  Entrada (main.entrada.txt): "Diego" y "6". Salida esperada:
//    Nombre del lider de la mision:
//    Escalones hasta el servidor:
//    Agente Diego: poder 30
//    Explorador Ana: poder 50
//    Hacker Zeta: poder 32
//    Hacker de élite Neo: poder 64
//    Poder del equipo: 176
//    Guardias en el salon: 10
//    Columna mas segura: 1
//    Formas de subir 6 escalones: 13
//    La mision de Diego puede empezar.
// ============================================================
import utilidades.y
import Agente.z
import Explorador.z
import Hacker.z
import HackerElite.z
import Mapa.z

VARIABILES>
esto lider : textum "";
esto escalones : numerus 0;
series equipo[4] : Agente;
series puntajes[4] : numerus;
esto mapa : novus Mapa(3, 4);
esto total : numerus 0;

MAIOR>
>> "Nombre del lider de la mision:";
lider <<
>> "Escalones hasta el servidor:";
escalones <<
equipo[0] = novus Agente(lider, 3);
equipo[1] = novus Explorador("Ana", 4, 5);
equipo[2] = novus Hacker("Zeta", 5);
equipo[3] = novus HackerElite("Neo");
per (esto i : numerus 0; i < 4; i++) {
    >> equipo[i].reporte();
    puntajes[i] = equipo[i].poder();
}
total = sumarArreglo(puntajes, 4);
>> "Poder del equipo: " >> total;
mapa.vigilar(0, 0, 2);
mapa.vigilar(0, 3, 1);
mapa.vigilar(1, 0, 1);
mapa.vigilar(1, 2, 1);
mapa.vigilar(2, 2, 3);
mapa.vigilar(2, 3, 2);
>> "Guardias en el salon: " >> mapa.guardiasDesde(0);
>> "Columna mas segura: " >> mapa.columnaMasSegura();
>> "Formas de subir " >> escalones >> " escalones: " >> fibonacci(escalones);
si (total > mapa.guardiasDesde(0) * 10) {
    >> "La mision de " >> lider >> " puede empezar.";
} aliter {
    >> "Faltan agentes.";
} finis;
FINIS;
