// ============================================================
//  p2/encapsulamiento/main.pig - encapsulamiento (fases 7 y 8), lo valido.
//  Cuenta.z usa sus miembros de las cuatro visibilidades; CuentaAhorro.z
//  (clase hija) usa lo public y lo protected; Banco.z (otra clase) y este
//  .pig usan solo lo public: atributos, metodos y constructores. Lo
//  default funciona como private (auxiliar, 22-oct): solo Cuenta.z lo usa,
//  y lo ofrece hacia fuera con getSaldo, depositar, cerrarMes y abrir.
//  Lo prohibido esta en errores.pig.
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
esto luis : Cuenta = cuenta.abrir("Luis");
esto copia : Cuenta = cuenta.abrir("Temporal");
esto banco : novus Banco(cuenta);
esto ok : bool falsus;
esto ahorro : novus CuentaAhorro(1000);
esto luisa : novus CuentaAhorro("Luisa");

MAIOR>
>> "Titular: " >> cuenta.titular;
cuenta.depositar(500);
ok = cuenta.retirar(1234, 300);
>> "Retiro con el pin correcto: " >> ok;
ok = cuenta.retirar(1111, 300);
>> "Retiro con otro pin: " >> ok;
cuenta.cerrarMes();
>> "Saldo con interes: " >> cuenta.getSaldo();
copia = cuenta.vacia();
>> "Vacia: " >> copia.titular >> " " >> copia.getSaldo();
copia = cuenta.interna(70);
>> "Interna: " >> copia.titular >> " " >> copia.getSaldo();
>> "Mismo pin que Luis: " >> cuenta.mismoPin(luis);
banco.reporte();
>> "Ahorro: " >> ahorro.titular >> " capitaliza " >> ahorro.capitalizar();
>> "Ahorro de " >> luisa.titular >> ": " >> luisa.getSaldo();
FINIS;
