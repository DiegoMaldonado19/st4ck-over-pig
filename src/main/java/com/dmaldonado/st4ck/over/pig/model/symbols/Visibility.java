package com.dmaldonado.st4ck.over.pig.model.symbols;

/**
 * Encapsulation of a Zetariano attribute, method or constructor, as written:
 * DEFAULT is no modifier at all. There are no packages (auxiliar, 15-oct), so
 * DEFAULT reaches as far as PUBLIC, while PRIVATE and PROTECTED stay inside
 * their class; the subclasses PROTECTED also reaches come with Fase 8.
 */
public enum Visibility
{
    PUBLIC,
    PROTECTED,
    DEFAULT,
    PRIVATE
}
