package com.dmaldonado.st4ck.over.pig.model.analysis;

import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.BinaryExpression;
import java.util.function.Function;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Lo que comparten los AST builders de todos los lenguajes: posiciones, la
 * guarda contra un arbol que el parser tuvo que reparar, y el plegado de las
 * reglas de precedencia.
 */
final class ParseTreeSupport
{
    private ParseTreeSupport()
    {
    }

    static int line(ParserRuleContext ctx)
    {
        Token token = ctx.getStart();
        return token == null ? 0 : token.getLine();
    }

    static int column(ParserRuleContext ctx)
    {
        Token token = ctx.getStart();
        return token == null ? 0 : token.getCharPositionInLine() + 1;
    }

    /**
     * The pipeline now builds the AST even after a syntax error, so a child
     * the builder needs may be absent: null because the parser gave up before
     * reaching it, or an ErrorNode such as the "<missing ID>" it conjured.
     */
    static boolean missing(ParseTree... nodes)
    {
        for (ParseTree node : nodes)
        {
            if (node == null || node instanceof ErrorNode)
            {
                return true;
            }
        }
        return false;
    }

    /** The value of a size written as a literal; -1 when it is not one, or does not fit an int. */
    static int constant(ParseTree node)
    {
        try
        {
            return Integer.parseInt(node.getText());
        }
        catch (NumberFormatException notConstant)
        {
            return -1;
        }
    }

    /**
     * Every precedence rule has the shape  A : B (op B)* , so one method folds
     * them all, LEFT associative:  a - b - c => ((a - b) - c).
     * With a single operand the loop does not run and the child passes intact.
     * Null when an operand did not build.
     */
    static Expression fold(ParserRuleContext ctx, Function<ParseTree, Expression> expression)
    {
        Expression left = expression.apply(ctx.getChild(0));

        for (int i = 1; i + 1 < ctx.getChildCount() && left != null; i += 2)
        {
            Expression right = expression.apply(ctx.getChild(i + 1));

            left = right == null ? null
                    : new BinaryExpression(left, ctx.getChild(i).getText(), right, line(ctx), column(ctx));
        }
        return left;
    }
}
