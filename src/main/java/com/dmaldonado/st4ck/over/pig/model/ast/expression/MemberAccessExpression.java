package com.dmaldonado.st4ck.over.pig.model.ast.expression;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.List;

/** lider.nombre, mi_selva.animales -- chainable: variable.prop.prop. */
public class MemberAccessExpression extends Expression
{
    private final Expression owner;
    private final String     memberName;

    public MemberAccessExpression(Expression owner, String memberName, int line, int column)
    {
        super(line, column);
        this.owner      = owner;
        this.memberName = memberName;
    }

    public Expression getOwner()
    {
        return owner;
    }

    public String getMemberName()
    {
        return memberName;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitMemberAccessExpression(this);
    }

    @Override
    public String getLabel()
    {
        return "." + memberName;
    }

    @Override
    public List<AstNode> getChildren()
    {
        return owner == null ? List.of() : List.of(owner);
    }
}
