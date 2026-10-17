// ============================================================
//  p2/encapsulamiento/errores.pig - encapsulamiento (Fase 7), lo prohibido.
//  Lo private y lo protected de Cuenta solo se usan dentro de su clase: ni
//  este .pig ni otra clase (Intruso.z) pueden. Errores esperados (13), con el
//  mensaje "... es private: solo puede usarse dentro de la clase 'Cuenta'.":
//    Intruso.z    5:25  atributo 'Cuenta.pin', private
//    Intruso.z    6:15  atributo 'Cuenta.tasa', protected (y de sus clases hijas)
//    Intruso.z    7:28  metodo 'Cuenta.validarPin(int)', private
//    Intruso.z    8:27  metodo 'Cuenta.interes()', protected
//    Intruso.z    9:24  constructor 'Cuenta.Cuenta()', private
//    Intruso.z   10:26  constructor 'Cuenta.Cuenta(int)', protected
//    errores.pig 25:14  constructor 'Cuenta.Cuenta()', private
//    errores.pig 26:16  constructor 'Cuenta.Cuenta(int)', protected
//    errores.pig 31:11  atributo 'Cuenta.pin', private (lectura)
//    errores.pig 32:7   atributo 'Cuenta.pin', private (escritura)
//    errores.pig 33:7   atributo 'Cuenta.tasa', protected
//    errores.pig 34:12  metodo 'Cuenta.validarPin(int)', private
//    errores.pig 35:11  metodo 'Cuenta.interes()', protected
// ============================================================
import Cuenta.z
import Intruso.z

VARIABILES>
esto cuenta : novus Cuenta("Diego", 1000, 1234);
esto vacia : novus Cuenta();
esto interna : novus Cuenta(5);
esto n : numerus 0;
esto ok : bool falsus;

MAIOR>
n = cuenta.pin;
cuenta.pin = 1;
cuenta.tasa = 50;
ok = cuenta.validarPin(1234);
n = cuenta.interes();
FINIS;
