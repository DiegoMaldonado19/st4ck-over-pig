package com.dmaldonado.st4ck.over.pig.model.ast.declaration;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import java.util.ArrayList;
import java.util.List;

/**
 * public class Persona { ... } -- a Zetariano class.
 *
 * An object lives in the heap with the same layout as a structure, so the
 * attributes are kept as one: field access, identity and passing by reference
 * reuse what structures already do. Constructors and methods are functions
 * whose owner is this class; they receive the object in the first slot.
 *
 * privateMembers keeps what was written 'private': encapsulation belongs to
 * Proyecto 2, so today the analyzer only reports it.
 */
public class ClassDeclaration extends AstNode
{
    private final String                    name;
    private final StructDeclaration         layout;
    private final List<FunctionDeclaration> methods;
    private final List<AstNode>             privateMembers;

    public ClassDeclaration(String name, StructDeclaration layout, List<FunctionDeclaration> methods,
                            List<AstNode> privateMembers, int line, int column)
    {
        super(line, column);
        this.name           = name;
        this.layout         = layout;
        this.methods        = methods;
        this.privateMembers = privateMembers;
    }

    public String getName()
    {
        return name;
    }

    /** The attributes, in declaration order: an attribute's index is its offset in the object. */
    public StructDeclaration getLayout()
    {
        return layout;
    }

    /** Constructors and methods; a class declared without constructors has an empty one. */
    public List<FunctionDeclaration> getMethods()
    {
        return methods;
    }

    public List<AstNode> getPrivateMembers()
    {
        return privateMembers;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitClassDeclaration(this);
    }

    @Override
    public String getLabel()
    {
        return "class " + name;
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>(layout.getFields());
        children.addAll(methods);
        return children;
    }
}
