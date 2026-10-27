import banco.Cuenta.z
import banco.CuentaAhorro.z
import banco.CuentaCorriente.z
import banco.Banco.z
import lib.Impuestos.y

VARIABILES>
esto banco : novus Banco() : Banco ;
esto c1 : novus CuentaAhorro("Ana", 1001, 0.05) : Cuenta ;
esto c2 : novus CuentaCorriente("Beto", 1002, 500.0) : Cuenta ;
esto c3 : novus Cuenta("Carla", 1003) : Cuenta ;
esto buscada : null : Cuenta ;
esto ok : bool falsus;
esto monto : decimalis 0.0;
series valoresBit[10] : decimalis {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
esto bitacora : Bitacora {0, valoresBit};

MAIOR>
>> "=== BANCO ZETARIANO ===" ;
banco.agregar(c1);
banco.agregar(c2);
banco.agregar(c3);

>> "--- Depositos iniciales ---" ;
c1.depositar(2000.0);
c2.depositar(300.0);
c3.depositar(100.0);

>> "--- Operaciones ---" ;
monto = 500.0;
ok = c1.transferir(c2, monto);
si (ok == verum) {
    anotar(bitacora, monto);
    >> "Comision sugerida: " >> comision(monto) ;
} finis;

monto = 1200.0;
ok = c2.retirar(monto);
si (ok == verum) {
    anotar(bitacora, monto);
    >> "Comision sugerida: " >> comision(monto) ;
} aliter {
    >> "Retiro rechazado" ;
} finis;

monto = 200.0;
ok = c2.retirar(monto);
si (ok == verum) {
    anotar(bitacora, monto);
} aliter {
    >> "Retiro rechazado" ;
} finis;

monto = 150.0;
ok = c3.retirar(monto);
si (ok == verum) {
    anotar(bitacora, monto);
} aliter {
    >> "Retiro rechazado" ;
} finis;

monto = 50.0;
ok = c3.transferir(c1, monto);
si (ok == verum) {
    anotar(bitacora, monto);
    >> "Comision sugerida: " >> comision(monto) ;
} finis;

banco.finDeMes();
banco.imprimirResumen();
>> "Total depositado en el banco: " >> banco.totalDepositado() ;

>> "--- Busquedas ---" ;
buscada = banco.buscar(1002);
si (buscada == null) {
    >> "Cuenta 1002 no encontrada" ;
} aliter {
    >> "Cuenta 1002 pertenece a " >> buscada.getTitular() ;
} finis;

buscada = banco.buscar(9999);
si (buscada == null) {
    >> "Cuenta 9999 no encontrada" ;
} aliter {
    >> "Cuenta 9999 pertenece a " >> buscada.getTitular() ;
} finis;

imprimirBitacora(bitacora);
FINIS;
