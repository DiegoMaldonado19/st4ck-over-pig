package com.dmaldonado.st4ck.over.pig.model.codegen;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.AstVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import com.dmaldonado.st4ck.over.pig.model.ast.Language;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ArrayDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ClassDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.FunctionDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ImportDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.Parameter;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.Program;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.StructDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.StructField;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.VariableDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.ArrayAccessExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.BinaryExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.CompositeLiteralExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.FunctionCallExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.IdentifierExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.IncrementExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.LiteralExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.MemberAccessExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.MethodCallExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.NewArrayExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.NewExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.ReadExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.TernaryExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.ThisExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.UnaryExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.Assignment;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.Block;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.BreakStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.CaseClause;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ContinueStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.DoWhileStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ExpressionStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ForStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.IfStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.IncrementStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.InputStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.PrintStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ReturnStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.SwitchStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.WhileStatement;
import com.dmaldonado.st4ck.over.pig.model.symbols.Symbol;
import com.dmaldonado.st4ck.over.pig.model.symbols.SymbolCategory;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The three address code as it is written by hand: the variables by their
 * own name, temporaries t1, t2... and labels L1, L2... with one counter each
 * for the whole program, and every condition turned into jumps to its false
 * branch, evaluated short circuit. Each visitX emits its quadruples and
 * returns the OPERAND that holds its value: a variable, a temporary or a
 * literal. Statements return null.
 *
 * It grows phase by phase. What it does not translate yet throws
 * UnsupportedOperationException, and the pipeline shows that instead.
 */
public class ThreeAddressGenerator implements AstVisitor<String>
{
    private static final String              HEAP         = "heap";
    private static final String              PRINT_STRING = "__print_str";
    /** H is the heap pointer and heap the memory; tN and LN would read as temporaries and labels. */
    private static final Pattern             RESERVED     = Pattern.compile("H|heap|[tL]\\d+");
    private static final Pattern             INTEGER      = Pattern.compile("-?\\d+");
    /** The comparison that jumps when the written one is false: while (i < 10) is if i >= 10 goto end. */
    private static final Map<String, String> NEGATED      = Map.of("<", ">=", ">", "<=", "<=", ">", ">=", "<",
                                                                   "==", "!=", "!=", "==");

    private final List<Quadruple>     code           = new ArrayList<>();
    /** The name each variable has in the code: its own, unless another variable already has it. */
    private final Map<Symbol, String> names          = new HashMap<>();
    private final Set<String>         globalNames    = new HashSet<>();
    private final Set<String>         localNames     = new HashSet<>();
    /** The helper functions the program used: they go after main. */
    private final Set<String>         helpers        = new LinkedHashSet<>();
    private final Deque<String>       breakLabels    = new ArrayDeque<>();
    private final Deque<String>       continueLabels = new ArrayDeque<>();

    private int      temporaryCount;
    private int      labelCount;
    /** Each language prints its booleans with its own words. */
    private Language language;

    /** The .pig with MAIOR: its globals, then main, then the helpers it used. */
    public List<Quadruple> generate(Program root)
    {
        language = root.getLanguage();

        for (AstNode global : root.getGlobals())
        {
            Symbol symbol = global instanceof VariableDeclaration variable ? variable.getSymbol()
                          : global instanceof ArrayDeclaration array ? array.getSymbol() : null;

            if (symbol != null)
            {
                emit("global", "", "", name(symbol));
            }
        }

        emit("func", "", "", "main");
        for (AstNode global : root.getGlobals())
        {
            global.accept(this);
        }
        for (AstNode statement : root.getMainStatements())
        {
            statement.accept(this);
        }
        emit("halt", "", "", "");
        emit("end_func", "", "", "main");

        if (helpers.contains(PRINT_STRING))
        {
            printStringFunction();
        }
        return code;
    }

    /* =================================================================
     * DECLARATIONS
     * ================================================================= */
    @Override
    public String visitProgram(Program node)
    {
        throw new IllegalStateException("A program is generated through generate().");
    }

    @Override
    public String visitImportDeclaration(ImportDeclaration node)
    {
        return null;
    }

    /** Without a value a variable still gets one: the slot of a local keeps what a previous call left. */
    @Override
    public String visitVariableDeclaration(VariableDeclaration node)
    {
        if (node.getType() == DataType.ESTRUCTURA)
        {
            throw pending(5, "estructuras y objetos");
        }

        Expression value = node.getInitialValue();

        emit("=", value == null ? "0" : operand(value, node.getType()), "", name(node.getSymbol()));
        return null;
    }

    @Override
    public String visitArrayDeclaration(ArrayDeclaration node)
    {
        throw pending(5, "arreglos");
    }

    @Override
    public String visitStructDeclaration(StructDeclaration node)
    {
        throw pending(5, "estructuras");
    }

    @Override
    public String visitStructField(StructField node)
    {
        return null;
    }

    @Override
    public String visitClassDeclaration(ClassDeclaration node)
    {
        throw pending(5, "clases");
    }

    @Override
    public String visitFunctionDeclaration(FunctionDeclaration node)
    {
        throw pending(4, "funciones");
    }

    @Override
    public String visitParameter(Parameter node)
    {
        return null;
    }

    /* =================================================================
     * STATEMENTS
     * ================================================================= */
    @Override
    public String visitBlock(Block node)
    {
        for (AstNode statement : node.getStatements())
        {
            statement.accept(this);
        }
        return null;
    }

    @Override
    public String visitAssignment(Assignment node)
    {
        String target = variable(node.getTarget());

        emit("=", operand(node.getValue(), node.getTarget().getComputedType()), "", target);
        return null;
    }

    /** si / aliter (c) / aliter: one exit label for the whole chain, one false label per condition. */
    @Override
    public String visitIfStatement(IfStatement node)
    {
        String end = newLabel();

        ifChain(node, end);
        label(end);
        return null;
    }

    private void ifChain(IfStatement node, String end)
    {
        if (node.getElseBranch() == null)
        {
            jumpIfFalse(node.getCondition(), end);
            node.getThenBranch().accept(this);
            return;
        }

        String otherwise = newLabel();

        jumpIfFalse(node.getCondition(), otherwise);
        node.getThenBranch().accept(this);
        jump(end);
        label(otherwise);

        if (node.getElseBranch() instanceof IfStatement elseIf)
        {
            ifChain(elseIf, end);
        }
        else
        {
            node.getElseBranch().accept(this);
        }
    }

    @Override
    public String visitWhileStatement(WhileStatement node)
    {
        String start = newLabel();
        String end   = newLabel();

        label(start);
        jumpIfFalse(node.getCondition(), end);
        loopBody(node.getBody(), end, start);
        jump(start);
        label(end);
        return null;
    }

    @Override
    public String visitDoWhileStatement(DoWhileStatement node)
    {
        String start = newLabel();
        String test  = newLabel();
        String end   = newLabel();

        label(start);
        loopBody(node.getBody(), end, test);
        label(test);
        jumpIfTrue(node.getCondition(), start);
        label(end);
        return null;
    }

    /** perge jumps to the update, not to the condition: skipping it would loop forever. */
    @Override
    public String visitForStatement(ForStatement node)
    {
        // Zetariano's for ( ; ; ) may leave out any of the three parts.
        if (node.getInitialization() != null)
        {
            node.getInitialization().accept(this);
        }

        String start  = newLabel();
        String update = newLabel();
        String end    = newLabel();

        label(start);
        if (node.getCondition() != null)
        {
            jumpIfFalse(node.getCondition(), end);
        }
        loopBody(node.getBody(), end, update);
        label(update);
        if (node.getUpdate() != null)
        {
            node.getUpdate().accept(this);
        }
        jump(start);
        label(end);
        return null;
    }

    @Override
    public String visitSwitchStatement(SwitchStatement node)
    {
        throw pending(5, "elegir / switch");
    }

    @Override
    public String visitCaseClause(CaseClause node)
    {
        return node.getBody().accept(this);
    }

    @Override
    public String visitReturnStatement(ReturnStatement node)
    {
        throw pending(4, "retorno de funciones");
    }

    @Override
    public String visitBreakStatement(BreakStatement node)
    {
        jump(breakLabels.peek());
        return null;
    }

    @Override
    public String visitContinueStatement(ContinueStatement node)
    {
        jump(continueLabels.peek());
        return null;
    }

    /** Each value by its type, then the line break: >> a >> b prints "ab\n". Zetariano's print has none. */
    @Override
    public String visitPrintStatement(PrintStatement node)
    {
        for (Expression value : node.getValues())
        {
            print(value);
        }
        if (node.endsLine())
        {
            emit("print_char", "10", "", "");
        }
        return null;
    }

    private void print(Expression value)
    {
        switch (value.getComputedType())
        {
            case NUMERUS  -> emit("print_int", value.accept(this), "", "");
            case LITTERA  -> emit("print_char", value.accept(this), "", "");
            case BOOLEANO -> printString(booleanText(value.accept(this)));
            case TEXTUM   -> printString(value.accept(this));
            default       -> throw pending(4, "impresion de decimales");
        }
    }

    @Override
    public String visitInputStatement(InputStatement node)
    {
        throw pending(4, "lectura de consola");
    }

    /** A call or a read alone drops its value; x++ alone is just the increment, without a copy of x. */
    @Override
    public String visitExpressionStatement(ExpressionStatement node)
    {
        if (node.getExpression() instanceof IncrementExpression increment)
        {
            increment(increment.getTarget(), increment.getOperator());
            return null;
        }
        node.getExpression().accept(this);
        return null;
    }

    @Override
    public String visitIncrementStatement(IncrementStatement node)
    {
        increment(node.getTarget(), node.getOperator());
        return null;
    }

    /* =================================================================
     * EXPRESSIONS
     * ================================================================= */
    @Override
    public String visitBinaryExpression(BinaryExpression node)
    {
        String operator = node.getOperator();

        if (NEGATED.containsKey(operator) || "&&".equals(operator) || "||".equals(operator))
        {
            return booleanValue(node);
        }
        if (node.getComputedType() == DataType.TEXTUM)
        {
            throw pending(4, "concatenacion de cadenas");
        }

        DataType type   = node.getComputedType();
        String   left   = operand(node.getLeft(), type);
        String   right  = operand(node.getRight(), type);
        String   result = newTemporary();

        emit(type == DataType.DECIMALIS ? operator + "f" : operator, left, right, result);
        return result;
    }

    /** -x is uminus x, and -1 is just the constant; non / ! is a condition as a value. */
    @Override
    public String visitUnaryExpression(UnaryExpression node)
    {
        if (!"-".equals(node.getOperator()))
        {
            return booleanValue(node);
        }
        if (node.getOperand() instanceof LiteralExpression literal
                && (literal.getType() == DataType.NUMERUS || literal.getType() == DataType.DECIMALIS))
        {
            return "-" + literal.getText();
        }

        String value  = node.getOperand().accept(this);
        String result = newTemporary();

        emit(node.getComputedType() == DataType.DECIMALIS ? "uminusf" : "uminus", value, "", result);
        return result;
    }

    /** ++x is the new value; x++ the old one, copied before x changes. */
    @Override
    public String visitIncrementExpression(IncrementExpression node)
    {
        if (node.isPrefix())
        {
            return increment(node.getTarget(), node.getOperator());
        }

        String before = newTemporary();

        emit("=", variable(node.getTarget()), "", before);
        increment(node.getTarget(), node.getOperator());
        return before;
    }

    /** x++ is x = x + 1 through a temporary, as written by hand: t1 = x + 1, x = t1. Returns the new value. */
    private String increment(Expression target, String operator)
    {
        String  variable = variable(target);
        boolean floating = target.getComputedType() == DataType.DECIMALIS;
        String  result   = newTemporary();

        emit(("++".equals(operator) ? "+" : "-") + (floating ? "f" : ""), variable, floating ? "1.0" : "1", result);
        emit("=", result, "", variable);
        return result;
    }

    /** Literals go into the code as written: 5, 2.5, 'a', "texto". A boolean is 1 or 0, and null is 0. */
    @Override
    public String visitLiteralExpression(LiteralExpression node)
    {
        return switch (node.getType())
        {
            case BOOLEANO -> node.isTrue() ? "1" : "0";
            case NULO     -> "0";
            default       -> node.getText();
        };
    }

    @Override
    public String visitIdentifierExpression(IdentifierExpression node)
    {
        if (node.getSymbol().getCategory() == SymbolCategory.ATTRIBUTE)
        {
            throw pending(5, "atributos");
        }
        return name(node.getSymbol());
    }

    @Override
    public String visitArrayAccessExpression(ArrayAccessExpression node)
    {
        throw pending(5, "arreglos");
    }

    @Override
    public String visitMemberAccessExpression(MemberAccessExpression node)
    {
        throw pending(5, "estructuras y objetos");
    }

    @Override
    public String visitFunctionCallExpression(FunctionCallExpression node)
    {
        throw pending(4, "llamadas a funciones");
    }

    @Override
    public String visitMethodCallExpression(MethodCallExpression node)
    {
        throw pending(5, "metodos");
    }

    @Override
    public String visitNewExpression(NewExpression node)
    {
        throw pending(5, "objetos");
    }

    @Override
    public String visitNewArrayExpression(NewArrayExpression node)
    {
        throw pending(5, "arreglos");
    }

    @Override
    public String visitThisExpression(ThisExpression node)
    {
        throw pending(5, "objetos");
    }

    @Override
    public String visitTernaryExpression(TernaryExpression node)
    {
        DataType type      = node.getComputedType();
        String   result    = newTemporary();
        String   otherwise = newLabel();
        String   end       = newLabel();

        jumpIfFalse(node.getCondition(), otherwise);
        emit("=", operand(node.getWhenTrue(), type), "", result);
        jump(end);
        label(otherwise);
        emit("=", operand(node.getWhenFalse(), type), "", result);
        label(end);
        return result;
    }

    @Override
    public String visitReadExpression(ReadExpression node)
    {
        throw pending(4, "lectura de consola");
    }

    @Override
    public String visitCompositeLiteralExpression(CompositeLiteralExpression node)
    {
        throw pending(5, "estructuras");
    }

    /* =================================================================
     * Conditions: jumping code
     * ================================================================= */

    /** Jumps to target when the condition is false and falls through when it is true. */
    private void jumpIfFalse(Expression condition, String target)
    {
        jumpIf(condition, false, target);
    }

    private void jumpIfTrue(Expression condition, String target)
    {
        jumpIf(condition, true, target);
    }

    /**
     * Jumps to target when the condition evaluates to {@code when}, short circuit:
     * a && b jumping when false (a || b when true) lets either side jump; the
     * other two cases let the left side skip the right one.
     */
    private void jumpIf(Expression condition, boolean when, String target)
    {
        String operator = condition instanceof BinaryExpression binary ? binary.getOperator() : "";

        if ("&&".equals(operator) || "||".equals(operator))
        {
            BinaryExpression logical = (BinaryExpression) condition;

            if ("&&".equals(operator) != when)
            {
                jumpIf(logical.getLeft(), when, target);
                jumpIf(logical.getRight(), when, target);
                return;
            }

            String skip = newLabel();

            jumpIf(logical.getLeft(), !when, skip);
            jumpIf(logical.getRight(), when, target);
            label(skip);
        }
        else if (NEGATED.containsKey(operator))
        {
            compare((BinaryExpression) condition, when ? operator : NEGATED.get(operator), target);
        }
        else if (condition instanceof UnaryExpression unary && !"-".equals(unary.getOperator()))
        {
            jumpIf(unary.getOperand(), !when, target);
        }
        else if (condition instanceof LiteralExpression literal && literal.getType() == DataType.BOOLEANO)
        {
            if (literal.isTrue() == when)
            {
                jump(target);
            }
        }
        else
        {
            emit(when ? "if!=" : "if==", condition.accept(this), "0", target);
        }
    }

    /** if a < b goto target, as floats when either side is a decimalis. */
    private void compare(BinaryExpression node, String operator, String target)
    {
        DataType left  = node.getLeft().getComputedType();
        DataType right = node.getRight().getComputedType();

        if (left == DataType.TEXTUM && right == DataType.TEXTUM)
        {
            throw pending(4, "comparacion de cadenas");
        }

        boolean  floating = left == DataType.DECIMALIS || right == DataType.DECIMALIS;
        DataType type     = floating ? DataType.DECIMALIS : DataType.NUMERUS;

        emit((floating ? "iff" : "if") + operator, operand(node.getLeft(), type), operand(node.getRight(), type),
             target);
    }

    /** A condition as a value, 1 or 0: for an assignment, an argument or a print. */
    private String booleanValue(Expression condition)
    {
        String result    = newTemporary();
        String falseCase = newLabel();
        String end       = newLabel();

        jumpIfFalse(condition, falseCase);
        emit("=", "1", "", result);
        jump(end);
        label(falseCase);
        emit("=", "0", "", result);
        label(end);
        return result;
    }

    /* =================================================================
     * Values and names
     * ================================================================= */

    /** The value of an expression, converted to the type it goes into. */
    private String operand(Expression expression, DataType target)
    {
        return convert(expression.accept(this), expression.getComputedType(), target);
    }

    /** An integer or a character that goes where a decimalis does: 2 becomes 2.0, n becomes itof n. */
    private String convert(String operand, DataType source, DataType target)
    {
        if (target != DataType.DECIMALIS || (source != DataType.NUMERUS && source != DataType.LITTERA))
        {
            return operand;
        }
        if (INTEGER.matcher(operand).matches())
        {
            return operand + ".0";
        }

        String result = newTemporary();

        emit("itof", operand, "", result);
        return result;
    }

    /** The variable a statement writes. Array cells and attributes live in the heap: Fase 5. */
    private String variable(Expression target)
    {
        if (target instanceof IdentifierExpression identifier)
        {
            return identifier.accept(this);
        }
        throw pending(5, "escritura en arreglos y atributos");
    }

    /**
     * The name a variable has in the code: its own, unless a global, another
     * local of the same function or a reserved name already has it, in which
     * case it gets _2, _3...
     */
    private String name(Symbol symbol)
    {
        return names.computeIfAbsent(symbol, key ->
        {
            String name = key.getName();

            for (int n = 2; globalNames.contains(name) || localNames.contains(name)
                    || RESERVED.matcher(name).matches(); n++)
            {
                name = key.getName() + "_" + n;
            }
            (key.isGlobal() ? globalNames : localNames).add(name);
            return name;
        });
    }

    /* =================================================================
     * Strings and booleans
     * ================================================================= */
    private void printString(String text)
    {
        helpers.add(PRINT_STRING);
        emit("param", text, "", "");
        emit("call", PRINT_STRING, "1", "");
    }

    /** verum or falsus, verdadero or falso, true or false: the words of the language being read. */
    private String booleanText(String value)
    {
        String result    = newTemporary();
        String falseText = newLabel();
        String end       = newLabel();

        emit("if==", value, "0", falseText);
        emit("=", "\"" + word(true) + "\"", "", result);
        jump(end);
        label(falseText);
        emit("=", "\"" + word(false) + "\"", "", result);
        label(end);
        return result;
    }

    private String word(boolean value)
    {
        return switch (language)
        {
            case Y  -> value ? "verdadero" : "falso";
            case Z  -> value ? "true" : "false";
            default -> value ? "verum" : "falsus";
        };
    }

    /** func __print_str(s): print_char each cell until the 0 that ends the text. */
    private void printStringFunction()
    {
        String loop      = newLabel();
        String end       = newLabel();
        String character = newTemporary();

        emit("func", "s", "", PRINT_STRING);
        label(loop);
        emit("=[]", HEAP, "s", character);
        emit("if==", character, "0", end);
        emit("print_char", character, "", "");
        emit("+", "s", "1", "s");
        jump(loop);
        label(end);
        emit("return", "", "", "");
        emit("end_func", "", "", PRINT_STRING);
    }

    /* =================================================================
     * Support
     * ================================================================= */
    private void loopBody(Block body, String breakLabel, String continueLabel)
    {
        breakLabels.push(breakLabel);
        continueLabels.push(continueLabel);
        body.accept(this);
        continueLabels.pop();
        breakLabels.pop();
    }

    /** What is not translated yet, and the phase that brings it. */
    private static UnsupportedOperationException pending(int phase, String what)
    {
        return new UnsupportedOperationException("pendiente: Fase " + phase + " (" + what + ")");
    }

    private void emit(String op, String arg1, String arg2, String result)
    {
        code.add(new Quadruple(op, arg1, arg2, result));
    }

    private void label(String name)
    {
        emit("label", "", "", name);
    }

    private void jump(String name)
    {
        emit("goto", "", "", name);
    }

    private String newTemporary()
    {
        return "t" + ++temporaryCount;
    }

    private String newLabel()
    {
        return "L" + ++labelCount;
    }
}
