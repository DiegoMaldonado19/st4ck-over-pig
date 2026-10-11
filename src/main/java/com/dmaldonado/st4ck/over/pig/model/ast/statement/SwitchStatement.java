package com.dmaldonado.st4ck.over.pig.model.ast.statement;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import java.util.ArrayList;
import java.util.List;

/**
 * elegir(opcion) { caso 1: ... romper  siempre: ... }
 *
 * The cases keep their source order because a case without romper falls
 * through into the next one, as in C.
 */
public class SwitchStatement extends AstNode
{
    private final Expression       discriminant;
    private final List<CaseClause> cases;

    public SwitchStatement(Expression discriminant, List<CaseClause> cases, int line, int column)
    {
        super(line, column);
        this.discriminant = discriminant;
        this.cases        = cases;
    }

    public Expression getDiscriminant()
    {
        return discriminant;
    }

    public List<CaseClause> getCases()
    {
        return cases;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitSwitchStatement(this);
    }

    @Override
    public String getLabel()
    {
        return "ELEGIR";
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>();
        if (discriminant != null)
        {
            children.add(discriminant);
        }
        children.addAll(cases);
        return children;
    }
}
