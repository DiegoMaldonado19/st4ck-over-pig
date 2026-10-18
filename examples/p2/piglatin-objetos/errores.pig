// ============================================================
//  p2/piglatin-objetos/errores.pig - objetos en PigLatin (Fase 9),
//  lo prohibido. Errores esperados (4):
//    18:1  'mal': una Persona no es un Profesor (el tipo explicito no es
//          la clase del novus ni una clase padre suya)
//    19:1  'numero': null con un tipo primitivo
//    20:1  'texto': null con un tipo primitivo
//    24:8  'otro' es una Persona: sus metodos se buscan en Persona, que
//          no tiene getAlumno()
// ============================================================
import Persona.z
import Profesor.z

VARIABILES>
esto persona : novus Persona(30, "Cocinero") : Persona ;
esto otro : novus Profesor(50, persona, persona) : Persona ;
esto bien : null : Profesor ;
esto mal : novus Persona() : Profesor ;
esto numero : null : numerus ;
esto texto : null : textum ;

MAIOR>
bien = null;
>> otro.getAlumno().presentarse();
FINIS;
