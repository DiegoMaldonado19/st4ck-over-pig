package com.dmaldonado.st4ck.over.pig.model.symbols;

import com.dmaldonado.st4ck.over.pig.model.types.DataType;

/**
 * Base entry of the symbol table.
 *
 * typeText is kept next to type because they are not the same thing: a variable
 * of type ESTRUCTURA needs to remember it was written "Persona", and error
 * messages have to echo back what the user typed.
 */
public abstract class Symbol
{
    private final String         name;
    private final DataType       type;
    private final String         typeText;
    private final SymbolCategory category;
    private final String         scopeName;
    private final int            line;
    private final int            column;

    /** Stack slot, assigned by SymbolTable.declare: absolute for a global, relative to P otherwise; -1 when it has none. */
    private int                  offset = -1;
    private boolean              global;

    protected Symbol(String name, DataType type, String typeText, SymbolCategory category,
                     String scopeName, int line, int column)
    {
        this.name      = name;
        this.type      = type;
        this.typeText  = typeText;
        this.category  = category;
        this.scopeName = scopeName;
        this.line      = line;
        this.column    = column;
    }

    public String getName()
    {
        return name;
    }

    public DataType getType()
    {
        return type;
    }

    public String getTypeText()
    {
        return typeText;
    }

    public SymbolCategory getCategory()
    {
        return category;
    }

    public String getScopeName()
    {
        return scopeName;
    }

    public int getLine()
    {
        return line;
    }

    public int getColumn()
    {
        return column;
    }

    /** Extra information shown in the "Detalle" column of the symbol table. */
    public String getDetail()
    {
        return "";
    }

    public int getOffset()
    {
        return offset;
    }

    public boolean isGlobal()
    {
        return global;
    }

    void setStorage(int offset, boolean global)
    {
        this.offset = offset;
        this.global = global;
    }

    /** stack[3] for a global, stack[P+3] for a local: the stack made visible in the table. */
    protected String describeStorage()
    {
        if (offset < 0)
        {
            return "";
        }
        return global ? ", stack[" + offset + "]" : ", stack[P+" + offset + "]";
    }
}
