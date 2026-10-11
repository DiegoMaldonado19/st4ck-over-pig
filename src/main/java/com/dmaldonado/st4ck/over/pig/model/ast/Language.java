package com.dmaldonado.st4ck.over.pig.model.ast;

import java.util.Set;

/**
 * Source language of a Program. The three languages share one AST; this is
 * what lets the semantic analyzer apply the few rules that differ, such as
 * where a variable may be declared.
 */
public enum Language
{
    PIG("pig", Set.of("numerus", "decimalis", "textum", "littera", "bool", "verum", "falsus")),
    Y("y", Set.of("entero", "flotante", "cadena", "caracter", "bool")),
    Z("z", Set.of("int", "double", "String", "char", "boolean"));

    private final String      extension;
    private final Set<String> typeNames;

    Language(String extension, Set<String> typeNames)
    {
        this.extension = extension;
        this.typeNames = typeNames;
    }

    /** Its own names for the primitive types: "int" names nothing in a .pig. */
    public boolean hasTypeName(String name)
    {
        return typeNames.contains(name);
    }

    /** @return null when the extension is not a language the compiler reads. */
    public static Language fromExtension(String extension)
    {
        for (Language language : values())
        {
            if (language.extension.equals(extension))
            {
                return language;
            }
        }
        return null;
    }
}
