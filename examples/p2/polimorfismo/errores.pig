// ============================================================
//  p2/polimorfismo/errores.pig - polimorfismo (Fase 8), lo prohibido.
//  @Override es obligatorio en un metodo que sobrescribe a otro, y el
//  metodo sobrescrito conserva su tipo de retorno y su visibilidad.
//  Errores esperados (5):
//    ImpresoraRota.z  4:5   imprimir() sobrescribe sin @Override
//    ImpresoraRota.z  8:5   @Override en escanear(), que no sobrescribe nada
//    ImpresoraRota.z 13:5   imprimirCopias(int) retorna int y no void
//    ImpresoraRota.z 18:5   estado() pasa de public a protected
//    errores.pig     23:1   una Impresora no cabe en una variable ImpresoraPDF
// ============================================================
import Impresora.z
import ImpresoraPDF.z
import ImpresoraRota.z

VARIABILES>
esto generica : novus Impresora();
esto pdf : novus ImpresoraPDF();

MAIOR>
generica = pdf;
generica.imprimir();
pdf = generica;
FINIS;
