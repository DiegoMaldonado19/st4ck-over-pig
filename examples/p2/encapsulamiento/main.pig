// ============================================================
//  p2/encapsulamiento/main.pig - encapsulamiento (fases 7 y 8), lo valido.
//  Cuenta.z usa sus miembros de las cuatro visibilidades; CuentaAhorro.z
//  (clase hija) usa lo public, lo default y lo protected; Banco.z (otra
//  clase) y este .pig usan solo lo public y lo default: atributos,
//  metodos y constructores. Lo prohibido esta en errores.pig.
//
//  Salida esperada:
//    Titular: Diego
//    Retiro con el pin correcto: verum
//    Retiro con otro pin: falsus
//    Saldo con interes: 1320
//    Vacia: Vacia 0
//    Interna: Interna 70
//    Mismo pin que Luis: falsus
//    Banco: Diego tiene 1562
//    Banco: Luis 0 false
//    Ahorro: Ahorro capitaliza 1440
//    Ahorro de Luisa: 0
// ============================================================
import Cuenta.z
import Banco.z
import CuentaAhorro.z

VARIABILES>
esto cuenta : novus Cuenta("Diego", 1000, 1234);
esto luis : novus Cuenta("Luis");
esto copia : novus Cuenta("Temporal");
esto banco : novus Banco(cuenta);
esto ok : bool falsus;
esto ahorro : novus CuentaAhorro(1000);
esto luisa : novus CuentaAhorro("Luisa");

MAIOR>
>> "Titular: " >> cuenta.titular;
cuenta.saldo = cuenta.saldo + 500;
ok = cuenta.retirar(1234, 300);
>> "Retiro con el pin correcto: " >> ok;
ok = cuenta.retirar(1111, 300);
>> "Retiro con otro pin: " >> ok;
cuenta.abonarInteres();
>> "Saldo con interes: " >> cuenta.saldo;
copia = cuenta.vacia();
>> "Vacia: " >> copia.titular >> " " >> copia.saldo;
copia = cuenta.interna(70);
>> "Interna: " >> copia.titular >> " " >> copia.saldo;
>> "Mismo pin que Luis: " >> cuenta.mismoPin(luis);
banco.reporte();
>> "Ahorro: " >> ahorro.titular >> " capitaliza " >> ahorro.capitalizar();
>> "Ahorro de " >> luisa.titular >> ": " >> luisa.saldo;
FINIS;
