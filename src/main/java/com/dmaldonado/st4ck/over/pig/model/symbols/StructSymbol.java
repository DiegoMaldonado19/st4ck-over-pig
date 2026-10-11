package com.dmaldonado.st4ck.over.pig.model.symbols;

import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A type declared with {@code structura}.
 *
 * The attributes live in a LinkedHashMap, not a list, and that is the whole
 * point: the statement says the order of the attributes does not matter when a
 * variable is created, so validation has to be by name. Insertion order is kept
 * only so the symbol table graph lists them the way they were written.
 */
public class StructSymbol extends Symbol
{
    private final Map<String, VariableSymbol> attributes = new LinkedHashMap<>();

    public StructSymbol(String name, String scopeName, boolean classType, int line, int column)
    {
        super(name, DataType.ESTRUCTURA, name, classType ? SymbolCategory.CLASS : SymbolCategory.STRUCT,
                scopeName, line, column);
    }

    /**
     * A Zetariano class: its instances start as null and are created with new,
     * while a structure variable gets its block as soon as it is declared.
     */
    public boolean isClass()
    {
        return getCategory() == SymbolCategory.CLASS;
    }

    /** @return false if the attribute name was already used in this structura. */
    public boolean addAttribute(VariableSymbol attribute)
    {
        if (attributes.containsKey(attribute.getName()))
        {
            return false;
        }
        attributes.put(attribute.getName(), attribute);
        return true;
    }

    public VariableSymbol getAttribute(String name)
    {
        return attributes.get(name);
    }

    public boolean hasAttribute(String name)
    {
        return attributes.containsKey(name);
    }

    public Map<String, VariableSymbol> getAttributes()
    {
        return attributes;
    }

    @Override
    public String getDetail()
    {
        return attributes.size() + " atributo(s): " + String.join(", ", attributes.keySet());
    }
}
