package com.dmaldonado.st4ck.over.pig.model.symbols;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One scope, linked to its parent. The parent chain IS the nesting: there is no
 * separate stack object, a scope simply knows which one contains it.
 */
public class Scope
{
    private final String              name;
    private final Scope               parent;
    private final Map<String, Symbol> symbols = new LinkedHashMap<>();

    public Scope(String name, Scope parent)
    {
        this.name   = name;
        this.parent = parent;
    }

    public String getName()
    {
        return name;
    }

    public Scope getParent()
    {
        return parent;
    }

    /** Declares in THIS scope. @return false if the name already existed. */
    public boolean declare(Symbol symbol)
    {
        if (symbols.containsKey(symbol.getName()))
        {
            return false;
        }
        symbols.put(symbol.getName(), symbol);
        return true;
    }

    public Symbol lookupLocal(String name)
    {
        return symbols.get(name);
    }

    /** Looks here first, then walks up the parent chain. */
    public Symbol lookup(String name)
    {
        Symbol found = symbols.get(name);

        if (found != null)
        {
            return found;
        }
        return (parent == null) ? null : parent.lookup(name);
    }
}
