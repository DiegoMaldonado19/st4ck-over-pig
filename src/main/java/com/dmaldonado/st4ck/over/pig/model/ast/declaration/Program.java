package com.dmaldonado.st4ck.over.pig.model.ast.declaration;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Language;
import java.util.ArrayList;
import java.util.List;

/**
 * Root of the AST: imports, VARIABILES, MUNERA and MAIOR. A .y file only fills
 * globals (its structures) and functions.
 */
public class Program extends AstNode
{
    private final Language                  language;
    private final List<ImportDeclaration>   imports;
    private final List<AstNode>             globals;
    private final List<FunctionDeclaration> functions;
    private final List<AstNode>             mainStatements;

    public Program(Language language, List<ImportDeclaration> imports, List<AstNode> globals,
                   List<FunctionDeclaration> functions, List<AstNode> mainStatements,
                   int line, int column)
    {
        super(line, column);
        this.language       = language;
        this.imports        = imports;
        this.globals        = globals;
        this.functions      = functions;
        this.mainStatements = mainStatements;
    }

    public Language getLanguage()
    {
        return language;
    }

    public List<ImportDeclaration> getImports()
    {
        return imports;
    }

    public List<AstNode> getGlobals()
    {
        return globals;
    }

    public List<FunctionDeclaration> getFunctions()
    {
        return functions;
    }

    public List<AstNode> getMainStatements()
    {
        return mainStatements;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitProgram(this);
    }

    @Override
    public String getLabel()
    {
        return "PROGRAMA";
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>(imports);
        children.addAll(globals);
        children.addAll(functions);
        children.addAll(mainStatements);
        return children;
    }
}
