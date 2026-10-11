package com.dmaldonado.st4ck.over.pig.model.analysis;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.LiteralExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.Block;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.BreakStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.CaseClause;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ContinueStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.DoWhileStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ForStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.IfStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ReturnStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.SwitchStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.WhileStatement;
import java.util.List;

/**
 * Checks the two rules the statement demands of a ratio function: "Que se
 * retorne un valor en todos los caminos posibles y que no quede codigo que no
 * se puede rastrear".
 *
 * Static and conservative: it never executes anything, it only looks at the
 * SHAPE of the AST. When in doubt it answers "does not always return", which
 * makes it report a possible problem rather than stay silent about a real one.
 * The one exception is a loop on a literal verum: it can only be left through
 * a reddere or its own interrumpe.
 *
 * The aliter chain needs no special case here: phase 1 desugars it into nested
 * IfStatement nodes, so the elseBranch of an if is simply another if and the
 * recursion already covers it.
 */
public final class ReturnPathChecker
{
    private ReturnPathChecker()
    {
    }

    /** Does this node guarantee a reddere on EVERY execution path? */
    public static boolean alwaysReturns(AstNode node)
    {
        return !completesNormally(node);
    }

    /**
     * Java's rule: can execution run past the end of this node? A reddere,
     * interrumpe or perge cannot; a loop whose condition is literally verum
     * (or missing) only can through an interrumpe of its own; a switch falls
     * through its cases, so without a break it ends where its last case ends.
     */
    private static boolean completesNormally(AstNode node)
    {
        if (node instanceof ReturnStatement || node instanceof BreakStatement
                || node instanceof ContinueStatement)
        {
            return false;
        }
        if (node instanceof Block block)
        {
            return block.getStatements().stream().allMatch(ReturnPathChecker::completesNormally);
        }
        if (node instanceof IfStatement ifStatement)
        {
            return ifStatement.getElseBranch() == null
                || completesNormally(ifStatement.getThenBranch())
                || completesNormally(ifStatement.getElseBranch());
        }
        if (node instanceof WhileStatement whileStatement)
        {
            return !isAlwaysTrue(whileStatement.getCondition()) || breaksOut(whileStatement.getBody());
        }
        if (node instanceof ForStatement forStatement)
        {
            return forStatement.getCondition() != null && !isAlwaysTrue(forStatement.getCondition())
                || breaksOut(forStatement.getBody());
        }
        if (node instanceof DoWhileStatement doWhile)
        {
            // perge jumps to the condition, which may be false.
            return !isAlwaysTrue(doWhile.getCondition())
                    && (completesNormally(doWhile.getBody()) || continues(doWhile.getBody()))
                || breaksOut(doWhile.getBody());
        }
        if (node instanceof SwitchStatement switchStatement)
        {
            List<CaseClause> cases = switchStatement.getCases();

            // Without siempre a value may match no case at all.
            return cases.stream().noneMatch(CaseClause::isDefault)
                || cases.stream().anyMatch(clause -> breaksOut(clause.getBody()))
                || completesNormally(cases.get(cases.size() - 1).getBody());
        }
        return true;
    }

    private static boolean isAlwaysTrue(AstNode condition)
    {
        return condition instanceof LiteralExpression literal && literal.isTrue();
    }

    /** An interrumpe that leaves THIS loop or switch: one inside a nested loop or switch leaves that one. */
    private static boolean breaksOut(AstNode node)
    {
        return node instanceof BreakStatement
            || node != null && !isLoop(node) && !(node instanceof SwitchStatement)
               && reachableChildren(node).stream().anyMatch(ReturnPathChecker::breaksOut);
    }

    /** A perge of THIS loop: a switch does not catch it, a nested loop does. */
    private static boolean continues(AstNode node)
    {
        return node instanceof ContinueStatement
            || node != null && !isLoop(node) && reachableChildren(node).stream().anyMatch(ReturnPathChecker::continues);
    }

    /** "reddere 1; interrumpe;": what follows a statement that cannot complete normally never runs. */
    private static List<AstNode> reachableChildren(AstNode node)
    {
        if (!(node instanceof Block block))
        {
            return node.getChildren();
        }

        List<AstNode> statements = block.getStatements();

        for (int i = 0; i < statements.size(); i++)
        {
            if (!completesNormally(statements.get(i)))
            {
                return statements.subList(0, i + 1);
            }
        }
        return statements;
    }

    private static boolean isLoop(AstNode node)
    {
        return node instanceof WhileStatement || node instanceof DoWhileStatement || node instanceof ForStatement;
    }

    /**
     * Index of the first unreachable statement inside a block, the one right
     * after a statement that cannot complete normally (a reddere, a si whose
     * branches all return, a dum (verum) without interrumpe), or -1 when there
     * is none.
     */
    public static int firstUnreachableIndex(Block block)
    {
        List<AstNode> statements = block.getStatements();

        for (int i = 0; i < statements.size() - 1; i++)
        {
            if (!completesNormally(statements.get(i)))
            {
                return i + 1;
            }
        }
        return -1;
    }
}
