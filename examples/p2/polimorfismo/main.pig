// ============================================================
//  p2/polimorfismo/main.pig - polimorfismo (Fase 8), lo valido.
//  Las tres impresoras del enunciado en un arreglo de Impresora: cada
//  imprimir() sale de la clase del objeto, no de la del arreglo. La
//  portatil no sobrescribe imprimir(), asi que usa la de ImpresoraPDF;
//  imprimirCopias() llama a this.imprimir() y tambien despacha.
//  Lo prohibido esta en errores.pig.
//
//  Salida esperada:
//    Imprimiendo documento genérico...
//    Imprimiendo en formato .PDF optimizado...
//    Imprimiendo hojas de cálculo en formato tabular...
//    Imprimiendo en formato .PDF optimizado...
//    Impresora 0: lista
//    Impresora 1: lista
//    Impresora 2: lista
//    Impresora 3: con batería al 80%
//    Copia 1: Imprimiendo en formato .PDF optimizado...
//    Copia 2: Imprimiendo en formato .PDF optimizado...
//    Copia 1: Imprimiendo hojas de cálculo en formato tabular...
// ============================================================
import Impresora.z
import ImpresoraPDF.z
import ImpresoraExcel.z
import ImpresoraPortatil.z

VARIABILES>
series impresoras[4] : Impresora;
esto pdf : novus ImpresoraPDF();

MAIOR>
impresoras[0] = novus Impresora();
impresoras[1] = pdf;
impresoras[2] = novus ImpresoraExcel();
impresoras[3] = novus ImpresoraPortatil();
per (esto i : numerus 0; i < 4; i++) {
    impresoras[i].imprimir();
}
per (esto i : numerus 0; i < 4; i++) {
    >> "Impresora " >> i >> ": " >> impresoras[i].estado();
}
pdf.imprimirCopias(2);
impresoras[2].imprimirCopias(1);
FINIS;
