// ============================================================
//  p2/herencia/main.pig - herencia (Fase 8), lo valido.
//  Animal.z y Perro.z son las clases del enunciado: perro.comer()
//  imprime con el 'nombre' heredado. Gato.z llama a super(...) y
//  Cachorro.z hereda en tres niveles y usa super.describir().
//  'mascota' es un Animal que guarda un Cachorro: describir() responde
//  la version de Cachorro. Lo prohibido esta en errores.pig.
//
//  Salida esperada:
//    Nace Michi con 7 vidas.
//    Firulais está comiendo.
//    Firulais está ladrando: ¡Guau, guau!
//    Firulais (energía 15)
//    Michi dice: miau.
//    Michi está comiendo.
//    Michi está comiendo.
//    Michi (energía 20)
//    Bobby está ladrando: ¡Guau, guau!
//    Bobby está comiendo.
//    Bobby (energía 15), 3 meses
//    Toby Jr. (energía 10), 2 meses
//    Bobby (energía 15), 3 meses
// ============================================================
import Animal.z
import Perro.z
import Gato.z
import Cachorro.z

VARIABILES>
esto perro : novus Perro();
esto gato : novus Gato("Michi");
esto cachorro : novus Cachorro("Bobby", 3);
esto otro : novus Cachorro("Toby");
esto mascota : Animal = cachorro;

MAIOR>
perro.nombre = "Firulais";
perro.comer();
perro.ladrar();
>> perro.describir();
gato.maullar();
gato.comer();
gato.comer();
>> gato.describir();
cachorro.ladrar();
cachorro.comer();
>> cachorro.describir();
>> otro.describir();
>> mascota.describir();
FINIS;
