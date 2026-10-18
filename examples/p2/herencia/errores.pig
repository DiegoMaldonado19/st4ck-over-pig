// ============================================================
//  p2/herencia/errores.pig - herencia (Fase 8), lo prohibido.
//  Errores esperados (7):
//    Ciclo.z      2:1   herencia ciclica (Ciclo -> Ciclo)
//    Huerfano.z   2:1   la clase padre 'Fantasma' no existe
//    Leon.z       7:9   super(...) no es la primera instruccion
//    Leon.z      10:5   'Gato' no tiene constructor sin parametros y
//                       Leon(int) no empieza con super(...)
//    Leon.z      15:16  'energia' es private de Animal: Leon no la nombra
//    Tigre.z      3:1   'Gato' no tiene constructor sin parametros y Tigre
//                       no declara uno que llame a super(...)
//    errores.pig 30:1   un Animal no cabe en una variable Perro
// ============================================================
import Animal.z
import Perro.z
import Gato.z
import Ciclo.z
import Huerfano.z
import Leon.z
import Tigre.z

VARIABILES>
esto animal : novus Animal();
esto perro : novus Perro();
esto mascota : Animal = perro;

MAIOR>
mascota = animal;
perro.ladrar();
perro = animal;
FINIS;
