package com.dmaldonado.st4ck.over.pig.model.ast.declaration;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import java.util.List;

/**
 * import carpeta.Objeto1.z
 *
 * The last segment is the extension; the ones before it are folders and the
 * file name, resolved against the folder of the importing .pig.
 */
public class ImportDeclaration extends AstNode
{
    private final List<String> segments;

    public ImportDeclaration(List<String> segments, int line, int column)
    {
        super(line, column);
        this.segments = segments;
    }

    public String getExtension()
    {
        return segments.get(segments.size() - 1);
    }

    /** carpeta/Objeto1.z */
    public String getRelativePath()
    {
        return String.join("/", segments.subList(0, segments.size() - 1)) + "." + getExtension();
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitImportDeclaration(this);
    }

    @Override
    public String getLabel()
    {
        return "import " + String.join(".", segments);
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
