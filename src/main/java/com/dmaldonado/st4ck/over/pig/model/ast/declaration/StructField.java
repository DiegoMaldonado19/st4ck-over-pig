package com.dmaldonado.st4ck.over.pig.model.ast.declaration;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.List;

/**
 * One attribute inside a structura.
 *
 * A PigLatin array field carries no size (series animales : Animal;) because
 * the dimension is only given when the variable is declared. A Y? array field
 * does (entero miArray[10]), and the size is constant by the statement.
 */
public class StructField extends AstNode
{
    private final String   name;
    private final String   typeText;
    private final DataType type;
    private final boolean  array;
    /** -1 when the field is not an array or its size is given later. */
    private final int      size;
    /** Dimensions: 0 for a value, 1 for an array; Zetariano can write more (int[][] m). */
    private final int      rank;

    public StructField(String name, String typeText, boolean array, int size, int line, int column)
    {
        this(name, typeText, array ? 1 : 0, size, line, column);
    }

    public StructField(String name, String typeText, int rank, int size, int line, int column)
    {
        super(line, column);
        this.name     = name;
        this.typeText = typeText;
        this.type     = DataType.fromText(typeText);
        this.array    = rank > 0;
        this.size     = size;
        this.rank     = rank;
    }

    public int getSize()
    {
        return size;
    }

    public int getRank()
    {
        return rank;
    }

    public String getName()
    {
        return name;
    }

    public String getTypeText()
    {
        return typeText;
    }

    public DataType getType()
    {
        return type;
    }

    public boolean isArray()
    {
        return array;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitStructField(this);
    }

    @Override
    public String getLabel()
    {
        return (array ? "series " : "esto ") + name + " : " + typeText;
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
