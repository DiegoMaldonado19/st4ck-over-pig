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
import com.dmaldonado.st4ck.over.pig.model.symbols.VariableSymbol;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Traduce el AST ya validado a cuartetas. Cada visitX emite sus instrucciones
 * y devuelve el OPERANDO donde quedo su valor: un temporal tN, una constante,
 * o la direccion fija de una cadena en el heap. Las sentencias devuelven null.
 *
 * Modelo de memoria:
 *   stack[0 .. G-1]  globales de VARIABILES>
 *   stack[P + k]     marco de la funcion activa: [0] retorno, [1..n]
 *                    parametros, luego las locales (el slot lo reparte la
 *                    tabla de simbolos)
 *   heap             cadenas, arreglos, estructuras y objetos; la variable
 *                    guarda el puntero, asi que pasarlas es pasarlas por
 *                    referencia. Una matriz se aplana fila por fila.
 *
 * Un metodo es una funcion mas: recibe su objeto en stack[P + 1], antes que
 * los parametros, y un atributo nombrado a secas es heap[this + indice].
 *
 * Los temporales son locales de cada funcion C, no globales: con globales,
 * fib(n - 1) + fib(n - 2) perderia el primer resultado en la segunda llamada.
 *
 * Solo corre sobre programas sin errores: el semantico ya garantizo que cada
 * nombre tiene su simbolo y cada llamada su sobrecarga.
 */
public class QuadrupleGenerator implements AstVisitor<String>
{
    /** Where a value lives: a cell of the stack or of the heap. */
    private record Location(String memory, String address)
    {
    }

    /** The two values an increment touches: the one it read and the one it wrote. */
    private record Step(String before, String after)
    {
    }

    private static final String STACK = "stack";
    private static final String HEAP  = "heap";

    private final List<Quadruple>                code        = new ArrayList<>();
    /** Every distinct string literal, at the heap address it is written to when main starts. */
    private final Map<String, Integer>           stringPool  = new LinkedHashMap<>();
    /** Layout of every structure, in the order they are declared: .campo is its index. */
    private final Map<String, StructDeclaration> structs     = new HashMap<>();
    /** The layouts above that are classes: their instances start as null, never pre-allocated. */
    private final Set<String>                    classes     = new HashSet<>();
    /** The C name each function got: two source names may not end up as one. */
    private final Map<FunctionSymbol, String>    cNames      = new HashMap<>();
    private final Deque<String>                  breakLabels    = new ArrayDeque<>();
    private final Deque<String>                  continueLabels = new ArrayDeque<>();

    /** heap[0] holds -1, the empty string: a null textum, 0, reads as "". */
    private int poolEnd = 1;
    private int temporaryCount;
    private int labelCount;
    /** Slots of the frame being generated: a call moves P past them. */
    private int frameSize;
    /** Class whose methods are being generated: an attribute named alone belongs to it. */
    private String   currentClass;
    /** Language of the code being generated: each one writes its booleans its own way. */
    private Language language;

    /**
     * @param programs   the imported files first, the .pig with MAIOR last.
     * @param globalSize slots of the globals: MAIOR's frame starts there.
     */
    public List<Quadruple> generate(List<Program> programs, int globalSize)
    {
        // Every layout first: a method may reach an object of a class another file declares.
        for (Program program : programs)
        {
            for (AstNode global : program.getGlobals())
            {
                if (global instanceof StructDeclaration struct)
                {
                    struct.accept(this);
                }
                else if (global instanceof ClassDeclaration type)
                {
                    structs.put(type.getName(), type.getLayout());
                    classes.add(type.getName());
                }
            }
        }
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
            for (FunctionDeclaration function : program.getFunctions())
            {
                function.accept(this);
            }
        }

        Program root = programs.get(programs.size() - 1);

        language = root.getLanguage();

        emit("function", "", "", "main");
        int prologue = code.size();

        frameSize = root.getFrameSize();
        for (AstNode global : root.getGlobals())
        {
            global.accept(this);
        }
        for (AstNode statement : root.getMainStatements())
        {
            statement.accept(this);
        }
        emit("end", "", "", "main");

        // The pool is only complete once everything was generated, so its
        // initialization is inserted afterwards, at the top of main.
        code.addAll(prologue, prologue(globalSize));
        return code;
    }

    /**
     * H past the literals and P past the globals, checked before anything is
     * written: a pool bigger than the heap must stop, not overwrite memory.
     * Then heap[0] = -1 and every literal byte by byte.
     */
    private List<Quadruple> prologue(int globalSize)
    {
        List<Quadruple> start = new ArrayList<>();

        start.add(new Quadruple("=", String.valueOf(poolEnd), "", "H"));
        start.add(new Quadruple("=", String.valueOf(globalSize), "", "P"));
        start.add(new Quadruple("checkMemory", "", "", ""));
        start.add(new Quadruple("[]=", "0", "-1", HEAP));

        for (Map.Entry<String, Integer> literal : stringPool.entrySet())
        {
            byte[] bytes   = literal.getKey().getBytes(StandardCharsets.UTF_8);
            int    address = literal.getValue();

            for (byte value : bytes)
            {
                start.add(new Quadruple("[]=", String.valueOf(address++), String.valueOf(value & 0xFF), HEAP));
            }
            start.add(new Quadruple("[]=", String.valueOf(address), "-1", HEAP));
        }
        return start;
    }

    /* =================================================================
     * DECLARATIONS
     * ================================================================= */
    @Override
    public String visitProgram(Program node)
    {
        throw new UnsupportedOperationException("A program is generated through generate().");
    }

    @Override
    public String visitImportDeclaration(ImportDeclaration node)
    {
        return null;
    }

    /**
     * Without a value a variable still gets one: frames are reused, so its slot
     * may hold whatever the previous call left there. A structure without a
     * literal (Y?) gets its block, ready to be filled field by field.
     */
    @Override
    public String visitVariableDeclaration(VariableDeclaration node)
    {
        String value;

        if (node.getInitialValue() != null)
        {
            value = node.getInitialValue().accept(this);
        }
        else if (node.getType() == DataType.ESTRUCTURA && !classes.contains(node.getTypeText()))
        {
            value = allocateStruct(node.getTypeText(), new HashSet<>());
        }
        else if (node.getType() == DataType.TEXTUM && language != Language.Z)
        {
            value = String.valueOf(intern(""));   // PigLatin and Y? have no null: a text starts empty
        }
        else
        {
            value = "0";
        }
        store(slotOf(node.getSymbol()), value);
        return null;
    }

    /** One block for every dimension together; a Zetariano "int[] a;" stays null, "int[] b = a;" names a. */
    @Override
    public String visitArrayDeclaration(ArrayDeclaration node)
    {
        if (node.getDimensions().isEmpty())
        {
            store(slotOf(node.getSymbol()),
                  node.getInitialValues().isEmpty() ? "0" : node.getInitialValues().get(0).accept(this));
            return null;
        }

        List<Expression> values        = node.getFlatValues();
        String           elementStruct = node.getElementType() == DataType.ESTRUCTURA && values.isEmpty()
                                         && !classes.contains(node.getTypeText()) ? node.getTypeText() : null;
        String           pointer       = allocateArray(sizesOf(node.getDimensions()), elementStruct, new HashSet<>());

        store(slotOf(node.getSymbol()), pointer);

        for (int i = 0; i < values.size(); i++)
        {
            store(new Location(HEAP, offset(pointer, i)), values.get(i).accept(this));
        }
        return null;
    }

    /** Emits nothing: it only records the layout the accesses and literals need. */
    @Override
    public String visitStructDeclaration(StructDeclaration node)
    {
        structs.put(node.getName(), node);
        return null;
    }

    @Override
    public String visitStructField(StructField node)
    {
        return null;
    }

    @Override
    public String visitParameter(Parameter node)
    {
        return null;
    }

    /** Its layout was recorded by generate(): here every constructor and method becomes a function. */
    @Override
    public String visitClassDeclaration(ClassDeclaration node)
    {
        String previousClass = currentClass;

        currentClass = node.getName();
        for (FunctionDeclaration method : node.getMethods())
        {
            method.accept(this);
        }
        currentClass = previousClass;
        return null;
    }

    @Override
    public String visitFunctionDeclaration(FunctionDeclaration node)
    {
        String name = cName(node.getSymbol());

        frameSize = node.getSymbol().getFrameSize();
        emit("function", "", "", name);

        for (AstNode local : node.getLocalVariables())
        {
            local.accept(this);
        }
        node.getBody().accept(this);

        emit("end", "", "", name);
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
        Location target = locate(node.getTarget());

        // x op= e shares its target with the operation: it reads x where it was already located.
        String value = node.getValue() instanceof BinaryExpression compound && compound.getLeft() == node.getTarget()
                       ? binary(compound, load(target)) : node.getValue().accept(this);

        store(target, value);
        return null;
    }

    @Override
    public String visitIfStatement(IfStatement node)
    {
        String otherwise = newLabel();

        emit("ifFalse", node.getCondition().accept(this), "", otherwise);
        node.getThenBranch().accept(this);

        if (node.getElseBranch() == null)
        {
            label(otherwise);
            return null;
        }

        String end = newLabel();

        jump(end);
        label(otherwise);
        node.getElseBranch().accept(this);
        label(end);
        return null;
    }

    @Override
    public String visitWhileStatement(WhileStatement node)
    {
        String start = newLabel();
        String end   = newLabel();

        label(start);
        emit("ifFalse", node.getCondition().accept(this), "", end);
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
        emit("if", node.getCondition().accept(this), "", start);
        label(end);
        return null;
    }

    /** perge jumps to the update, not to the condition: skipping it would loop forever. */
    @Override
    public String visitForStatement(ForStatement node)
    {
        String start  = newLabel();
        String update = newLabel();
        String end    = newLabel();

        // Zetariano's for ( ; ; ) may leave out any of the three parts.
        if (node.getInitialization() != null)
        {
            node.getInitialization().accept(this);
        }
        label(start);

        if (node.getCondition() != null)
        {
            emit("ifFalse", node.getCondition().accept(this), "", end);
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
     * Every case compares first, then the bodies come in order: a case without
     * romper falls into the next one, as in C. romper leaves the elegir, while
     * continuar still belongs to the enclosing loop.
     */
    @Override
    public String visitSwitchStatement(SwitchStatement node)
    {
        String       value       = node.getDiscriminant().accept(this);
        DataType     type        = node.getDiscriminant().getComputedType();
        String       end         = newLabel();
        String       defaultCase = end;
        List<String> bodies      = new ArrayList<>();

        for (CaseClause clause : node.getCases())
        {
            String body = newLabel();

            bodies.add(body);
            if (clause.isDefault())
            {
                defaultCase = body;
                continue;
            }
            emit("if", equality(value, clause.getValue().accept(this), type, true), "", body);
        }
        // Without a case the value is still read, so C does not flag it unused: both ways lead on.
        if (node.getCases().stream().allMatch(CaseClause::isDefault))
        {
            emit("if", value, "", defaultCase);
        }
        jump(defaultCase);

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

    @Override
    public String visitReturnStatement(ReturnStatement node)
    {
        if (node.getValue() != null)
        {
            store(new Location(STACK, "P"), node.getValue().accept(this));
        }
        emit("return", "", "", "");
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

    /** Each value by its own native, then one line break: >> a >> b prints "ab\n". Zetariano's print has none. */
    @Override
    public String visitPrintStatement(PrintStatement node)
    {
        for (Expression value : node.getValues())
        {
            String   operand = value.accept(this);
            DataType type    = value.getComputedType();

            emit(printer(type), operand, type == DataType.BOOLEANO ? booleanStyle() : "", "");
        }
        if (node.endsLine())
        {
            emit("printLine", "", "", "");
        }
        return null;
    }

    /** "x <<" reads by the type of x; a bare "<<" reads a line and drops it. */
    @Override
    public String visitInputStatement(InputStatement node)
    {
        if (node.getTarget() == null)
        {
            emit("readString", "", "", "");
            return null;
        }

        Location location = locate(node.getTarget());
        String   value    = newTemporary();

        emit(reader(node.getTarget().getComputedType()), "", "", value);
        store(location, value);
        return null;
    }

    /** A call or a read alone drops its result: reading it would leave a temporary nobody uses. */
    @Override
    public String visitExpressionStatement(ExpressionStatement node)
    {
        Expression expression = node.getExpression();

        if (expression instanceof FunctionCallExpression call)
        {
            call(call.getFunction(), implicitReceiver(call.getFunction()), call.getArguments(), false);
        }
        else if (expression instanceof MethodCallExpression call)
        {
            methodCall(call, false);
        }
        else if (expression instanceof ReadExpression)
        {
            emit(reader(expression.getComputedType()), "", "", "");
        }
        else
        {
            expression.accept(this);
        }
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

        if ("&&".equals(operator) || "||".equals(operator))
        {
            return shortCircuit(node);
        }
        return binary(node, node.getLeft().accept(this));
    }

    /** The operation once its left operand is already evaluated. */
    private String binary(BinaryExpression node, String left)
    {
        String   operator  = node.getOperator();
        String   right     = node.getRight().accept(this);
        DataType leftType  = node.getLeft().getComputedType();
        DataType rightType = node.getRight().getComputedType();

        if ("+".equals(operator) && node.getComputedType() == DataType.TEXTUM)
        {
            return nativeCall("concat", text(left, leftType), text(right, rightType));
        }
        if (("==".equals(operator) || "!=".equals(operator)) && leftType == DataType.TEXTUM
                && rightType == DataType.TEXTUM)
        {
            return equality(left, right, DataType.TEXTUM, "==".equals(operator));
        }

        // numerus / numerus trunca: 7 / 2 es 3, aunque en C todo sea double.
        String op     = switch (operator)
        {
            case "/" -> node.getComputedType() == DataType.NUMERUS ? "div" : operator;
            case "%" -> "mod";
            default  -> operator;
        };
        String result = newTemporary();

        emit(op, left, right, result);
        return result;
    }

    /** a && b: b is only evaluated when a leaves the answer open. */
    private String shortCircuit(BinaryExpression node)
    {
        String result = newTemporary();
        String end    = newLabel();

        emit("=", node.getLeft().accept(this), "", result);
        emit("&&".equals(node.getOperator()) ? "ifFalse" : "if", result, "", end);
        emit("=", node.getRight().accept(this), "", result);
        label(end);
        return result;
    }

    /** Two textum are equal by content, not by address; anything else compares as a number. */
    private String equality(String left, String right, DataType type, boolean equal)
    {
        String result;

        if (type == DataType.TEXTUM)
        {
            // Only Zetariano has null: elsewhere an unset text (0) is "".
            result = nativeCall(language == Language.Z ? "stringEqualsOrNull" : "stringEquals", left, right);

            if (equal)
            {
                return result;
            }
            String negated = newTemporary();
            emit("==", result, "0", negated);
            return negated;
        }

        result = newTemporary();
        emit(equal ? "==" : "!=", left, right, result);
        return result;
    }

    /** -x is 0 - x and a negation is x == 0: no operator of their own needed. */
    @Override
    public String visitUnaryExpression(UnaryExpression node)
    {
        String operand = node.getOperand().accept(this);
        String result  = newTemporary();

        if ("-".equals(node.getOperator()))
        {
            emit("-", "0", operand, result);
        }
        else
        {
            emit("==", operand, "0", result);
        }
        return result;
    }

    @Override
    public String visitIncrementExpression(IncrementExpression node)
    {
        Step step = increment(node.getTarget(), node.getOperator());

        return node.isPrefix() ? step.after() : step.before();
    }

    private Step increment(Expression target, String operator)
    {
        Location location = locate(target);
        String   before   = load(location);
        String   after    = newTemporary();

        emit("++".equals(operator) ? "+" : "-", before, "1", after);
        store(location, after);
        return new Step(before, after);
    }

    /** Constants go straight into the operand; a textum is the address of its copy in the pool. */
    @Override
    public String visitLiteralExpression(LiteralExpression node)
    {
        return switch (node.getType())
        {
            case TEXTUM   -> String.valueOf(intern(decode(node.getText())));
            case LITTERA  -> String.valueOf(decode(node.getText()).codePointAt(0));
            case BOOLEANO -> node.isTrue() ? "1" : "0";
            case NULO     -> "0";   // no block lives at address 0: it is null
            case NUMERUS  -> node.getText().replaceFirst("^0+(?=\\d)", "");   // C reads 010 as octal 8
            default       -> node.getText();
        };
    }

    @Override
    public String visitIdentifierExpression(IdentifierExpression node)
    {
        return load(locate(node));
    }

    @Override
    public String visitArrayAccessExpression(ArrayAccessExpression node)
    {
        return load(locate(node));
    }

    @Override
    public String visitMemberAccessExpression(MemberAccessExpression node)
    {
        return load(locate(node));
    }

    @Override
    public String visitFunctionCallExpression(FunctionCallExpression node)
    {
        return call(node.getFunction(), implicitReceiver(node.getFunction()), node.getArguments(), true);
    }

    /**
     * Every argument is evaluated BEFORE any is written: a nested call would
     * reuse the same slots, past this frame, and overwrite what was already
     * there. Then P moves past the frame, and back once the callee returns.
     * A method gets its object ahead of the arguments, in the callee's slot 1.
     */
    private String call(FunctionSymbol function, String receiver, List<Expression> arguments,
                        boolean wantResult)
    {
        List<String> values = new ArrayList<>();

        if (receiver != null)
        {
            values.add(receiver);
        }
        for (Expression argument : arguments)
        {
            values.add(argument.accept(this));
        }
        for (int i = 0; i < values.size(); i++)
        {
            store(new Location(STACK, offset("P", frameSize + 1 + i)), values.get(i));
        }

        String size = String.valueOf(frameSize);

        emit("+", "P", size, "P");
        emit("checkMemory", "", "", "");
        emit("call", "", "", cName(function));
        emit("-", "P", size, "P");

        if (!wantResult || !function.returnsValue())
        {
            return null;
        }
        return load(new Location(STACK, offset("P", frameSize)));
    }

    /** A method called without an object, from inside another one, runs on the same object. */
    private String implicitReceiver(FunctionSymbol function)
    {
        return function.isMethod() ? self() : null;
    }

    /** The object of the running method, in slot 1 of its frame. */
    private String self()
    {
        return load(new Location(STACK, offset("P", 1)));
    }

    @Override
    public String visitMethodCallExpression(MethodCallExpression node)
    {
        return methodCall(node, true);
    }

    /** A method on null would read someone else's memory: it stops the program instead. */
    private String methodCall(MethodCallExpression node, boolean wantResult)
    {
        String receiver = node.getOwner().accept(this);

        emit("checkNull", receiver, "", "");
        return call(node.getFunction(), receiver, node.getArguments(), wantResult);
    }

    /** The object is reserved first and the constructor then fills it, as its receiver. */
    @Override
    public String visitNewExpression(NewExpression node)
    {
        String pointer = allocateStruct(node.getClassName(), new HashSet<>());

        call(node.getConstructor(), pointer, node.getArguments(), false);
        return pointer;
    }

    /** new int[f][c] as a value: the same block a declaration reserves. */
    @Override
    public String visitNewArrayExpression(NewArrayExpression node)
    {
        String elementStruct = node.getElementType() == DataType.ESTRUCTURA && !classes.contains(node.getTypeText())
                               ? node.getTypeText() : null;

        return allocateArray(sizesOf(node.getDimensions()), elementStruct, new HashSet<>());
    }

    @Override
    public String visitThisExpression(ThisExpression node)
    {
        return self();
    }

    @Override
    public String visitTernaryExpression(TernaryExpression node)
    {
        String result    = newTemporary();
        String otherwise = newLabel();
        String end       = newLabel();

        emit("ifFalse", node.getCondition().accept(this), "", otherwise);
        emit("=", node.getWhenTrue().accept(this), "", result);
        jump(end);
        label(otherwise);
        emit("=", node.getWhenFalse().accept(this), "", result);
        label(end);
        return result;
    }

    @Override
    public String visitReadExpression(ReadExpression node)
    {
        String value = newTemporary();

        emit(reader(node.getComputedType()), "", "", value);
        return value;
    }

    /** A structure literal: a fresh block, each value in the slot of its attribute. */
    @Override
    public String visitCompositeLiteralExpression(CompositeLiteralExpression node)
    {
        StructDeclaration struct  = structs.get(node.getStructName());
        String            pointer = reserve(String.valueOf(struct.getFields().size()));

        for (int i = 0; i < node.getValues().size(); i++)
        {
            int         slot  = node.isNamed() ? fieldIndex(struct, node.getFieldNames().get(i)) : i;
            StructField field = struct.getFields().get(slot);

            store(new Location(HEAP, offset(pointer, slot)), fieldValue(node.getValues().get(i), field));
        }
        return pointer;
    }

    /** "animales: Animal[7]" names the TYPE: it creates an array of that size instead of reading one. */
    private String fieldValue(Expression value, StructField field)
    {
        if (field.isArray() && value instanceof ArrayAccessExpression access
                && access.getArray() instanceof IdentifierExpression typeName
                && typeName.getSymbol() == null)
        {
            String element = field.getType() == DataType.ESTRUCTURA ? field.getTypeText() : null;
            return allocateArray(List.of(access.getIndex().accept(this)), element, new HashSet<>());
        }
        return value.accept(this);
    }

    /* =================================================================
     * Memory
     * ================================================================= */

    /** The stack cell of a variable: absolute for a global, relative to P otherwise. */
    private Location slotOf(Symbol symbol)
    {
        return new Location(STACK, symbol.isGlobal()
                ? String.valueOf(symbol.getOffset())
                : offset("P", symbol.getOffset()));
    }

    /** Where an assignable expression lives: x, a[i][j] or s.campo, chained as deep as needed. */
    private Location locate(Expression target)
    {
        if (target instanceof IdentifierExpression identifier)
        {
            Symbol symbol = identifier.getSymbol();

            // An attribute named alone inside a method: the attribute of this object.
            if (symbol.getCategory() == SymbolCategory.ATTRIBUTE)
            {
                return new Location(HEAP, offset(self(), fieldIndex(structs.get(currentClass), symbol.getName())));
            }
            return slotOf(symbol);
        }
        if (target instanceof ArrayAccessExpression access)
        {
            return element(access);
        }

        MemberAccessExpression member = (MemberAccessExpression) target;
        String                 owner  = member.getOwner().accept(this);
        String                 type   = member.getOwner().getStructName();

        // An object, or the field a recursive structure left null (Nodo sig): writing through it
        // would land on heap[campo], inside the string literals.
        emit("checkNull", owner, "", "");
        return new Location(HEAP, offset(owner, fieldIndex(structs.get(type), member.getMemberName())));
    }

    /**
     * m[i][j] is ONE cell of the flattened matrix: m + i * columns + j, the
     * strides read from the sizes the array carries ahead of its cells, so they
     * may be variables. A vector is checked against its length, which also
     * stops a null array; a matrix, each index against its own dimension.
     */
    private Location element(ArrayAccessExpression access)
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

        if (rank == 1)
        {
            emit("checkIndex", pointer, index, "");
        }
        else
        {
            // The header is read through the pointer: a null matrix must stop before.
            emit("checkNull", pointer, "", "");
            emit("checkBound", index, load(new Location(HEAP, offset(pointer, -rank))), "");
        }

        for (int k = 1; k < rank; k++)
        {
            String size   = load(new Location(HEAP, offset(pointer, k - rank)));
            String column = indices.get(k).accept(this);
            String row    = newTemporary();
            String cell   = newTemporary();

            // m[0][4] of a 2 x 3 is not m[1][1]: each inner index within its own dimension.
            emit("checkBound", column, size, "");
            emit("*", index, size, row);
            emit("+", row, column, cell);
            index = cell;
        }

        String address = newTemporary();

        emit("+", pointer, index, address);
        return new Location(HEAP, address);
    }

    private String load(Location location)
    {
        String value = newTemporary();

        emit("=[]", location.memory(), location.address(), value);
        return value;
    }

    private void store(Location location, String value)
    {
        emit("[]=", location.address(), value, location.memory());
    }

    /** base + k as an operand; base itself when k is 0, which spares a temporary. */
    private String offset(String base, int k)
    {
        if (k == 0)
        {
            return base;
        }

        String address = newTemporary();

        emit("+", base, String.valueOf(k), address);
        return address;
    }

    /** H is the first free cell of the heap: reserving is moving it. */
    private String reserve(String size)
    {
        String pointer = newTemporary();

        emit("=", "H", "", pointer);
        emit("+", "H", size, "H");
        emit("checkMemory", "", "", "");
        return pointer;
    }

    /** The size of each dimension, evaluated in order. */
    private List<String> sizesOf(List<Expression> dimensions)
    {
        List<String> sizes = new ArrayList<>();

        for (Expression dimension : dimensions)
        {
            sizes.add(dimension.accept(this));
        }
        return sizes;
    }

    /**
     * An array is its sizes and then its cells, [d0 .. dk-1 | cells], and its
     * pointer is the first cell: heap[pointer - 1] is the length of a vector,
     * and heap[pointer - k + j] the size of dimension j of a matrix.
     * One of structures also gets a block per element, so writing
     * arreglo[0].campo never goes through a null.
     */
    private String allocateArray(List<String> sizes, String elementStruct, Set<String> expanding)
    {
        String size = sizes.get(0);

        emit("checkSize", size, "", "");
        for (int k = 1; k < sizes.size(); k++)
        {
            String total = newTemporary();

            emit("checkSize", sizes.get(k), "", "");
            emit("*", size, sizes.get(k), total);
            size = total;
        }

        String cells   = newTemporary();
        String pointer = newTemporary();

        emit("+", size, String.valueOf(sizes.size()), cells);

        String header = reserve(cells);

        for (int k = 0; k < sizes.size(); k++)
        {
            store(new Location(HEAP, offset(header, k)), sizes.get(k));
        }
        emit("+", header, String.valueOf(sizes.size()), pointer);

        if (elementStruct == null)
        {
            return pointer;
        }

        String index = newTemporary();
        String start = newLabel();
        String end   = newLabel();
        String test  = newTemporary();

        emit("=", "0", "", index);
        label(start);
        emit("<", index, size, test);
        emit("ifFalse", test, "", end);

        String address = newTemporary();

        emit("+", pointer, index, address);
        store(new Location(HEAP, address), allocateStruct(elementStruct, expanding));
        emit("+", index, "1", index);
        jump(start);
        label(end);
        return pointer;
    }

    /**
     * A zeroed block for one structure, with its nested structures and its
     * constant size array fields reserved too, so a declared variable can be
     * written field by field. A field whose type is already being expanded (a
     * recursive structure) stays null: expanding it would never end.
     */
    private String allocateStruct(String name, Set<String> expanding)
    {
        List<StructField> fields  = structs.get(name).getFields();
        String            pointer = reserve(String.valueOf(fields.size()));

        expanding.add(name);

        for (int i = 0; i < fields.size(); i++)
        {
            StructField field  = fields.get(i);
            // An object inside another starts as null: only structures are expanded.
            String      nested = field.getType() == DataType.ESTRUCTURA && !classes.contains(field.getTypeText())
                                 && !expanding.contains(field.getTypeText()) ? field.getTypeText() : null;
            String      value  = null;

            if (field.isArray() && field.getSize() >= 0)
            {
                value = allocateArray(List.of(String.valueOf(field.getSize())), nested, expanding);
            }
            else if (!field.isArray() && nested != null)
            {
                value = allocateStruct(nested, expanding);
            }
            if (value != null)
            {
                store(new Location(HEAP, offset(pointer, i)), value);
            }
        }

        expanding.remove(name);
        return pointer;
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
     * Strings and natives
     * ================================================================= */

    /** The address the literal will have: equal texts share one copy. */
    private int intern(String text)
    {
        Integer address = stringPool.get(text);

        if (address == null)
        {
            address  = poolEnd;
            poolEnd += text.getBytes(StandardCharsets.UTF_8).length + 1;   // + the -1 terminator
            stringPool.put(text, address);
        }
        return address;
    }

    /** The text of a textum or littera literal: without its quotes, escapes resolved. */
    private static String decode(String quoted)
    {
        String        body = quoted.substring(1, quoted.length() - 1);
        StringBuilder text = new StringBuilder();

        for (int i = 0; i < body.length(); i++)
        {
            char character = body.charAt(i);

            if (character == '\\' && i + 1 < body.length())
            {
                character = switch (body.charAt(++i))
                {
                    case 'n'            -> '\n';
                    case 't'            -> '\t';
                    case 'r'            -> '\r';
                    case '"', '\'', '\\' -> body.charAt(i);
                    default             -> body.charAt(--i);   // "C:\Users" keeps its backslash
                };
            }
            text.append(character);
        }
        return text.toString();
    }

    /** The operand as a textum: concatenation turns every other type into text first. */
    private String text(String operand, DataType type)
    {
        return switch (type)
        {
            case TEXTUM    -> operand;
            case DECIMALIS -> nativeCall("floatToString", operand, "");
            case LITTERA   -> nativeCall("charToString", operand, "");
            case BOOLEANO  -> nativeCall("boolToString", operand, booleanStyle());
            default        -> nativeCall("intToString", operand, "");
        };
    }

    /** verum, verdadero or true: the index of the language, which the natives in C use. */
    private String booleanStyle()
    {
        return String.valueOf(language.ordinal());
    }

    private String nativeCall(String name, String first, String second)
    {
        String result = newTemporary();

        emit(name, first, second, result);
        return result;
    }

    private static String printer(DataType type)
    {
        return switch (type)
        {
            case TEXTUM    -> "printString";
            case DECIMALIS -> "printFloat";
            case LITTERA   -> "printChar";
            case BOOLEANO  -> "printBool";
            default        -> "printInt";
        };
    }

    private static String reader(DataType type)
    {
        return switch (type)
        {
            case TEXTUM    -> "readString";
            case DECIMALIS -> "readFloat";
            case LITTERA   -> "readChar";
            case BOOLEANO  -> "readBool";
            default        -> "readInt";
        };
    }

    /**
     * Its C name: fn_ keeps it apart from the natives and from C itself, and
     * the parameter types keep every overload apart. fn_something and
     * fn_something__numerus are two functions. '_' is also legal inside a
     * name, so Pila.apilar and a function Pila_apilar would meet: the second
     * one to ask gets a number.
     */
    private String cName(FunctionSymbol function)
    {
        return cNames.computeIfAbsent(function, key ->
        {
            StringBuilder name      = new StringBuilder("fn_" + key.getName());
            String        separator = "__";

            for (VariableSymbol parameter : key.getParameters())
            {
                name.append(separator).append(parameter.isArray() ? "arr_" : "").append(parameter.getTypeText());
                separator = "_";
            }

            // Only what C takes in a name: Clase.<atributos> becomes fn_Clase__atributos_, a type Año, A_o.
            String base   = name.toString().replaceAll("[^A-Za-z0-9_]", "_");
            String unique = base;

            for (int n = 2; cNames.containsValue(unique); n++)
            {
                unique = base + "_" + n;
            }
            return unique;
        });
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
        return "t" + temporaryCount++;
    }

    private String newLabel()
    {
        return "L" + labelCount++;
    }
}
