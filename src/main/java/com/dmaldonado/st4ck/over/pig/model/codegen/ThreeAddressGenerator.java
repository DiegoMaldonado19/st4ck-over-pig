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
 * What outlives a call lives in the heap, reserved by moving H (design, 3):
 * an array is [sizes | cells], a structure [fields] and an object [class id |
 * fields]. A variable holds the address of its block, so passing it passes
 * it by reference.
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

    /** Layout of every structure and class: a field's index is its place in the block. */
    private final Map<String, StructDeclaration> layouts  = new HashMap<>();
    /** The id each class writes first in its objects, 1..n: what a polymorphic call reads (Fase 8). */
    private final Map<String, Integer>           classIds = new HashMap<>();

    private int      temporaryCount;
    private int      labelCount;
    /** Each language prints its booleans with its own words. */
    private Language language;

    /** The function being generated: what its return gives back takes its return type. */
    private FunctionDeclaration function;
    /** The object the method being generated runs on, its this: null outside a class. */
    private String              self;

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

        // Every layout first: a function may reach an object of a class that another file declares.
        for (Program program : programs)
        {
            for (AstNode global : program.getGlobals())
            {
                if (global instanceof StructDeclaration struct)
                {
                    layouts.put(struct.getName(), struct);
                }
                else if (global instanceof ClassDeclaration type)
                {
                    layouts.put(type.getName(), type.getLayout());
                    classIds.put(type.getName(), classIds.size() + 1);
                }
            }
        }
        language = root.getLanguage();

        // VARIABILES> of the .pig holds the only globals: they are named here, before any local.
        for (AstNode global : root.getGlobals())
        {
            Symbol symbol = global instanceof VariableDeclaration variable ? variable.getSymbol()
                          : global instanceof ArrayDeclaration array ? array.getSymbol() : null;

            if (symbol != null)
            {
                emit("global", "", "", names.computeIfAbsent(symbol, key -> declare(key.getName(), true)));
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
            for (AstNode global : program.getGlobals())
            {
                if (global instanceof ClassDeclaration type)
                {
                    type.accept(this);
                }
            }
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

    /**
     * Without a value a variable still gets one: the slot of a local keeps
     * what a previous call left. A Y? structure without a literal gets its
     * block, to be filled field by field; an object without new is null.
     */
    @Override
    public String visitVariableDeclaration(VariableDeclaration node)
    {
        Expression value     = node.getInitialValue();
        String     structure = structure(node.getType(), node.getTypeText());

        if (value == null && structure != null)
        {
            emit("=", allocate(structure, new HashSet<>()), "", name(node.getSymbol()));
            return null;
        }
        emit("=", value == null ? "0" : operand(value, node.getType()), "", name(node.getSymbol()));
        return null;
    }

    /** One block for every dimension together; a Zetariano "int[] a;" stays null, "int[] b = a;" names a. */
    @Override
    public String visitArrayDeclaration(ArrayDeclaration node)
    {
        if (node.getDimensions().isEmpty())
        {
            String array = node.getInitialValues().isEmpty() ? "0" : node.getInitialValues().get(0).accept(this);

            emit("=", array, "", name(node.getSymbol()));
            return null;
        }

        List<Expression> values  = node.getFlatValues();
        String           pointer = allocateArray(sizes(node.getDimensions()),
                values.isEmpty() ? structure(node.getElementType(), node.getTypeText()) : null, new HashSet<>());

        emit("=", pointer, "", name(node.getSymbol()));
        for (int i = 0; i < values.size(); i++)
        {
            String cell = offset(pointer, i);

            emit("[]=", cell, operand(values.get(i), node.getElementType()), HEAP);
        }
        return null;
    }

    /** A Y? structure declared inside a function: its layout, for what follows it. generate() takes the global ones. */
    @Override
    public String visitStructDeclaration(StructDeclaration node)
    {
        layouts.put(node.getName(), node);
        return null;
    }

    @Override
    public String visitStructField(StructField node)
    {
        return null;
    }

    /** Its layout was taken by generate(): here each constructor and method becomes a function. */
    @Override
    public String visitClassDeclaration(ClassDeclaration node)
    {
        for (FunctionDeclaration method : node.getMethods())
        {
            method.accept(this);
        }
        return null;
    }

    /**
     * func f(p1, p2): ... end_func f, which is also the return of a function
     * that reaches its end. A constructor or method gets its object first, as
     * this (design, 4.4).
     */
    @Override
    public String visitFunctionDeclaration(FunctionDeclaration node)
    {
        String       name       = functionName(node.getSymbol());
        List<String> parameters = new ArrayList<>();

        localNames.clear();
        function = node;
        self     = node.getOwnerClass() == null ? null : declare("this", false);
        if (self != null)
        {
            parameters.add(self);
        }
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
        assign(node.getTarget(), operand(node.getValue(), node.getTarget().getComputedType()));
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

    /**
     * Every case compares first and then the bodies follow in order, so a
     * case without break falls into the next one (design, 4.3). break leaves
     * the switch; continue still belongs to the loop around it.
     */
    @Override
    public String visitSwitchStatement(SwitchStatement node)
    {
        String       value     = node.getDiscriminant().accept(this);
        DataType     type      = node.getDiscriminant().getComputedType();
        String       end       = newLabel();
        String       otherwise = end;
        List<String> bodies    = new ArrayList<>();

        for (CaseClause clause : node.getCases())
        {
            String body = newLabel();

            bodies.add(body);
            if (clause.isDefault())
            {
                otherwise = body;
            }
            else if (type == DataType.TEXTUM)
            {
                emit("if!=", callHelper(STRING_EQUALS, value, clause.getValue().accept(this)), "0", body);
            }
            else
            {
                emit(type == DataType.DECIMALIS ? "iff==" : "if==", value, operand(clause.getValue(), type), body);
            }
        }
        jump(otherwise);

        breakLabels.push(end);
        for (int i = 0; i < bodies.size(); i++)
        {
            label(bodies.get(i));
            node.getCases().get(i).accept(this);
        }
        breakLabels.pop();
        label(end);
        return null;
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

        assign(node.getTarget(), read(node.getTarget().getComputedType()));
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
            call(call.getFunction(), implicitReceiver(call.getFunction()), call.getArguments(), false);
            return null;
        }
        if (node.getExpression() instanceof MethodCallExpression call)
        {
            call(call.getFunction(), call.getOwner().accept(this), call.getArguments(), false);
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

        emit("=", node.getTarget().accept(this), "", before);
        increment(node.getTarget(), node.getOperator());
        return before;
    }

    /** x++ is x = x + 1 through a temporary, as written by hand: t1 = x + 1, x = t1. Returns the new value. */
    private String increment(Expression target, String operator)
    {
        String  current  = target.accept(this);
        boolean floating = target.getComputedType() == DataType.DECIMALIS;
        String  result   = newTemporary();

        emit(("++".equals(operator) ? "+" : "-") + (floating ? "f" : ""), current, floating ? "1.0" : "1", result);
        assign(target, result);
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

    /** A variable by its name; an attribute named alone inside a method, from its cell in this object. */
    @Override
    public String visitIdentifierExpression(IdentifierExpression node)
    {
        if (node.getSymbol().getCategory() == SymbolCategory.ATTRIBUTE)
        {
            return load(address(node));
        }
        return name(node.getSymbol());
    }

    @Override
    public String visitArrayAccessExpression(ArrayAccessExpression node)
    {
        return load(address(node));
    }

    @Override
    public String visitMemberAccessExpression(MemberAccessExpression node)
    {
        return load(address(node));
    }

    @Override
    public String visitFunctionCallExpression(FunctionCallExpression node)
    {
        return call(node.getFunction(), implicitReceiver(node.getFunction()), node.getArguments(), true);
    }

    /** A method called without an object, from another one of its class, runs on the same object. */
    private String implicitReceiver(FunctionSymbol callee)
    {
        return callee.isMethod() ? self : null;
    }

    /**
     * Every argument first and then their params, together, right before the
     * call: a call inside an argument never ends up between them (design,
     * 4.4). The object of a method goes first, as this. Each argument goes as
     * its parameter's type, 2 into a decimalis is 2.0; an array goes as it
     * is, its address.
     */
    private String call(FunctionSymbol callee, String receiver, List<Expression> arguments, boolean wantResult)
    {
        List<String> values = new ArrayList<>();

        if (receiver != null)
        {
            values.add(receiver);
        }
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
        return call(node.getFunction(), node.getOwner().accept(this), node.getArguments(), true);
    }

    /** novus / new: the object is reserved with its class id, and its constructor fills it as this (design, 4.6). */
    @Override
    public String visitNewExpression(NewExpression node)
    {
        String object = allocate(node.getClassName(), new HashSet<>());

        call(node.getConstructor(), object, node.getArguments(), false);
        return object;
    }

    /** new int[f][c] as a value: the same block a declaration reserves. */
    @Override
    public String visitNewArrayExpression(NewArrayExpression node)
    {
        return allocateArray(sizes(node.getDimensions()), structure(node.getElementType(), node.getTypeText()),
                new HashSet<>());
    }

    @Override
    public String visitThisExpression(ThisExpression node)
    {
        return self;
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

    /** A structure literal: a fresh block, each value in the field it names or in its place. */
    @Override
    public String visitCompositeLiteralExpression(CompositeLiteralExpression node)
    {
        StructDeclaration struct  = layouts.get(node.getStructName());
        String            pointer = reserve(String.valueOf(struct.getFields().size()));

        for (int i = 0; i < node.getValues().size(); i++)
        {
            int    index = node.isNamed() ? fieldIndex(struct, node.getFieldNames().get(i)) : i;
            String cell  = offset(pointer, index);

            emit("[]=", cell, fieldValue(node.getValues().get(i), struct.getFields().get(index)), HEAP);
        }
        return pointer;
    }

    /** "animales: Animal[7]" names the TYPE: it creates an array of that size instead of reading one. */
    private String fieldValue(Expression value, StructField field)
    {
        if (field.isArray() && value instanceof ArrayAccessExpression access
                && access.getArray() instanceof IdentifierExpression typeName && typeName.getSymbol() == null)
        {
            return allocateArray(List.of(access.getIndex().accept(this)),
                    structure(field.getType(), field.getTypeText()), new HashSet<>());
        }
        return field.isArray() ? value.accept(this) : operand(value, field.getType());
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

    /**
     * The name a variable has in the code: its own, unless a global, another
     * local of the same function or a reserved name already has it, in which
     * case it gets _2, _3... The globals were named first, by generate().
     */
    private String name(Symbol symbol)
    {
        return names.computeIfAbsent(symbol, key -> declare(key.getName(), false));
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
     * Heap: arrays, structures and objects (design, 3, 4.5 and 4.6)
     * ================================================================= */

    /** target = value: a variable by its name; an element, a field or an attribute through its cell. */
    // ponytail: the value goes before the cell, and a[i] += 1 locates a[i] twice: an index with side effects
    // (a[i++] = i, a[f()] += 1) would not behave as in Java. No program writes one.
    private void assign(Expression target, String value)
    {
        if (target instanceof IdentifierExpression identifier
                && identifier.getSymbol().getCategory() != SymbolCategory.ATTRIBUTE)
        {
            emit("=", value, "", name(identifier.getSymbol()));
            return;
        }
        emit("[]=", address(target), value, HEAP);
    }

    /** The cell of an element (m[i][j]), a field (p.campo) or an attribute named alone inside a method. */
    private String address(Expression target)
    {
        if (target instanceof ArrayAccessExpression access)
        {
            return element(access);
        }
        if (target instanceof MemberAccessExpression member)
        {
            return field(member.getOwner().accept(this), member.getOwner().getStructName(), member.getMemberName());
        }
        return field(self, function.getOwnerClass(), ((IdentifierExpression) target).getName());
    }

    /** base + k, k being the field's place in the block: an object keeps its class id ahead of its fields. */
    private String field(String base, String type, String fieldName)
    {
        return offset(base, fieldIndex(layouts.get(type), fieldName) + (classIds.containsKey(type) ? 1 : 0));
    }

    /**
     * m[i][j] is one cell of the flattened matrix, m + i * d1 + j: the sizes
     * are read from the header ahead of the cells, so they may be variables
     * (design, 4.5).
     */
    private String element(ArrayAccessExpression access)
    {
        List<Expression> indices = new ArrayList<>();
        Expression       base    = access;

        while (base instanceof ArrayAccessExpression inner)
        {
            indices.add(0, inner.getIndex());
            base = inner.getArray();
        }

        int    rank    = indices.size();
        String pointer = base.accept(this);
        String index   = indices.get(0).accept(this);

        for (int k = 1; k < rank; k++)
        {
            String size   = load(offset(pointer, k - rank));
            String column = indices.get(k).accept(this);
            String row    = newTemporary();
            String cell   = newTemporary();

            emit("*", index, size, row);
            emit("+", row, column, cell);
            index = cell;
        }

        String address = newTemporary();

        emit("+", pointer, index, address);
        return address;
    }

    private String load(String address)
    {
        String value = newTemporary();

        emit("=[]", HEAP, address, value);
        return value;
    }

    /** base + k as an operand: base itself when k is 0, and base - 1 for the size right before a vector. */
    private String offset(String base, int k)
    {
        if (k == 0)
        {
            return base;
        }

        String address = newTemporary();

        emit(k > 0 ? "+" : "-", base, String.valueOf(Math.abs(k)), address);
        return address;
    }

    /** H is the first free cell of the heap: reserving n cells is taking H and moving it n ahead. */
    private String reserve(String size)
    {
        String pointer = newTemporary();

        emit("=", "H", "", pointer);
        emit("+", "H", size, "H");
        return pointer;
    }

    private List<String> sizes(List<Expression> dimensions)
    {
        return dimensions.stream().map(dimension -> dimension.accept(this)).toList();
    }

    /** a + b or a * b of two sizes, or its value when both are constants: series a[10] reserves H + 11. */
    private String fold(String operator, String left, String right)
    {
        if (INTEGER.matcher(left).matches() && INTEGER.matcher(right).matches())
        {
            int first  = Integer.parseInt(left);
            int second = Integer.parseInt(right);

            return String.valueOf("*".equals(operator) ? first * second : first + second);
        }

        String result = newTemporary();

        emit(operator, left, right, result);
        return result;
    }

    /**
     * An array is its sizes and then its cells, [d0 .. dk-1 | cells], and its
     * pointer is its first cell: heap[p - 1] is the length of a vector and
     * heap[p - k + j] the size j of a matrix (design, 4.5). An array of
     * structures gets a block in each cell, so lista[0].campo can be written
     * right away.
     */
    private String allocateArray(List<String> sizes, String structure, Set<String> expanding)
    {
        String total = sizes.get(0);

        for (int k = 1; k < sizes.size(); k++)
        {
            total = fold("*", total, sizes.get(k));
        }

        String header = reserve(fold("+", total, String.valueOf(sizes.size())));

        for (int k = 0; k < sizes.size(); k++)
        {
            String cell = offset(header, k);

            emit("[]=", cell, sizes.get(k), HEAP);
        }

        String pointer = offset(header, sizes.size());

        if (structure != null)
        {
            String index = newTemporary();
            String cell  = newTemporary();
            String loop  = newLabel();
            String end   = newLabel();

            emit("=", "0", "", index);
            label(loop);
            emit("if>=", index, total, end);
            emit("+", pointer, index, cell);
            emit("[]=", cell, allocate(structure, expanding), HEAP);
            emit("+", index, "1", index);
            jump(loop);
            label(end);
        }
        return pointer;
    }

    /**
     * A zeroed block for a structure or an object, an object's class id in
     * its first cell, with its nested structures and fixed size arrays
     * reserved too, so it can be written field by field. A field of a type
     * already being reserved (a recursive structure) and an object inside
     * another one stay null.
     */
    private String allocate(String type, Set<String> expanding)
    {
        List<StructField> fields  = layouts.get(type).getFields();
        Integer           classId = classIds.get(type);
        int               first   = classId == null ? 0 : 1;
        String            pointer = reserve(String.valueOf(first + fields.size()));

        if (classId != null)
        {
            emit("[]=", pointer, String.valueOf(classId), HEAP);
        }
        expanding.add(type);
        for (int i = 0; i < fields.size(); i++)
        {
            StructField field  = fields.get(i);
            String      nested = structure(field.getType(), field.getTypeText());

            if (expanding.contains(nested))
            {
                nested = null;
            }

            String value = field.isArray() && field.getSize() >= 0
                           ? allocateArray(List.of(String.valueOf(field.getSize())), nested, expanding)
                           : !field.isArray() && nested != null ? allocate(nested, expanding) : null;

            if (value != null)
            {
                String cell = offset(pointer, first + i);

                emit("[]=", cell, value, HEAP);
            }
        }
        expanding.remove(type);
        return pointer;
    }

    /** The structure a declaration without literal, or an array of them, reserves; null for the rest, objects too. */
    private String structure(DataType type, String typeText)
    {
        return type == DataType.ESTRUCTURA && !classIds.containsKey(typeText) ? typeText : null;
    }

    private static int fieldIndex(StructDeclaration struct, String fieldName)
    {
        for (int i = 0; i < struct.getFields().size(); i++)
        {
            if (struct.getFields().get(i).getName().equals(fieldName))
            {
                return i;
            }
        }
        throw new IllegalStateException("'" + fieldName + "' is not a field of " + struct.getName());
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
