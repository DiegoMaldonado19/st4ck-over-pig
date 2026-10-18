// ============================================================
//  p2/encapsulamiento/errores.pig - encapsulamiento (fases 7 y 8), lo
//  prohibido. Lo private de Cuenta solo se usa dentro de su clase, y lo
//  protected tambien en sus clases hijas: ni este .pig, ni otra clase
//  (Intruso.z), ni una clase hija con lo private (CuentaEspia.z) pueden.
//  Errores esperados (18), con el mensaje "... es private: solo puede
//  usarse dentro de la clase 'Cuenta'.":
//    CuentaEspia.z  5:9   constructor 'Cuenta.Cuenta()', private (con super)
//    CuentaEspia.z  9:22  atributo 'Cuenta.pin', private (lectura)
//    CuentaEspia.z 10:9   atributo 'Cuenta.pin', private (escritura)
//    CuentaEspia.z 11:22  metodo 'Cuenta.validarPin(int)', private
//    CuentaEspia.z 12:28  metodo 'Cuenta.validarPin(int)', private (con this)
//    Intruso.z      5:25  atributo 'Cuenta.pin', private
//    Intruso.z      6:15  atributo 'Cuenta.tasa', protected (y de sus clases hijas)
//    Intruso.z      7:28  metodo 'Cuenta.validarPin(int)', private
//    Intruso.z      8:27  metodo 'Cuenta.interes()', protected
//    Intruso.z      9:24  constructor 'Cuenta.Cuenta()', private
//    Intruso.z     10:26  constructor 'Cuenta.Cuenta(int)', protected
//    errores.pig   33:14  constructor 'Cuenta.Cuenta()', private
//    errores.pig   34:16  constructor 'Cuenta.Cuenta(int)', protected
//    errores.pig   39:11  atributo 'Cuenta.pin', private (lectura)
//    errores.pig   40:7   atributo 'Cuenta.pin', private (escritura)
//    errores.pig   41:7   atributo 'Cuenta.tasa', protected
//    errores.pig   42:12  metodo 'Cuenta.validarPin(int)', private
//    errores.pig   43:11  metodo 'Cuenta.interes()', protected
// ============================================================
import Cuenta.z
import Intruso.z
import CuentaEspia.z

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
