package com.dmaldonado.st4ck.over.pig.model.symbols;

/**
 * Encapsulation of a Zetariano attribute, method or constructor, as written:
 * DEFAULT is no modifier at all. There are no packages (auxiliar, 15-oct), and
 * DEFAULT works like PRIVATE (auxiliar, 22-oct and 24-oct): both stay inside
 * their class. PROTECTED reaches its subclasses too, and PUBLIC everywhere.
 */
public enum Visibility
{
    PUBLIC,
    PROTECTED,
    DEFAULT,
    PRIVATE;

    /** Only its own class uses it, so no subclass inherits or overrides it. */
    public boolean isClassOnly()
    {
        return this == PRIVATE || this == DEFAULT;
    }
}
