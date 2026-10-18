// ============================================================
//  p2/piglatin-objetos/main.pig - objetos en PigLatin (Fase 9).
//  Las tres declaraciones del enunciado: 'otroObjeto' se toma como
//  Persona, que es su tipo explicito, aunque guarda un Profesor, y su
//  presentarse() responde la version de Profesor. 'nadie' empieza null.
//  Lo prohibido esta en errores.pig.
//
//  Salida esperada:
//    Persona de 12 años: Profesor
//    Profesor de 12 años, colega de: Persona de 12 años: Profesor
//    nadie es null
//    ahora nadie es: Profesor de 45 años, colega de: Profesor de 12 años, colega de: Persona de 12 años: Profesor
//    su alumno: Persona de 12 años: Profesor
//    nadie vuelve a ser null
// ============================================================
import Persona.z
import Profesor.z

VARIABILES>
esto miObjeto : novus Persona(12, "Profesor") : Persona ;
// la clase se toma en su forma Persona, porque es el tipo explicito
esto otroObjeto : novus Profesor(12, miObjeto, novus Persona()) : Persona ;
// Ahora las clases pueden ser nulas
esto nadie : null : Persona ;
// Sin tipo explicito, el tipo es la clase del novus
esto profe : novus Profesor(45, otroObjeto, miObjeto);

MAIOR>
>> miObjeto.presentarse();
>> otroObjeto.presentarse();
si (nadie == null) {
    >> "nadie es null";
} finis;
nadie = profe;
si (nadie != null) {
    >> "ahora nadie es: " >> nadie.presentarse();
} finis;
>> "su alumno: " >> profe.getAlumno().presentarse();
nadie = null;
si (nadie == null) {
    >> "nadie vuelve a ser null";
} finis;
FINIS;
