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
import com.dmaldonado.st4ck.over.pig.model.symbols.FunctionSymbol;
import com.dmaldonado.st4ck.over.pig.model.symbols.Symbol;
import com.dmaldonado.st4ck.over.pig.model.symbols.SymbolCategory;
import com.dmaldonado.st4ck.over.pig.model.symbols.SymbolTable;
import com.dmaldonado.st4ck.over.pig.model.symbols.VariableSymbol;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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

    /** main and the other helpers: no function of the program takes their names. */
    private static final String MAIN            = "main";
    private static final String CONCAT          = "__concat";
    private static final String INT_TO_STRING   = "__int_to_str";
    private static final String FLOAT_TO_STRING = "__float_to_str";
    private static final String CHAR_TO_STRING  = "__char_to_str";
    private static final String STRING_EQUALS   = "__str_eq";

    private final List<Quadruple>     code           = new ArrayList<>();
    /** The name each variable has in the code: its own, unless another variable already has it. */
    private final Map<Symbol, String> names          = new HashMap<>();
    private final Set<String>         globalNames    = new HashSet<>();
    private final Set<String>         localNames     = new HashSet<>();
    /** The helper functions the program used, in the order it first did: they go after its functions. */
    private final List<String>        helpers        = new ArrayList<>();
    private final Deque<String>       breakLabels    = new ArrayDeque<>();
    private final Deque<String>       continueLabels = new ArrayDeque<>();

    /** The name each function has in the code, and every name already taken by one. */
    private final Map<FunctionSymbol, String> functionNames  = new HashMap<>();
    private final Set<String>                 takenFunctions = new HashSet<>(Set.of(MAIN, PRINT_STRING, CONCAT,
            INT_TO_STRING, FLOAT_TO_STRING, CHAR_TO_STRING, STRING_EQUALS));
    /** Every overload of each function: a name declared more than once carries its parameter types. */
    private final SymbolTable                 symbolTable;

    private int      temporaryCount;
    private int      labelCount;
    /** Each language prints its booleans with its own words. */
    private Language language;

    /** The function being generated: what its return gives back takes its return type. */
    private FunctionDeclaration function;

    public ThreeAddressGenerator(SymbolTable symbolTable)
    {
        this.symbolTable = symbolTable;
    }

    /**
     * The imported files first and the .pig with MAIOR last: the globals,
     * main, the functions of every file and then the helpers the program used.
     */
    public List<Quadruple> generate(List<Program> programs)
    {
        Program root = programs.get(programs.size() - 1);

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

        emit("func", "", "", MAIN);
        for (AstNode global : root.getGlobals())
        {
            global.accept(this);
        }
        for (AstNode statement : root.getMainStatements())
        {
            statement.accept(this);
        }
        emit("halt", "", "", "");
        emit("end_func", "", "", MAIN);

        for (Program program : programs)
        {
            language = program.getLanguage();
            for (FunctionDeclaration declaration : program.getFunctions())
            {
                declaration.accept(this);
            }
        }
        // A helper may call another one, __float_to_str writes through __int_to_str: the list grows as it goes.
        for (int i = 0; i < helpers.size(); i++)
        {
            helper(helpers.get(i));
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

    /** func f(p1, p2): ... end_func f, which is also the return of a function that reaches its end. */
    @Override
    public String visitFunctionDeclaration(FunctionDeclaration node)
    {
        String       name       = functionName(node.getSymbol());
        List<String> parameters = new ArrayList<>();

        localNames.clear();
        function = node;
        for (Parameter parameter : node.getParameters())
        {
            parameters.add(name(parameter.getSymbol()));
        }

        emit("func", String.join(",", parameters), "", name);
        for (AstNode local : node.getLocalVariables())
        {
            local.accept(this);
        }
        node.getBody().accept(this);
        emit("end_func", "", "", name);
        return null;
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

    /** return v, as the type the function returns: 0 out of a decimalis function is 0.0. */
    @Override
    public String visitReturnStatement(ReturnStatement node)
    {
        Expression value = node.getValue();

        emit("return", value == null ? "" : operand(value, function.getReturnType()), "", "");
        return null;
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
            case NUMERUS   -> emit("print_int", value.accept(this), "", "");
            case LITTERA   -> emit("print_char", value.accept(this), "", "");
            case BOOLEANO  -> printString(booleanText(value.accept(this)));
            case DECIMALIS -> printString(callHelper(FLOAT_TO_STRING, value.accept(this)));
            default        -> printString(value.accept(this));
        }
    }

    /** x << reads by the type of x; a bare << reads a line and drops it. */
    @Override
    public String visitInputStatement(InputStatement node)
    {
        if (node.getTarget() == null)
        {
            emit("read_str", "", "", "");
            return null;
        }

        String value = read(node.getTarget().getComputedType());

        emit("=", value, "", variable(node.getTarget()));
        return null;
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
        if (node.getExpression() instanceof FunctionCallExpression call)
        {
            call(call.getFunction(), call.getArguments(), false);
            return null;
        }
        if (node.getExpression() instanceof ReadExpression)
        {
            emit("read_str", "", "", "");
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
            String left  = text(node.getLeft());
            String right = text(node.getRight());

            return callHelper(CONCAT, left, right);
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
        return call(node.getFunction(), node.getArguments(), true);
    }

    /**
     * Every argument first and then their params, together, right before the
     * call: a call inside an argument never ends up between them (design,
     * 4.4). Each one goes as its parameter's type, 2 into a decimalis is 2.0;
     * an array goes as it is, its address.
     */
    private String call(FunctionSymbol callee, List<Expression> arguments, boolean wantResult)
    {
        List<String> values = new ArrayList<>();

        for (int i = 0; i < arguments.size(); i++)
        {
            VariableSymbol parameter = callee.getParameters().get(i);

            values.add(parameter.isArray() ? arguments.get(i).accept(this)
                                           : operand(arguments.get(i), parameter.getType()));
        }
        for (String value : values)
        {
            emit("param", value, "", "");
        }

        String result = wantResult && callee.returnsValue() ? newTemporary() : "";

        emit("call", functionName(callee), String.valueOf(values.size()), result);
        return result.isEmpty() ? null : result;
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

    /** leer() / readln(): its type is the one its destination expects. */
    @Override
    public String visitReadExpression(ReadExpression node)
    {
        return read(node.getComputedType());
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

    /**
     * if a < b goto target, as floats when either side is a decimalis. Two
     * textum are equal by their characters, not by their address: __str_eq
     * gives 1 or 0. Against null, a pointer compares as a number.
     */
    private void compare(BinaryExpression node, String operator, String target)
    {
        DataType left  = node.getLeft().getComputedType();
        DataType right = node.getRight().getComputedType();

        if (left == DataType.TEXTUM && right == DataType.TEXTUM)
        {
            String first  = node.getLeft().accept(this);
            String second = node.getRight().accept(this);
            String equal  = callHelper(STRING_EQUALS, first, second);

            emit("==".equals(operator) ? "if!=" : "if==", equal, "0", target);
            return;
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
        return names.computeIfAbsent(symbol, key -> declare(key.getName(), key.isGlobal()));
    }

    /** A variable of the code, a helper's included: base, or base_2, base_3... when that one is taken. */
    private String declare(String base, boolean global)
    {
        String name = unique(base, taken -> globalNames.contains(taken) || localNames.contains(taken)
                || RESERVED.matcher(taken).matches());

        (global ? globalNames : localNames).add(name);
        return name;
    }

    /**
     * The name a function has in the code: its own, Clase_metodo for a
     * method, with its parameter types when it is overloaded
     * (sumar__entero_entero), and _2, _3... when main, a helper or another
     * function already has it.
     */
    private String functionName(FunctionSymbol function)
    {
        return functionNames.computeIfAbsent(function, key ->
        {
            // Clase.<atributos>, the method of the attribute initializers, is Clase_atributos.
            StringBuilder base      = new StringBuilder(key.getName().replace('.', '_').replaceAll("[<>]", ""));
            String        separator = "__";

            if (symbolTable.lookupFunctions(key.getName()).size() > 1)
            {
                for (VariableSymbol parameter : key.getParameters())
                {
                    base.append(separator).append(parameter.isArray() ? "arr_" : "").append(parameter.getTypeText());
                    separator = "_";
                }
            }

            String name = unique(base.toString(), takenFunctions::contains);

            takenFunctions.add(name);
            return name;
        });
    }

    /** base, or base_2, base_3...: the first one not taken. */
    private static String unique(String base, Predicate<String> taken)
    {
        String name = base;

        for (int n = 2; taken.test(name); n++)
        {
            name = base + "_" + n;
        }
        return name;
    }

    /* =================================================================
     * Strings and booleans
     * ================================================================= */
    private void printString(String text)
    {
        useHelper(PRINT_STRING);
        emit("param", text, "", "");
        emit("call", PRINT_STRING, "1", "");
    }

    /** param each argument and call the helper: the temporary that holds what it returns. */
    private String callHelper(String helper, String... arguments)
    {
        String result = newTemporary();

        useHelper(helper);
        for (String argument : arguments)
        {
            emit("param", argument, "", "");
        }
        emit("call", helper, String.valueOf(arguments.length), result);
        return result;
    }

    private void useHelper(String helper)
    {
        if (!helpers.contains(helper))
        {
            helpers.add(helper);
        }
    }

    /** The value as a textum, for a +: each type through its own helper, a boolean through its word. */
    private String text(Expression value)
    {
        String operand = value.accept(this);

        return switch (value.getComputedType())
        {
            case NUMERUS   -> callHelper(INT_TO_STRING, operand);
            case DECIMALIS -> callHelper(FLOAT_TO_STRING, operand);
            case LITTERA   -> callHelper(CHAR_TO_STRING, operand);
            case BOOLEANO  -> booleanText(operand);
            default        -> operand;
        };
    }

    /** A line of the console as the type that receives it: a bool is true when it is the language's word. */
    private String read(DataType type)
    {
        String value = newTemporary();

        emit(switch (type)
        {
            case NUMERUS   -> "read_int";
            case DECIMALIS -> "read_float";
            case LITTERA   -> "read_char";
            default        -> "read_str";
        }, "", "", value);
        return type == DataType.BOOLEANO ? callHelper(STRING_EQUALS, value, "\"" + word(true) + "\"") : value;
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

    /* =================================================================
     * Helpers: the functions the compiler writes in three address code
     * (design, 5), only those the program used
     * ================================================================= */

    /** Its parameters are named as any variable: a global s would otherwise be read in place of its own. */
    private void helper(String name)
    {
        localNames.clear();
        switch (name)
        {
            case PRINT_STRING    -> printStringFunction();
            case CONCAT          -> concatFunction();
            case INT_TO_STRING   -> intToStringFunction();
            case FLOAT_TO_STRING -> floatToStringFunction();
            case CHAR_TO_STRING  -> charToStringFunction();
            default              -> stringEqualsFunction();
        }
    }

    /** func __print_str(s): print_char each cell until the 0 that ends the text. */
    private void printStringFunction()
    {
        String text      = declare("s", false);
        String loop      = newLabel();
        String end       = newLabel();
        String character = newTemporary();

        emit("func", text, "", PRINT_STRING);
        label(loop);
        emit("=[]", HEAP, text, character);
        emit("if==", character, "0", end);
        emit("print_char", character, "", "");
        emit("+", text, "1", text);
        jump(loop);
        label(end);
        emit("return", "", "", "");
        emit("end_func", "", "", PRINT_STRING);
    }

    /** func __concat(a, b): a and then b, copied from H on, and the 0 that ends them. */
    private void concatFunction()
    {
        String first  = declare("a", false);
        String second = declare("b", false);
        String start  = newTemporary();

        emit("func", first + "," + second, "", CONCAT);
        emit("=", "H", "", start);
        copyText(first);
        copyText(second);
        endText(start);
        emit("end_func", "", "", CONCAT);
    }

    /** Each character of the text at source to heap[H], H after it, up to the 0 that it leaves out. */
    private void copyText(String source)
    {
        String loop      = newLabel();
        String end       = newLabel();
        String character = newTemporary();

        label(loop);
        emit("=[]", HEAP, source, character);
        emit("if==", character, "0", end);
        emit("[]=", "H", character, HEAP);
        emit("+", "H", "1", "H");
        emit("+", source, "1", source);
        jump(loop);
        label(end);
    }

    /** The 0 that ends the text written up to H, and its start back to the caller. */
    private void endText(String start)
    {
        emit("[]=", "H", "0", HEAP);
        emit("+", "H", "1", "H");
        emit("return", start, "", "");
    }

    /**
     * func __int_to_str(n): the sign, then the digits counted first and
     * written right to left, n % 10 each, so the text ends where it should.
     */
    private void intToStringFunction()
    {
        String number   = declare("n", false);
        String start    = newTemporary();
        String rest     = newTemporary();
        String digits   = newTemporary();
        String cell     = newTemporary();
        String digit    = newTemporary();
        String code     = newTemporary();
        String positive = newLabel();
        String count    = newLabel();
        String write    = newLabel();

        emit("func", number, "", INT_TO_STRING);
        emit("=", "H", "", start);
        emit("if>=", number, "0", positive);
        emit("[]=", "H", "45", HEAP);
        emit("+", "H", "1", "H");
        // ponytail: -2147483648 has no positive in 32 bits and prints wrong; no program reaches it.
        emit("uminus", number, "", number);
        label(positive);
        emit("=", number, "", rest);
        emit("=", "0", "", digits);
        label(count);
        emit("+", digits, "1", digits);
        emit("/", rest, "10", rest);
        emit("if!=", rest, "0", count);
        emit("+", "H", digits, cell);
        emit("[]=", cell, "0", HEAP);
        emit("+", cell, "1", "H");
        label(write);
        emit("-", cell, "1", cell);
        emit("%", number, "10", digit);
        emit("+", digit, "48", code);
        emit("[]=", cell, code, HEAP);
        emit("/", number, "10", number);
        emit("if!=", number, "0", write);
        emit("return", start, "", "");
        emit("end_func", "", "", INT_TO_STRING);
    }

    /**
     * func __float_to_str(f): the sign, the integer part through
     * __int_to_str, a '.' over the 0 that ends it, and six decimals, rounded,
     * without the zeros at their end: 2.0, 0.5, 4.833333.
     *
     * The decimals come out of the fraction counted in 2^-24ths, an integer,
     * digit by digit as with n % 10: fraction * 1000000.0 in 32 bits would
     * round 4.83333349 to 4.833334.
     */
    private void floatToStringFunction()
    {
        String value    = declare("f", false);
        String start    = newTemporary();
        String whole    = newTemporary();
        String back     = newTemporary();
        String fraction = newTemporary();
        String scaled   = newTemporary();
        String rest     = newTemporary();
        String decimals = newTemporary();
        String count    = newTemporary();
        String digit    = newTemporary();
        String cell     = newTemporary();
        String code     = newTemporary();
        String positive = newLabel();
        String next     = newLabel();
        String fits     = newLabel();
        String trim     = newLabel();
        String trimmed  = newLabel();
        String write    = newLabel();

        emit("func", value, "", FLOAT_TO_STRING);
        emit("=", "H", "", start);
        emit("iff>=", value, "0.0", positive);
        emit("[]=", "H", "45", HEAP);
        emit("+", "H", "1", "H");
        emit("uminusf", value, "", value);
        label(positive);
        // ponytail: from 2^31 on the integer part saturates; no program prints numbers that large.
        emit("ftoi", value, "", whole);
        emit("itof", whole, "", back);
        emit("-f", value, back, fraction);
        emit("*f", fraction, "16777216.0", scaled);
        emit("ftoi", scaled, "", rest);
        emit("=", "0", "", decimals);
        emit("=", "6", "", count);
        label(next);
        emit("*", rest, "10", rest);
        emit("/", rest, "16777216", digit);
        emit("%", rest, "16777216", rest);
        emit("*", decimals, "10", decimals);
        emit("+", decimals, digit, decimals);
        emit("-", count, "1", count);
        emit("if!=", count, "0", next);
        // What is left past the sixth decimal rounds it up from half on: 2^23 of 2^24.
        emit("if<", rest, "8388608", fits);
        emit("+", decimals, "1", decimals);
        emit("if<", decimals, "1000000", fits);
        // 0.9999999 rounds up to the next integer.
        emit("=", "0", "", decimals);
        emit("+", whole, "1", whole);
        label(fits);
        useHelper(INT_TO_STRING);
        emit("param", whole, "", "");
        emit("call", INT_TO_STRING, "1", "");
        emit("-", "H", "1", "H");
        emit("[]=", "H", "46", HEAP);
        emit("+", "H", "1", "H");
        emit("=", "6", "", count);
        label(trim);
        emit("if==", count, "1", trimmed);
        emit("%", decimals, "10", digit);
        emit("if!=", digit, "0", trimmed);
        emit("/", decimals, "10", decimals);
        emit("-", count, "1", count);
        jump(trim);
        label(trimmed);
        emit("+", "H", count, cell);
        emit("[]=", cell, "0", HEAP);
        emit("+", cell, "1", "H");
        label(write);
        emit("-", cell, "1", cell);
        emit("%", decimals, "10", digit);
        emit("+", digit, "48", code);
        emit("[]=", cell, code, HEAP);
        emit("/", decimals, "10", decimals);
        emit("-", count, "1", count);
        emit("if!=", count, "0", write);
        emit("return", start, "", "");
        emit("end_func", "", "", FLOAT_TO_STRING);
    }

    /** func __char_to_str(c): a text of one character. */
    private void charToStringFunction()
    {
        String character = declare("c", false);
        String start     = newTemporary();

        emit("func", character, "", CHAR_TO_STRING);
        emit("=", "H", "", start);
        emit("[]=", "H", character, HEAP);
        emit("+", "H", "1", "H");
        endText(start);
        emit("end_func", "", "", CHAR_TO_STRING);
    }

    /** func __str_eq(a, b): 1 when they have the same characters, 0 at the first one that differs. */
    private void stringEqualsFunction()
    {
        String first     = declare("a", false);
        String second    = declare("b", false);
        String loop      = newLabel();
        String different = newLabel();
        String equal     = newLabel();
        String mine      = newTemporary();
        String theirs    = newTemporary();

        emit("func", first + "," + second, "", STRING_EQUALS);
        label(loop);
        emit("=[]", HEAP, first, mine);
        emit("=[]", HEAP, second, theirs);
        emit("if!=", mine, theirs, different);
        emit("if==", mine, "0", equal);
        emit("+", first, "1", first);
        emit("+", second, "1", second);
        jump(loop);
        label(different);
        emit("return", "0", "", "");
        label(equal);
        emit("return", "1", "", "");
        emit("end_func", "", "", STRING_EQUALS);
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
