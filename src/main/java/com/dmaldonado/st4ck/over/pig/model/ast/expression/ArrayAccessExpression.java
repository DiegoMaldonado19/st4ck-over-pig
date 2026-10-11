package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.ArrayList;
import java.util.List;

/**
 * nombres[0], mi_selva.animales[1].
 *
 * The array is an Expression and not a plain name so accesses can chain:
 * arr[i].prop[j].
 */
public class ArrayAccessExpression extends Expression
{
    private final Expression array;
    private final Expression index;

    public ArrayAccessExpression(Expression array, Expression index, int line, int column)
    {
        super(line, column);
        this.array = array;
        this.index = index;
    }

    public Expression getArray()
    {
        return array;
    }

    public Expression getIndex()
    {
        return index;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitArrayAccessExpression(this);
    }

    @Override
    public String getLabel()
    {
        return "INDICE []";
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>();
        if (array != null)
        {
            children.add(array);
        }
        if (index != null)
        {
            children.add(index);
        }
        return children;
    }
}
