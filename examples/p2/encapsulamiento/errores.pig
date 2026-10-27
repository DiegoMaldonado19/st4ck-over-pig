// ============================================================
//  p2/encapsulamiento/errores.pig - encapsulamiento (fases 7 y 8), lo
//  prohibido. Lo private y lo default de Cuenta solo se usan dentro de su
//  clase (default funciona como private, auxiliar 22-oct), y lo protected
//  tambien en sus clases hijas: ni este .pig, ni otra clase (Intruso.z),
//  ni una clase hija con lo private o lo default (CuentaEspia.z) pueden.
//  Errores esperados (27), con el mensaje "... es private (o default, o
//  protected): solo puede usarse dentro de la clase 'Cuenta'.":
//    CuentaEspia.z  5:9   constructor 'Cuenta.Cuenta()', private (con super)
//    CuentaEspia.z  9:22  atributo 'Cuenta.pin', private (lectura)
//    CuentaEspia.z 10:9   atributo 'Cuenta.pin', private (escritura)
//    CuentaEspia.z 11:22  metodo 'Cuenta.validarPin(int)', private
//    CuentaEspia.z 12:28  metodo 'Cuenta.validarPin(int)', private (con this)
//    CuentaEspia.z 13:9   atributo 'Cuenta.saldo', default
//    CuentaEspia.z 14:9   metodo 'Cuenta.abonarInteres()', default
//    CuentaEspia.z 18:9   constructor 'Cuenta.Cuenta(String)', default (con super)
//    Intruso.z      5:25  atributo 'Cuenta.pin', private
//    Intruso.z      6:15  atributo 'Cuenta.tasa', protected (y de sus clases hijas)
//    Intruso.z      7:28  metodo 'Cuenta.validarPin(int)', private
//    Intruso.z      8:27  metodo 'Cuenta.interes()', protected
//    Intruso.z      9:24  constructor 'Cuenta.Cuenta()', private
//    Intruso.z     10:26  constructor 'Cuenta.Cuenta(int)', protected
//    Intruso.z     11:15  atributo 'Cuenta.saldo', default
//    Intruso.z     12:15  metodo 'Cuenta.abonarInteres()', default
//    Intruso.z     13:24  constructor 'Cuenta.Cuenta(String)', default
//    errores.pig   43:14  constructor 'Cuenta.Cuenta()', private
//    errores.pig   44:16  constructor 'Cuenta.Cuenta(int)', protected
//    errores.pig   49:11  atributo 'Cuenta.pin', private (lectura)
//    errores.pig   50:7   atributo 'Cuenta.pin', private (escritura)
//    errores.pig   51:7   atributo 'Cuenta.tasa', protected
//    errores.pig   52:12  metodo 'Cuenta.validarPin(int)', private
//    errores.pig   53:11  metodo 'Cuenta.interes()', protected
//    errores.pig   54:11  atributo 'Cuenta.saldo', default
//    errores.pig   55:7   metodo 'Cuenta.abonarInteres()', default
//    errores.pig   56:14  constructor 'Cuenta.Cuenta(String)', default
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
n = cuenta.saldo;
cuenta.abonarInteres();
esto nueva : novus Cuenta("Luis");
FINIS;
