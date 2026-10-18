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
 */
public class ClassDeclaration extends AstNode
{
    private final String                    name;
    /** The class it extends; null without extends. */
    private final String                    parentName;
    private final StructDeclaration         layout;
    private final List<FunctionDeclaration> methods;

    public ClassDeclaration(String name, String parentName, StructDeclaration layout,
                            List<FunctionDeclaration> methods, int line, int column)
    {
        super(line, column);
        this.name       = name;
        this.parentName = parentName;
        this.layout     = layout;
        this.methods    = methods;
    }

    public String getName()
    {
        return name;
    }

    public String getParentName()
    {
        return parentName;
    }

    /** Its own attributes, in declaration order; in the object they follow those of its parents. */
    public StructDeclaration getLayout()
    {
        return layout;
    }

    /** Constructors and methods; a class declared without constructors has an empty one. */
    public List<FunctionDeclaration> getMethods()
    {
        return methods;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitClassDeclaration(this);
    }

    @Override
    public String getLabel()
    {
        return "class " + name + (parentName == null ? "" : " extends " + parentName);
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>(layout.getFields());
        children.addAll(methods);
        return children;
    }
}
