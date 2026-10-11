package com.dmaldonado.st4ck.over.pig.view;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import javafx.scene.control.TreeItem;

/**
 * Turns any AST into a JavaFX tree without knowing a single concrete node type:
 * getLabel() and getChildren() of AstNode are the whole contract.
 *
 * The item keeps the node itself, not its label, so the cell can show where
 * it is written and a double click can take the editor there. Only the first
 * two levels start expanded: a medium program has hundreds of nodes.
 */
public final class AstTreeBuilder
{
    private static final int EXPANDED_LEVELS = 2;

    private AstTreeBuilder()
    {
    }

    public static TreeItem<AstNode> build(AstNode node)
    {
        return build(node, 0);
    }

    private static TreeItem<AstNode> build(AstNode node, int depth)
    {
        TreeItem<AstNode> item = new TreeItem<>(node);

        for (AstNode child : node.getChildren())
        {
            item.getChildren().add(build(child, depth + 1));
        }
        item.setExpanded(depth < EXPANDED_LEVELS);
        return item;
    }

    /** The same tree as indented text, one "| label" per line, ready for the technical manual. */
    public static String toText(AstNode node)
    {
        StringBuilder text = new StringBuilder();

        write(node, 0, text);
        return text.toString();
    }

    private static void write(AstNode node, int depth, StringBuilder text)
    {
        text.append("\t".repeat(depth)).append("| ").append(node.getLabel()).append('\n');

        for (AstNode child : node.getChildren())
        {
            write(child, depth + 1, text);
        }
    }
}
