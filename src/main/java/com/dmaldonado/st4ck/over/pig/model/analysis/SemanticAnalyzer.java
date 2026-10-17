package com.dmaldonado.st4ck.over.pig.model.analysis;

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
import com.dmaldonado.st4ck.over.pig.model.codegen.RiscVEmitter;
import com.dmaldonado.st4ck.over.pig.model.errors.ErrorManager;
import com.dmaldonado.st4ck.over.pig.model.symbols.ArraySymbol;
import com.dmaldonado.st4ck.over.pig.model.symbols.FunctionSymbol;
import com.dmaldonado.st4ck.over.pig.model.symbols.StructSymbol;
import com.dmaldonado.st4ck.over.pig.model.symbols.Symbol;
import com.dmaldonado.st4ck.over.pig.model.symbols.SymbolCategory;
import com.dmaldonado.st4ck.over.pig.model.symbols.SymbolTable;
import com.dmaldonado.st4ck.over.pig.model.symbols.VariableSymbol;
import com.dmaldonado.st4ck.over.pig.model.symbols.Visibility;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import com.dmaldonado.st4ck.over.pig.model.types.TypeSystem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Semantic analysis: every visitX validates its rule and returns the DataType,
 * so the parent keeps composing the check upwards. On failure it reports the
 * error and returns ERROR instead of throwing.
 *
 * One instance per Program. The pipeline runs the imported .y files first and
 * the .pig last, all on the same SymbolTable, so the .pig sees what they declare.
 *
 * Besides checking, it leaves on the AST what the three address generator
 * needs: the symbol behind each name and the overload each call resolved to.
 */
public class SemanticAnalyzer implements AstVisitor<DataType>
{
    private final ErrorManager errorManager;
    private final SymbolTable  symbolTable;

    /** The few rules that differ: where to declare, and whether a structura may be declared at all. */
    private Language            language;
    /** Function being analyzed; null inside MAIOR. */
    private FunctionDeclaration currentFunction;
    /** Class whose methods are being analyzed; null outside a .z file. */
    private StructSymbol        currentClass;
    /** Loop nesting: interrumpe/perge are only valid when greater than zero. */
    private int                 loopDepth;
    /** elegir nesting: romper also leaves an elegir. */
    private int                 switchDepth;
    /**
     * True donde el enunciado prohibe declarar en PigLatin: el cuerpo de
     * si/dum/facere/per y el cuerpo de una funcion, cuyas variables van todas en
     * VARIABILES[ ]. Y? permite declarar en cualquier parte.
     */
    private boolean             declarationsForbidden;
    /** Only to give each per scope a distinct name in the symbol table graph. */
    private int                 forCounter;

    public SemanticAnalyzer(ErrorManager errorManager, SymbolTable symbolTable)
    {
        this.errorManager = errorManager;
        this.symbolTable  = symbolTable;
    }

    /* =================================================================
     * PROGRAM
     * ================================================================= */

    /**
     * Paso 1: estructuras, clases y firmas, antes de leer un solo cuerpo. El
     * pipeline lo corre sobre TODOS los archivos antes del paso 2, asi un
     * archivo usa lo que declara otro importado despues: Pila.z usa Nodo sin
     * importarlo.
     */
    public void register(Program node)
    {
        language = node.getLanguage();

        for (AstNode global : node.getGlobals())
        {
            if (global instanceof StructDeclaration struct)
            {
                registerStruct(struct, false);
            }
            else if (global instanceof ClassDeclaration type)
            {
                registerClass(type);
            }
        }
        for (FunctionDeclaration function : node.getFunctions())
        {
            registerFunctionSignature(function);
        }
    }

    /** Paso 2: recorre los cuerpos, con todo ya registrado por register(). */
    @Override
    public DataType visitProgram(Program node)
    {
        language = node.getLanguage();

        for (AstNode global : node.getGlobals())
        {
            if (global instanceof StructDeclaration struct)
            {
                validateStructFieldTypes(struct);   // already registered in pass 1
            }
            else
            {
                global.accept(this);
            }
        }
        for (FunctionDeclaration function : node.getFunctions())
        {
            function.accept(this);
        }

        symbolTable.openScope("MAIOR");
        for (AstNode statement : node.getMainStatements())
        {
            statement.accept(this);
        }
        symbolTable.closeScope();
        return DataType.VOID;
    }

    /** The pipeline resolves imports before any analysis runs: nothing is left to check here. */
    @Override
    public DataType visitImportDeclaration(ImportDeclaration node)
    {
        return DataType.VOID;
    }

    /* =================================================================
     * DECLARATIONS
     * ================================================================= */
    @Override
    public DataType visitVariableDeclaration(VariableDeclaration node)
    {
        requireDeclarationPlace(node, node.getName());

        if (symbolTable.lookupLocal(node.getName()) != null)
        {
            error(node, "La variable '" + node.getName() + "' ya fue declarada en el ambito '"
                    + symbolTable.getCurrentScopeName() + "'.", node.getName());
            return DataType.ERROR;
        }

        checkTypeName(node, node.getTypeText());

        DataType declaredType = node.getType();
        String   structName   = null;
        boolean  object       = false;

        if (declaredType == DataType.ESTRUCTURA)
        {
            StructSymbol struct = symbolTable.lookupStruct(node.getTypeText());

            if (struct == null)
            {
                // A novus of that same unknown class says it better: report that one alone.
                if (node.getInitialValue() instanceof NewExpression instance
                        && instance.getClassName().equals(node.getTypeText()))
                {
                    instance.accept(this);
                }
                else
                {
                    error(node, "El tipo '" + node.getTypeText() + "' no existe.", node.getTypeText());
                }
                declareVariable(node, DataType.ERROR, null);
                return DataType.ERROR;
            }
            structName = struct.getName();
            object     = struct.isClass();
        }

        Expression value = node.getInitialValue();

        if (value != null)
        {
            DataType valueType = checkValue(value, declaredType, structName, false);

            if (!assignable(declaredType, structName, valueType, value.getStructName()))
            {
                error(node, "No se puede asignar un valor '"
                        + describe(valueType, value.getStructName()) + "' a la variable '"
                        + node.getName() + "' de tipo '" + node.getTypeText() + "'.",
                        node.getName());
            }
        }
        else if (declaredType == DataType.ESTRUCTURA && language == Language.PIG && !object)
        {
            // Statement: "todos los atributos tengan un valor explicito". En Y?
            // "Persona alumno" si es valido: sus campos se llenan despues. Un
            // objeto sin novus es null, como en Java.
            error(node, "La variable '" + node.getName() + "' de tipo '" + node.getTypeText()
                    + "' debe inicializar todos sus atributos.", node.getName());
        }

        declareVariable(node, declaredType, structName);
        return declaredType;
    }

    private void declareVariable(VariableDeclaration node, DataType type, String structName)
    {
        VariableSymbol symbol = new VariableSymbol(node.getName(), type, node.getTypeText(),
                SymbolCategory.VARIABLE, symbolTable.getCurrentScopeName(),
                node.getInitialValue() != null, structName, 0, node.getLine(), node.getColumn());

        symbolTable.declare(symbol);
        node.setSymbol(symbol);
    }

    @Override
    public DataType visitArrayDeclaration(ArrayDeclaration node)
    {
        requireDeclarationPlace(node, node.getName());

        if (symbolTable.lookupLocal(node.getName()) != null)
        {
            error(node, "El arreglo '" + node.getName() + "' ya fue declarado en el ambito '"
                    + symbolTable.getCurrentScopeName() + "'.", node.getName());
            return DataType.ERROR;
        }

        checkTypeName(node, node.getTypeText());

        DataType elementType       = node.getElementType();
        String   elementStructName = null;

        if (node.getTypeText() == null)
        {
            error(node, "Falta el tipo del arreglo '" + node.getName()
                    + "' y no hay valores iniciales de los cuales deducirlo.", node.getName());
            node.getDimensions().forEach(size -> size.accept(this));
            declareArray(node, DataType.ERROR, null, Collections.nCopies(Math.max(1, node.getRank()), -1));
            return DataType.ERROR;
        }
        else if (elementType == DataType.ESTRUCTURA)
        {
            StructSymbol struct = symbolTable.lookupStruct(node.getTypeText());

            if (struct == null)
            {
                error(node, "El tipo '" + node.getTypeText() + "' no existe.", node.getTypeText());
                declareArray(node, DataType.ERROR, null, Collections.nCopies(Math.max(1, node.getRank()), -1));
                return DataType.ERROR;
            }
            elementStructName = struct.getName();
        }

        List<Integer> dimensions = new ArrayList<>();

        for (Expression size : node.getDimensions())
        {
            dimensions.add(dimension(node, node.getName(), size));
        }
        validateDimensions(node, dimensions);

        if (dimensions.isEmpty())
        {
            dimensions = Collections.nCopies(node.getRank(), -1);   // "int[] a;" starts as null
        }
        if (!node.getTypeText().equals(node.getCreatedTypeText()))
        {
            error(node, "'" + node.getName() + "' es un arreglo de '" + node.getTypeText() + "' y se crea con 'new "
                    + node.getCreatedTypeText() + "[...]'.", node.getName());
        }

        // int[] b = a: no size and one value that is a whole array, which b now names too.
        if (node.getDimensions().isEmpty() && node.getInitialValues().size() == 1
                && !(node.getInitialValues().get(0) instanceof CompositeLiteralExpression))
        {
            Expression value     = node.getInitialValues().get(0);
            DataType   valueType = checkValue(value, elementType, elementStructName, true);

            checkArrayShape(value, node.getRank());
            if (!fits(true, elementType, elementStructName, valueType, value.getStructName()))
            {
                error(value, "Un arreglo de '" + describe(valueType, value.getStructName())
                        + "' es incompatible con el arreglo '" + node.getName() + "' de '"
                        + node.getTypeText() + "'.", node.getName());
            }
            declareArray(node, elementType, elementStructName, dimensions);
            return elementType;
        }
        if (!node.getInitialValues().isEmpty())
        {
            validateShape(node, node.getInitialValues(), dimensions, 0);
        }
        for (Expression value : node.getFlatValues())
        {
            DataType valueType = checkValue(value, elementType, elementStructName, false);

            if (!assignable(elementType, elementStructName, valueType, value.getStructName()))
            {
                error(value, "Valor de tipo '" + describe(valueType, value.getStructName())
                        + "' incompatible con el arreglo '" + node.getName() + "' de tipo '"
                        + node.getTypeText() + "'.", node.getName());
            }
        }

        declareArray(node, elementType, elementStructName, dimensions);
        return elementType;
    }

    /** One dimension of an array declared or created with new: numerus. Its value when constant, -1 otherwise. */
    private int dimension(AstNode node, String name, Expression size)
    {
        DataType sizeType = size.accept(this);

        if (sizeType != DataType.NUMERUS && sizeType != DataType.ERROR)
        {
            error(node, "El tamano del arreglo '" + name
                    + "' debe ser '" + describe(DataType.NUMERUS, null) + "', se recibio '"
                    + describe(sizeType, null) + "'.", name);
        }

        Integer declared = extractInteger(size);

        if (declared != null && declared < 0)
        {
            error(node, "El tamano del arreglo '" + name + "' no puede ser negativo.", name);
            return -1;
        }
        return declared == null ? -1 : declared;
    }

    /**
     * The sizes may be variables, even the inner ones: an array keeps them in the heap and
     * m[i][j] reads its row length there. Only their number has to match the declaration.
     */
    private void validateDimensions(ArrayDeclaration node, List<Integer> dimensions)
    {
        if (!dimensions.isEmpty() && dimensions.size() != node.getRank())
        {
            error(node, "'" + node.getName() + "' se declara con " + node.getRank() + " dimension(es) y se "
                    + "crea con " + dimensions.size() + ".", node.getName());
        }
    }

    /** The values against the dimensions, level by level: a matrix takes one { } per row. */
    private void validateShape(ArrayDeclaration node, List<Expression> values, List<Integer> dimensions,
                               int level)
    {
        int    expected = level < dimensions.size() ? dimensions.get(level) : -1;
        String name     = node.getName();

        if (expected >= 0 && values.size() != expected)
        {
            error(level == 0 || values.isEmpty() ? node : values.get(0), "El arreglo '" + name + "' declara " + expected
                    + " posiciones" + (level > 0 ? " por fila" : "") + " pero recibe " + values.size()
                    + " valores.", name);
        }
        if (level + 1 >= dimensions.size())
        {
            return;
        }
        for (Expression row : values)
        {
            if (!(row instanceof CompositeLiteralExpression literal))
            {
                error(row, "La matriz '" + name + "' recibe sus valores fila por fila: se esperaba "
                        + "'{ ... }'.", name);
                return;
            }
            validateShape(node, literal.getValues(), dimensions, level + 1);
        }
    }

    private void declareArray(ArrayDeclaration node, DataType elementType, String elementStructName,
                              List<Integer> dimensions)
    {
        ArraySymbol symbol = new ArraySymbol(node.getName(), elementType, node.getTypeText(),
                symbolTable.getCurrentScopeName(), dimensions, node.getFlatValues().size(),
                elementStructName, node.getLine(), node.getColumn());

        symbolTable.declare(symbol);
        node.setSymbol(symbol);
    }

    /** Solo para una structura suelta: las de VARIABILES ya las registro el paso 1. */
    @Override
    public DataType visitStructDeclaration(StructDeclaration node)
    {
        registerStruct(node, false);
        validateStructFieldTypes(node);
        return DataType.ESTRUCTURA;
    }

    @Override
    public DataType visitStructField(StructField node)
    {
        return node.getType();
    }

    @Override
    public DataType visitParameter(Parameter node)
    {
        return node.getType();
    }

    /**
     * The attributes are in scope inside every method, below its parameters
     * and locals: "cima = nuevo" reaches the attribute unless a local shadows it.
     */
    @Override
    public DataType visitClassDeclaration(ClassDeclaration node)
    {
        validateStructFieldTypes(node.getLayout());

        StructSymbol previousClass = currentClass;

        currentClass = symbolTable.lookupStruct(node.getName());
        symbolTable.openScope(node.getName());

        if (currentClass != null)
        {
            currentClass.getAttributes().values().forEach(symbolTable::declare);
        }
        for (FunctionDeclaration method : node.getMethods())
        {
            if (method.isConstructor() && !method.getName().equals(node.getName()))
            {
                error(method, "El constructor '" + method.getName() + "' debe llamarse como la clase '"
                        + node.getName() + "'; si es un metodo, escriba su tipo de retorno.",
                        method.getName());
            }
            method.accept(this);
        }

        symbolTable.closeScope();
        currentClass = previousClass;
        return DataType.VOID;
    }

    @Override
    public DataType visitFunctionDeclaration(FunctionDeclaration node)
    {
        FunctionDeclaration previousFunction  = currentFunction;
        boolean             previousForbidden = declarationsForbidden;
        int                 previousDepth     = loopDepth;
        int                 previousSwitch    = switchDepth;

        currentFunction       = node;
        declarationsForbidden = false;   // VARIABILES[ ] es justo donde SI se declara
        loopDepth             = 0;
        switchDepth           = 0;

        symbolTable.openScope(node.getName());

        if (node.returnsValue())
        {
            checkTypeName(node, node.getReturnTypeText());
        }

        if (node.returnsValue() && node.getReturnType() == DataType.ESTRUCTURA
                && symbolTable.lookupStruct(node.getReturnTypeText()) == null)
        {
            error(node, "El tipo de retorno '" + node.getReturnTypeText() + "' no existe.",
                    node.getReturnTypeText());
        }

        // A method receives its object first, ahead of the parameters.
        if (node.getOwnerClass() != null)
        {
            symbolTable.declare(new VariableSymbol("this", DataType.ESTRUCTURA, node.getOwnerClass(),
                    SymbolCategory.PARAMETER, node.getName(), true, node.getOwnerClass(), 0,
                    node.getLine(), node.getColumn()));
        }

        for (Parameter parameter : node.getParameters())
        {
            if (symbolTable.lookupLocal(parameter.getName()) != null)
            {
                error(parameter, "El parametro '" + parameter.getName() + "' esta repetido.",
                        parameter.getName());
                continue;
            }
            checkTypeName(parameter, parameter.getTypeText());

            // Y?: "{} Persona p" is how a structure is received, and only a structure.
            boolean structure = parameter.getType() == DataType.ESTRUCTURA && !parameter.isArray();

            if (language == Language.Y && parameter.hasStructMarker() != structure)
            {
                error(parameter, structure
                        ? "La estructura '" + parameter.getName() + "' se recibe por referencia: escriba '{} "
                          + parameter.getTypeText() + " " + parameter.getName() + "'."
                        : "'{}' solo marca un parametro de tipo estructura; '" + parameter.getName()
                          + "' es '" + parameter.getTypeText() + "' y se pasa por valor.", parameter.getName());
            }
            if (parameter.getType() == DataType.ESTRUCTURA
                    && symbolTable.lookupStruct(parameter.getTypeText()) == null)
            {
                error(parameter, "El tipo '" + parameter.getTypeText() + "' del parametro '"
                        + parameter.getName() + "' no existe.", parameter.getTypeText());
            }
            Symbol symbol = parameterSymbol(parameter, node.getName());

            symbolTable.declare(symbol);
            parameter.setSymbol(symbol);
        }

        // RISC-V passes the arguments in a0-a7, the object of a method first: one more has no register.
        int registers = RiscVEmitter.ARGUMENTS - (node.getOwnerClass() == null ? 0 : 1);

        if (node.getParameters().size() > registers)
        {
            Parameter extra = node.getParameters().get(registers);

            error(extra, (node.isConstructor() ? "El constructor" : functionWord(node, true)) + " '" + node.getName()
                    + "' recibe " + node.getParameters().size() + " parametros"
                    + (node.getOwnerClass() == null ? "" : " y el objeto") + ": RISC-V pasa a lo sumo "
                    + RiscVEmitter.ARGUMENTS + " argumentos en registros (a0-a7).", extra.getName());
        }

        // Las structuras locales van primero: la siguiente variable ya puede usarlas.
        for (AstNode local : node.getLocalVariables())
        {
            if (local instanceof StructDeclaration struct)
            {
                registerStruct(struct, false);
            }
        }
        for (AstNode local : node.getLocalVariables())
        {
            if (local instanceof StructDeclaration struct)
            {
                validateStructFieldTypes(struct);
            }
            else
            {
                local.accept(this);
            }
        }

        // Enunciado: "dentro de las funciones solo se pueden definir variables
        // al principio". Pasada la seccion VARIABILES[ ], el cuerpo ya no admite
        // ninguna declaracion mas. El cuerpo comparte el ambito de los parametros:
        // una local no puede llamarse igual que uno de ellos.
        declarationsForbidden = true;
        visitStatements(node.getBody());
        symbolTable.closeScope();

        if (node.returnsValue() && !ReturnPathChecker.alwaysReturns(node.getBody()))
        {
            error(node, functionWord(node, true) + " '" + node.getName()
                    + "' no retorna un valor en todos sus caminos posibles.", node.getName());
        }

        currentFunction       = previousFunction;
        declarationsForbidden = previousForbidden;
        loopDepth             = previousDepth;
        switchDepth           = previousSwitch;
        return node.getReturnType();
    }

    /** A "[] entero arr" parameter is an array: it has to be indexable inside the body. */
    private Symbol parameterSymbol(Parameter parameter, String functionName)
    {
        String structName = structNameOf(parameter.getType(), parameter.getTypeText());

        if (parameter.isArray())
        {
            return new ArraySymbol(parameter.getName(), parameter.getType(), parameter.getTypeText(),
                    functionName, Collections.nCopies(parameter.getRank(), -1), 0, structName,
                    parameter.getLine(), parameter.getColumn());
        }
        return new VariableSymbol(parameter.getName(), parameter.getType(), parameter.getTypeText(),
                SymbolCategory.PARAMETER, functionName, true, structName, 0,
                parameter.getLine(), parameter.getColumn());
    }

    /* =================================================================
     * STATEMENTS
     * ================================================================= */

    /** Every block is a scope: what si, dum or a Zetariano { } declares ends with it. */
    @Override
    public DataType visitBlock(Block node)
    {
        symbolTable.openScope(symbolTable.getCurrentScopeName());
        visitStatements(node);
        symbolTable.closeScope();
        return DataType.VOID;
    }

    private void visitStatements(Block node)
    {
        int unreachable = ReturnPathChecker.firstUnreachableIndex(node);

        for (AstNode statement : node.getStatements())
        {
            statement.accept(this);
        }
        // After them: an error of that statement at the same spot tells more, and this one gives way.
        if (unreachable >= 0)
        {
            AstNode statement = node.getStatements().get(unreachable);

            error(statement, "Codigo inalcanzable: nunca se ejecutara esta instruccion.", statement.getLabel());
        }
    }

    @Override
    public DataType visitAssignment(Assignment node)
    {
        DataType targetType   = node.getTarget().accept(this);
        String   targetStruct = node.getTarget().getStructName();
        boolean  wholeArray   = isArrayValue(node.getTarget());
        DataType valueType    = checkValue(node.getValue(), targetType, targetStruct, wholeArray);

        if (wholeArray)
        {
            checkArrayShape(node.getValue(), dimensionsOf(node.getTarget()).size());

            // Its declared sizes no longer say how long it is.
            if (node.getTarget() instanceof IdentifierExpression identifier
                    && identifier.getSymbol() instanceof ArraySymbol array)
            {
                array.markRebound();
            }
        }
        if (!fits(wholeArray, targetType, targetStruct, valueType, node.getValue().getStructName()))
        {
            error(node, "Asignacion invalida: no se puede guardar " + (wholeArray ? "un arreglo de '" : "'")
                    + describe(valueType, node.getValue().getStructName()) + "' en "
                    + (wholeArray ? "un arreglo de '" : "un destino de tipo '")
                    + describe(targetType, targetStruct) + "'.", "=");
        }
        return targetType;
    }

    @Override
    public DataType visitIfStatement(IfStatement node)
    {
        requireBooleanCondition(node.getCondition(), keyword("si", "si", "if"));

        node.getThenBranch().accept(this);

        if (node.getElseBranch() != null)
        {
            node.getElseBranch().accept(this);
        }
        return DataType.VOID;
    }

    @Override
    public DataType visitWhileStatement(WhileStatement node)
    {
        requireBooleanCondition(node.getCondition(), keyword("dum", "mientras", "while"));
        walkLoopBody(node.getBody());
        return DataType.VOID;
    }

    @Override
    public DataType visitDoWhileStatement(DoWhileStatement node)
    {
        walkLoopBody(node.getBody());
        requireBooleanCondition(node.getCondition(), keyword("facere-dum", "hacer-mientras", "do-while"));
        return DataType.VOID;
    }

    @Override
    public DataType visitForStatement(ForStatement node)
    {
        symbolTable.openScope("per_" + (++forCounter));

        // El iterador es la unica declaracion que el enunciado permite fuera de
        // VARIABILES[ ], asi que la bandera se limpia solo para el: dentro de una
        // funcion ya viene levantada y su "per (esto i : numerus 0; ...)" es valido.
        boolean previousForbidden = declarationsForbidden;
        declarationsForbidden     = false;

        if (node.getInitialization() != null)
        {
            node.getInitialization().accept(this);
        }
        declarationsForbidden = previousForbidden;

        requireBooleanCondition(node.getCondition(), keyword("per", "para", "for"));

        if (node.getUpdate() != null)
        {
            node.getUpdate().accept(this);
        }
        walkLoopBody(node.getBody());
        symbolTable.closeScope();
        return DataType.VOID;
    }

    /** The case values have to be comparable with the discriminant, by the same rule as '=='. */
    @Override
    public DataType visitSwitchStatement(SwitchStatement node)
    {
        DataType discriminant = operandType(node.getDiscriminant(), "elegir");

        switchDepth++;

        for (CaseClause clause : node.getCases())
        {
            if (!clause.isDefault())
            {
                DataType value = operandType(clause.getValue(), "caso");

                if (discriminant != DataType.ERROR && value != DataType.ERROR
                        && TypeSystem.relationalResult(discriminant, value, "==") == DataType.ERROR)
                {
                    error(clause.getValue(), "El valor del caso es '" + describe(value, null)
                            + "' y no se puede comparar con '" + describe(discriminant, null) + "'.",
                            "caso");
                }
            }
            clause.accept(this);
        }

        switchDepth--;
        return DataType.VOID;
    }

    @Override
    public DataType visitCaseClause(CaseClause node)
    {
        return node.getBody().accept(this);
    }

    @Override
    public DataType visitReturnStatement(ReturnStatement node)
    {
        String keyword = keyword("reddere", "retornar", "return");

        if (currentFunction == null)
        {
            error(node, "'" + keyword + "' solo puede usarse dentro de una funcion.", keyword);
            return DataType.ERROR;
        }

        if (node.getValue() == null)
        {
            if (currentFunction.returnsValue())
            {
                error(node, functionWord(currentFunction, true) + " '" + currentFunction.getName() + "' debe retornar '"
                        + currentFunction.getReturnTypeText() + "'.", keyword);
            }
            return DataType.VOID;
        }

        String   expectedStruct = structNameOf(currentFunction.getReturnType(),
                                               currentFunction.getReturnTypeText());
        DataType returnedType   = checkValue(node.getValue(), currentFunction.getReturnType(),
                                             expectedStruct, false);

        if (!currentFunction.returnsValue())
        {
            error(node, functionWord(currentFunction, true) + " '" + currentFunction.getName() + "' "
                    + keyword("fue declarada con 'actio'", "no declara '-> tipo'", "es 'void'")
                    + " y no puede retornar un valor.", keyword);
        }
        else if (!assignable(currentFunction.getReturnType(), expectedStruct, returnedType,
                             node.getValue().getStructName()))
        {
            error(node, "Se retorna '"
                    + describe(returnedType, node.getValue().getStructName())
                    + "' pero " + functionWord(currentFunction, false) + " '" + currentFunction.getName() + "' declara '"
                    + currentFunction.getReturnTypeText() + "'.", keyword);
        }
        return returnedType;
    }

    @Override
    public DataType visitBreakStatement(BreakStatement node)
    {
        if (loopDepth == 0 && switchDepth == 0)
        {
            String keyword = keyword("interrumpe", "romper", "break");
            error(node, "'" + keyword + "' solo puede usarse dentro de un ciclo"
                    + keyword("", " o un elegir", " o un switch") + ".", keyword);
        }
        return DataType.VOID;
    }

    @Override
    public DataType visitContinueStatement(ContinueStatement node)
    {
        if (loopDepth == 0)
        {
            String keyword = keyword("perge", "continuar", "continue");
            error(node, "'" + keyword + "' solo puede usarse dentro de un ciclo.", keyword);
        }
        return DataType.VOID;
    }

    @Override
    public DataType visitPrintStatement(PrintStatement node)
    {
        String keyword = keyword(">>", "imprimir", "print");

        for (Expression value : node.getValues())
        {
            DataType type = operandType(value, keyword);

            if (type == DataType.ESTRUCTURA)
            {
                error(value, "No se puede imprimir una " + structWord() + " u objeto completo; imprima sus "
                        + "atributos.", keyword);
            }
            else if (type == DataType.VOID)
            {
                error(value, "'" + nameOf(value) + "' no retorna un valor que se pueda imprimir.",
                        keyword);
            }
        }
        return DataType.VOID;
    }

    @Override
    public DataType visitInputStatement(InputStatement node)
    {
        // A bare "<<" is valid: it reads and discards. There is nothing to check.
        if (node.getTarget() == null)
        {
            return DataType.VOID;
        }

        DataType type = node.getTarget().accept(this);

        if (type == DataType.ESTRUCTURA || isArrayValue(node.getTarget()))
        {
            error(node, "Solo se puede leer un valor primitivo desde la entrada, no una " + structWord() + " "
                    + "ni un arreglo completo.", "<<");
        }
        return DataType.VOID;
    }

    /**
     * Only what has an effect may stand alone: a call, a method call, leer() or
     * ++/--. Anything else is reported once, without descending into it, so
     * "pergue;" gives exactly one error.
     */
    @Override
    public DataType visitExpressionStatement(ExpressionStatement node)
    {
        Expression expression = node.getExpression();

        if (expression instanceof FunctionCallExpression || expression instanceof MethodCallExpression
                || expression instanceof ReadExpression || expression instanceof IncrementExpression
                || expression instanceof NewExpression)
        {
            expression.accept(this);
            return DataType.VOID;
        }

        String text = nameOf(expression);
        error(node, "'" + text + "' no es una instruccion valida: solo "
                + keyword("una llamada", "una llamada, 'leer()'", "una llamada, 'new', 'readln()'")
                + " o '++'/'--' pueden ir solas.", text);
        return DataType.ERROR;
    }

    @Override
    public DataType visitIncrementStatement(IncrementStatement node)
    {
        DataType type = operandType(node.getTarget(), node.getOperator());

        if (TypeSystem.incrementResult(type) == DataType.ERROR && type != DataType.ERROR)
        {
            error(node, "El operador '" + node.getOperator() + "' solo aplica a "
                    + describe(DataType.NUMERUS, null) + " o " + describe(DataType.DECIMALIS, null)
                    + ", no a '" + describe(type, null) + "'.", node.getOperator());
        }
        return type;
    }

    /* =================================================================
     * EXPRESSIONS
     * ================================================================= */
    @Override
    public DataType visitBinaryExpression(BinaryExpression node)
    {
        String   operator = node.getOperator();

        // arr == null: a whole array only compares against null.
        if (("==".equals(operator) || "!=".equals(operator))
                && (isNull(node.getLeft()) && isArrayOperand(node.getRight())
                    || isNull(node.getRight()) && isArrayOperand(node.getLeft())))
        {
            return typeCheck(node, DataType.BOOLEANO);
        }

        DataType left     = operandType(node.getLeft(), operator);
        DataType right    = operandType(node.getRight(), operator);

        DataType result = switch (operator)
        {
            case "+", "-", "*", "/", "%"          -> TypeSystem.arithmeticResult(left, right, operator);
            case "==", "!=", "<", ">", "<=", ">=" -> TypeSystem.relationalResult(left, right, operator);
            case "&&", "||"                       -> TypeSystem.logicalResult(left, right);
            default                               -> DataType.ERROR;
        };

        if (result == DataType.ERROR && left != DataType.ERROR && right != DataType.ERROR)
        {
            // La pista solo aplica si hay un textum de por medio: sin esta guarda
            // salia hasta en "'Carro' == 'Carro'", donde no viene a cuento.
            boolean concatenation = (left == DataType.TEXTUM || right == DataType.TEXTUM)
                    && List.of("-", "*", "/", "%").contains(operator);

            error(node, "Operacion invalida: '"
                    + describe(left, node.getLeft().getStructName()) + "' " + operator + " '"
                    + describe(right, node.getRight().getStructName()) + "'."
                    + (concatenation ? " Recuerde que " + describe(DataType.TEXTUM, null) + " solo admite '+'." : ""),
                    operator);
        }
        return typeCheck(node, result);
    }

    /** "-" is the minus sign; any other operator ("non", "!") is logical negation. */
    @Override
    public DataType visitUnaryExpression(UnaryExpression node)
    {
        // Java's one literal that only fits with its sign: -2147483648.
        if ("-".equals(node.getOperator()) && node.getOperand() instanceof LiteralExpression literal
                && literal.getType() == DataType.NUMERUS && "2147483648".equals(literal.getText()))
        {
            typeCheck(literal, DataType.NUMERUS);
            return typeCheck(node, DataType.NUMERUS);
        }

        DataType operand = operandType(node.getOperand(), node.getOperator());
        DataType result  = "-".equals(node.getOperator())
                ? TypeSystem.unaryMinusResult(operand)
                : TypeSystem.negationResult(operand);

        if (result == DataType.ERROR && operand != DataType.ERROR)
        {
            error(node, "El operador '" + node.getOperator() + "' no puede aplicarse a '"
                    + describe(operand, null) + "'.", node.getOperator());
        }
        return typeCheck(node, result);
    }

    @Override
    public DataType visitIncrementExpression(IncrementExpression node)
    {
        Expression target = node.getTarget();

        if (!(target instanceof IdentifierExpression || target instanceof ArrayAccessExpression
                || target instanceof MemberAccessExpression))
        {
            target.accept(this);
            error(node, "El operador '" + node.getOperator() + "' necesita una variable, una posicion "
                    + "de arreglo o un atributo donde guardar el resultado.", node.getOperator());
            return typeCheck(node, DataType.ERROR);
        }

        DataType operand = operandType(target, node.getOperator());
        DataType result  = TypeSystem.incrementResult(operand);

        if (result == DataType.ERROR && operand != DataType.ERROR)
        {
            error(node, "El operador '" + node.getOperator() + "' solo aplica a "
                    + describe(DataType.NUMERUS, null) + " o " + describe(DataType.DECIMALIS, null)
                    + ", no a '" + describe(operand, null) + "'.", node.getOperator());
        }
        return typeCheck(node, result);
    }

    @Override
    public DataType visitLiteralExpression(LiteralExpression node)
    {
        // A numerus is a 32 bit word: a longer literal would wrap around.
        if (node.getType() == DataType.NUMERUS && extractInteger(node) == null)
        {
            error(node, "El entero " + node.getText() + " excede el rango permitido (hasta 2147483647).",
                    node.getText());
            return typeCheck(node, DataType.ERROR);
        }
        // A decimalis is a 32 bit float: a larger decimal would be infinity.
        if (node.getType() == DataType.DECIMALIS && Float.isInfinite(Float.parseFloat(node.getText())))
        {
            error(node, "El decimal " + node.getText() + " excede el rango permitido.", node.getText());
            return typeCheck(node, DataType.ERROR);
        }
        return typeCheck(node, node.getType());
    }

    @Override
    public DataType visitIdentifierExpression(IdentifierExpression node)
    {
        Symbol symbol = symbolTable.lookup(node.getName());

        if (symbol == null)
        {
            // Las funciones viven fuera de los ambitos: sin esta consulta, usar
            // una sin parentesis diria "no declarada" en vez de la pista util.
            error(node, symbolTable.lookupFunctions(node.getName()).isEmpty()
                    ? "La variable '" + node.getName() + "' no ha sido declarada."
                    : "'" + node.getName() + "' es una funcion; para llamarla escriba '"
                            + node.getName() + "(...)'.", node.getName());
            return typeCheck(node, DataType.ERROR);
        }
        if (symbol instanceof StructSymbol struct)
        {
            error(node, "'" + node.getName() + "' es una "
                    + (struct.isClass() ? "clase" : structWord())
                    + ", no una variable.", node.getName());
            return typeCheck(node, DataType.ERROR);
        }

        node.setSymbol(symbol);

        if (symbol instanceof VariableSymbol variable)
        {
            node.setStructName(variable.getStructName());
        }
        else if (symbol instanceof ArraySymbol array)
        {
            // Reporta el tipo del ELEMENTO, que es lo que numeros[i] necesita.
            // Operar el arreglo pelado lo corta antes operandType().
            node.setStructName(array.getElementStructName());
        }
        return typeCheck(node, symbol.getType());
    }

    /**
     * m[i][j] is ONE access to a flattened matrix: the chain of indices is
     * walked down to the array it indexes, and each index is checked against
     * its own dimension. m[i] alone is not an element.
     */
    @Override
    public DataType visitArrayAccessExpression(ArrayAccessExpression node)
    {
        List<ArrayAccessExpression> chain = new ArrayList<>();
        Expression                  base  = node;

        while (base instanceof ArrayAccessExpression access)
        {
            chain.add(0, access);
            base = access.getArray();
        }
        for (ArrayAccessExpression access : chain)
        {
            DataType indexType = access.getIndex().accept(this);

            if (indexType != DataType.NUMERUS && indexType != DataType.ERROR)
            {
                error(access, "El indice de un arreglo debe ser '" + describe(DataType.NUMERUS, null)
                        + "', se recibio '" + describe(indexType, null) + "'.", "[]");
            }
        }

        DataType elementType = base.accept(this);
        String   name        = nameOf(base);

        // El nombre de la structura sobrevive al indice: sin eso no se resuelve
        // una cadena como mi_selva.animales[1].nombre.
        for (ArrayAccessExpression access : chain)
        {
            access.setStructName(base.getStructName());
            typeCheck(access, DataType.ERROR);
        }

        if (elementType == DataType.ERROR)
        {
            return DataType.ERROR;
        }
        if (!isArrayValue(base))
        {
            error(node, "'" + name + "' no es un arreglo.", name);
            return DataType.ERROR;
        }

        List<Integer> dimensions = dimensionsOf(base);

        if (chain.size() != dimensions.size())
        {
            error(node, "'" + name + "' tiene " + dimensions.size() + " dimension(es) y se indexo con "
                    + chain.size() + " indice(s).", name);
            return DataType.ERROR;
        }
        for (int i = 0; i < chain.size(); i++)
        {
            Integer index = extractInteger(chain.get(i).getIndex());
            int     size  = dimensions.get(i);

            if (index != null && index < 0)
            {
                error(node, "Indice fuera de rango: las posiciones de un arreglo "
                        + "empiezan en 0.", "[]");
            }
            else if (index != null && size >= 0 && index >= size
                     && !(base instanceof IdentifierExpression identifier
                          && identifier.getSymbol() instanceof ArraySymbol array && array.isRebound()))
            {
                error(node, "Indice fuera de rango: el arreglo '" + name + "' tiene " + size
                        + " posiciones" + (dimensions.size() > 1 ? " en la dimension " + (i + 1) : "")
                        + ".", "[]");
            }
        }
        for (ArrayAccessExpression access : chain)
        {
            typeCheck(access, elementType);
        }
        return elementType;
    }

    /**
     * Sizes of an array value, -1 where they are not constant: those of its declaration or of
     * its new. An attribute only knows how many it has.
     */
    private List<Integer> dimensionsOf(Expression array)
    {
        if (array instanceof IdentifierExpression identifier && identifier.getSymbol() instanceof ArraySymbol symbol)
        {
            return symbol.getDimensions();
        }

        VariableSymbol attribute = array instanceof IdentifierExpression identifier
                                   && identifier.getSymbol() instanceof VariableSymbol variable ? variable
                                   : array instanceof MemberAccessExpression member ? attributeOf(member) : null;

        if (attribute != null && attribute.isArray())
        {
            return Collections.nCopies(attribute.getRank(), -1);
        }
        if (array instanceof NewArrayExpression created)
        {
            return created.getDimensions().stream()
                    .map(size -> Objects.requireNonNullElse(extractInteger(size), -1))
                    .toList();
        }
        return List.of(-1);
    }

    @Override
    public DataType visitMemberAccessExpression(MemberAccessExpression node)
    {
        Expression owner = node.getOwner();

        // Un error en el dueno ya fue reportado: el acceso no suma otro.
        if (owner.accept(this) == DataType.ERROR)
        {
            return typeCheck(node, DataType.ERROR);
        }
        if (isArrayValue(owner))
        {
            error(node, "'" + nameOf(owner) + "' es un arreglo: indexe una posicion antes de "
                    + "acceder a '." + node.getMemberName() + "'.", node.getMemberName());
            return typeCheck(node, DataType.ERROR);
        }

        String structName = owner.getStructName();

        if (structName == null)
        {
            error(node, "El acceso '." + node.getMemberName()
                    + "' solo es valido sobre una variable de tipo "
                    + keyword("structura", "estructura", "clase") + ".",
                    node.getMemberName());
            return typeCheck(node, DataType.ERROR);
        }

        StructSymbol struct = symbolTable.lookupStruct(structName);

        if (struct == null || !struct.hasAttribute(node.getMemberName()))
        {
            error(node, (struct != null && struct.isClass() ? "La clase '" : "La " + structWord() + " '")
                    + structName + "' no tiene el atributo '" + node.getMemberName() + "'.",
                    node.getMemberName());
            return typeCheck(node, DataType.ERROR);
        }

        VariableSymbol attribute = struct.getAttribute(node.getMemberName());

        checkAccess(node, "El atributo '" + struct.getName() + "." + node.getMemberName() + "'",
                attribute.getVisibility(), struct.getName(), node.getMemberName());
        node.setStructName(attribute.getStructName());
        return typeCheck(node, attribute.getType());
    }

    /** Resolved by resolve(), like any other call: see there how an overload is chosen. */
    @Override
    public DataType visitFunctionCallExpression(FunctionCallExpression node)
    {
        // Inside a class a bare call reaches its own methods first, as in Java.
        List<FunctionSymbol> methods   = currentClass == null ? List.of()
                                         : methodsOf(currentClass.getName(), node.getName());
        List<FunctionSymbol> overloads = methods.isEmpty() ? symbolTable.lookupFunctions(node.getName())
                                         : methods;

        if (overloads.isEmpty())
        {
            visitArguments(node.getArguments());
            error(node, "La funcion '" + node.getName() + "' no ha sido declarada.", node.getName());
            return typeCheck(node, DataType.ERROR);
        }

        FunctionSymbol function = resolve(node, methods.isEmpty() ? "La funcion" : "El metodo",
                node.getName(), overloads, node.getArguments());

        return typed(node, function);
    }

    /**
     * Resolucion de sobrecarga, la misma para funciones, metodos y
     * constructores: los argumentos se visitan UNA vez (un literal { } espera
     * al parametro elegido, que es quien le da su tipo), luego se eligen los
     * candidatos por aridad y, si queda mas de uno, por compatibilidad.
     *
     * @return the chosen overload, or null once the reason was reported. A
     *         single overload with the wrong arity is still returned, so the
     *         call keeps its type and does not cascade.
     */
    private FunctionSymbol resolve(Expression node, String kind, String name,
                                   List<FunctionSymbol> overloads, List<Expression> arguments)
    {
        List<DataType> types = new ArrayList<>();

        for (Expression argument : arguments)
        {
            types.add(argument instanceof CompositeLiteralExpression ? null : argument.accept(this));
        }

        List<FunctionSymbol> candidates = overloads.stream()
                .filter(function -> function.getParameterCount() == arguments.size())
                .toList();

        if (candidates.isEmpty())
        {
            if (overloads.size() == 1)
            {
                FunctionSymbol function = overloads.get(0);

                error(node, kind + " '" + function.getSignature() + "' espera "
                        + function.getParameterCount() + " argumento(s) y recibio "
                        + arguments.size() + ".", name);
                return function;
            }
            error(node, "Ninguna version de '" + name + "' recibe " + arguments.size()
                    + " argumento(s). Versiones: " + signatures(overloads) + ".", name);
            return null;
        }

        // An argument that already failed fits every overload: picking one would add the errors of its guess.
        if (candidates.size() > 1 && types.contains(DataType.ERROR))
        {
            return null;
        }

        List<FunctionSymbol> best = candidates.size() == 1
                ? candidates
                : mostSpecific(candidates, arguments, types);

        if (best.size() != 1)
        {
            error(node, best.isEmpty()
                    ? "Ninguna version de '" + name + "' acepta esos argumentos. Versiones: "
                      + signatures(overloads) + "."
                    : "La llamada a '" + name + "' es ambigua entre: " + signatures(best) + ".", name);
            return null;
        }

        FunctionSymbol function = best.get(0);

        for (int i = 0; i < arguments.size(); i++)
        {
            VariableSymbol parameter    = function.getParameters().get(i);
            Expression     argument     = arguments.get(i);
            DataType       argumentType = types.get(i) == null
                    ? checkValue(argument, parameter.getType(), parameter.getStructName(), parameter.isArray())
                    : checkShape(argument, types.get(i), parameter.isArray());

            if (parameter.isArray())
            {
                checkArrayShape(argument, parameter.getRank());
            }

            if (!fits(parameter.isArray(), parameter.getType(), parameter.getStructName(), argumentType,
                      argument.getStructName()))
            {
                String of = parameter.isArray() ? "un arreglo de '" : "'";

                error(argument, "El argumento " + (i + 1) + " de '" + name + "' debe ser " + of
                        + describe(parameter.getType(), parameter.getStructName()) + "' y es " + of
                        + describe(argumentType, argument.getStructName()) + "'.", name);
            }
        }
        return function;
    }

    /** The type of a call is the return type of the overload it resolved to. */
    private DataType typed(Expression node, FunctionSymbol function)
    {
        if (function == null)
        {
            return typeCheck(node, DataType.ERROR);
        }
        if (node instanceof FunctionCallExpression call)
        {
            call.setFunction(function);
        }
        else if (node instanceof MethodCallExpression call)
        {
            call.setFunction(function);
        }
        node.setStructName(function.getReturnStructName());
        return typeCheck(node, function.getType());
    }

    /** The methods of a class with that name; its constructors are not among them. */
    private List<FunctionSymbol> methodsOf(String className, String methodName)
    {
        return symbolTable.lookupFunctions(className + "." + methodName).stream()
                .filter(function -> !function.isConstructor())
                .toList();
    }

    /**
     * Encapsulation: what is private or protected is only used inside its own
     * class, on any object of it, as in Java. Without packages (auxiliar,
     * 15-oct) default reaches as far as public; the subclasses protected also
     * reaches come with Fase 8.
     *
     * ponytail: the overload is chosen first and its access checked after;
     * Java leaves the inaccessible ones out before choosing.
     */
    private void checkAccess(AstNode node, String member, Visibility visibility, String ownerClass, String lexeme)
    {
        if ((visibility == Visibility.PRIVATE || visibility == Visibility.PROTECTED)
                && (currentClass == null || !currentClass.getName().equals(ownerClass)))
        {
            error(node, member + (visibility == Visibility.PRIVATE
                    ? " es private: solo puede usarse dentro de la clase '" + ownerClass + "'."
                    : " es protected: solo puede usarse dentro de la clase '" + ownerClass
                      + "' y de sus clases hijas."), lexeme);
        }
    }

    /**
     * Java's rule: of the overloads that accept every argument, those no other
     * one beats by being narrower, so g('c') picks g(int) over g(double). Empty
     * when none accepts them; several when they tie, as f(int, double) and
     * f(double, int) called with (1, 1).
     */
    private List<FunctionSymbol> mostSpecific(List<FunctionSymbol> candidates, List<Expression> arguments,
                                              List<DataType> types)
    {
        List<FunctionSymbol> applicable = candidates.stream()
                .filter(candidate -> accepts(candidate, arguments, types))
                .toList();

        return applicable.stream()
                .filter(candidate -> applicable.stream()
                        .noneMatch(other -> narrower(other, candidate) && !narrower(candidate, other)))
                .toList();
    }

    private boolean accepts(FunctionSymbol candidate, List<Expression> arguments, List<DataType> types)
    {
        for (int i = 0; i < arguments.size(); i++)
        {
            VariableSymbol parameter = candidate.getParameters().get(i);
            Expression     argument  = arguments.get(i);
            DataType       type      = types.get(i);

            if (argument instanceof CompositeLiteralExpression literal)
            {
                // A { } literal fits a single structura parameter that has the attributes it names, or one per value.
                StructSymbol struct = symbolTable.lookupStruct(parameter.getStructName());

                if (parameter.getType() != DataType.ESTRUCTURA || parameter.isArray() || struct == null
                        || (literal.isNamed()
                            ? !struct.getAttributes().keySet().containsAll(literal.getFieldNames())
                            : literal.getValues().size() != struct.getAttributes().size()))
                {
                    return false;
                }
            }
            else if (type != DataType.ERROR
                    && (rankOf(argument) != parameter.getRank() && type != DataType.NULO
                        || !fits(parameter.isArray(), parameter.getType(), parameter.getStructName(), type,
                                 argument.getStructName())))
            {
                return false;
            }
        }
        return true;
    }

    /** Every parameter of the first overload could be passed to the second one. */
    private boolean narrower(FunctionSymbol first, FunctionSymbol second)
    {
        for (int i = 0; i < first.getParameterCount(); i++)
        {
            VariableSymbol mine   = first.getParameters().get(i);
            VariableSymbol theirs = second.getParameters().get(i);

            if (mine.getRank() != theirs.getRank()
                    || !fits(mine.isArray(), theirs.getType(), theirs.getStructName(), mine.getType(),
                             mine.getStructName()))
            {
                return false;
            }
        }
        return true;
    }

    private static String signatures(List<FunctionSymbol> overloads)
    {
        return overloads.stream().map(FunctionSymbol::getSignature).collect(Collectors.joining(", "));
    }

    /** Only an object has methods: an instance of a class imported from a .z file. */
    @Override
    public DataType visitMethodCallExpression(MethodCallExpression node)
    {
        Expression   owner     = node.getOwner();
        DataType     ownerType = owner.accept(this);
        StructSymbol type      = ownerType == DataType.ESTRUCTURA && !isArrayValue(owner)
                                 ? symbolTable.lookupStruct(owner.getStructName()) : null;

        if (type == null || !type.isClass())
        {
            visitArguments(node.getArguments());

            if (ownerType != DataType.ERROR)
            {
                error(node, "El metodo '" + node.getMethodName() + "()' no existe: solo los objetos, "
                        + "cuyas clases se importan de un archivo .z, tienen metodos.",
                        node.getMethodName());
            }
            return typeCheck(node, DataType.ERROR);
        }

        List<FunctionSymbol> overloads = methodsOf(type.getName(), node.getMethodName());

        if (overloads.isEmpty())
        {
            visitArguments(node.getArguments());
            error(node, "La clase '" + type.getName() + "' no tiene el metodo '"
                    + node.getMethodName() + "()'.", node.getMethodName());
            return typeCheck(node, DataType.ERROR);
        }

        FunctionSymbol method = resolve(node, "El metodo", node.getMethodName(), overloads, node.getArguments());

        if (method != null)
        {
            checkAccess(node, "El metodo '" + method.getSignature() + "'", method.getVisibility(),
                    method.getOwnerClass(), node.getMethodName());
        }
        return typed(node, method);
    }

    /** novus / new: the constructor is chosen among the overloads, like any call. */
    @Override
    public DataType visitNewExpression(NewExpression node)
    {
        StructSymbol type = symbolTable.lookupStruct(node.getClassName());

        if (type == null || !type.isClass())
        {
            visitArguments(node.getArguments());
            error(node, "La clase '" + node.getClassName() + "' no existe: los objetos se crean con "
                    + "clases importadas de un archivo .z.", node.getClassName());
            return typeCheck(node, DataType.ERROR);
        }

        FunctionSymbol constructor = resolve(node, "El constructor", node.getClassName(),
                symbolTable.lookupFunctions(type.getName() + "." + type.getName()), node.getArguments());

        if (constructor == null)
        {
            return typeCheck(node, DataType.ERROR);
        }
        checkAccess(node, "El constructor '" + constructor.getSignature() + "'", constructor.getVisibility(),
                type.getName(), node.getClassName());
        node.setConstructor(constructor);
        node.setStructName(type.getName());
        return typeCheck(node, DataType.ESTRUCTURA);
    }

    /** new int[f][c]: every size a numerus, and an element type that exists. */
    @Override
    public DataType visitNewArrayExpression(NewArrayExpression node)
    {
        for (Expression size : node.getDimensions())
        {
            dimension(node, node.getLabel(), size);
        }
        checkTypeName(node, node.getTypeText());

        if (node.getElementType() == DataType.ESTRUCTURA)
        {
            StructSymbol struct = symbolTable.lookupStruct(node.getTypeText());

            if (struct == null)
            {
                error(node, "El tipo '" + node.getTypeText() + "' no existe.", node.getTypeText());
                return typeCheck(node, DataType.ERROR);
            }
            node.setStructName(struct.getName());
        }
        return typeCheck(node, node.getElementType());
    }

    /** this is the object of the method it is written in. */
    @Override
    public DataType visitThisExpression(ThisExpression node)
    {
        if (currentClass == null)
        {
            error(node, "'this' solo existe dentro de los metodos de una clase.", "this");
            return typeCheck(node, DataType.ERROR);
        }
        node.setStructName(currentClass.getName());
        return typeCheck(node, DataType.ESTRUCTURA);
    }

    /** The wider branch gives the type: c ? 1 : 2.5 is decimal, and null fits an object. */
    @Override
    public DataType visitTernaryExpression(TernaryExpression node)
    {
        requireBooleanCondition(node.getCondition(), "?:");

        DataType whenTrue    = operandType(node.getWhenTrue(), "?:");
        DataType whenFalse   = operandType(node.getWhenFalse(), "?:");
        String   trueStruct  = node.getWhenTrue().getStructName();
        String   falseStruct = node.getWhenFalse().getStructName();

        if (whenTrue == DataType.ERROR || whenFalse == DataType.ERROR)
        {
            return typeCheck(node, DataType.ERROR);
        }
        if (whenTrue != DataType.NULO && assignable(whenTrue, trueStruct, whenFalse, falseStruct))
        {
            node.setStructName(trueStruct);
            return typeCheck(node, whenTrue);
        }
        if (assignable(whenFalse, falseStruct, whenTrue, trueStruct))
        {
            node.setStructName(falseStruct);
            return typeCheck(node, whenFalse);
        }
        error(node, "Las ramas del ternario no son compatibles: '" + describe(whenTrue, trueStruct)
                + "' y '" + describe(whenFalse, falseStruct) + "'.", "?:");
        return typeCheck(node, DataType.ERROR);
    }

    /** The error of a misplaced { } belongs to the owner call, not to each argument. */
    private void visitArguments(List<Expression> arguments)
    {
        for (Expression argument : arguments)
        {
            if (!(argument instanceof CompositeLiteralExpression))
            {
                argument.accept(this);
            }
        }
    }

    /** Alone it reads text; checkValue gives it the type of its destination first. */
    @Override
    public DataType visitReadExpression(ReadExpression node)
    {
        return typeCheck(node, DataType.TEXTUM);
    }

    /** Solo se alcanza con un { } fuera de lugar: checkValue desvia los validos. */
    @Override
    public DataType visitCompositeLiteralExpression(CompositeLiteralExpression node)
    {
        error(node, "Un literal '{ ... }' solo puede usarse al declarar o asignar una "
                + structWord() + ", o como valores iniciales de un arreglo.", "{}");

        for (Expression value : node.getValues())
        {
            value.accept(this);
        }
        return typeCheck(node, DataType.ERROR);
    }

    /* =================================================================
     * Registration (pass 1)
     * ================================================================= */
    /** A class is registered as the structure of its attributes plus the signatures of its methods. */
    private void registerClass(ClassDeclaration node)
    {
        registerStruct(node.getLayout(), true);

        for (FunctionDeclaration method : node.getMethods())
        {
            registerFunctionSignature(method);
        }
    }

    private void registerStruct(StructDeclaration node, boolean classType)
    {
        requireDeclarationPlace(node, node.getName());

        // Enunciado: PigLatin ya no define estructuras propias. Se registra igual,
        // para que cada uso posterior no sume un "el tipo no existe".
        if (language == Language.PIG)
        {
            error(node, "La structura '" + node.getName() + "' debe declararse en un archivo .y "
                    + "e importarse: PigLatin ya no define estructuras propias.", node.getName());
        }

        // "textum" names a primitive in PigLatin: as a Y? struct it could never be used from there.
        if (DataType.fromText(node.getName()) != DataType.ESTRUCTURA)
        {
            error(node, "'" + node.getName() + "' es el nombre de un tipo primitivo: la "
                    + (classType ? "clase" : structWord())
                    + " necesita otro nombre.", node.getName());
            return;
        }
        // Also one visible from an outer scope: two layouts under one name would be taken for each other.
        if (symbolTable.lookupLocal(node.getName()) != null || symbolTable.lookupStruct(node.getName()) != null)
        {
            error(node, "La " + (classType ? "clase" : structWord())
                    + " '" + node.getName() + "' ya fue declarada.", node.getName());
            return;
        }

        StructSymbol struct = new StructSymbol(node.getName(), symbolTable.getCurrentScopeName(),
                classType, node.getLine(), node.getColumn());

        for (StructField field : node.getFields())
        {
            checkTypeName(field, field.getTypeText());

            VariableSymbol attribute = new VariableSymbol(field.getName(), field.getType(),
                    field.getTypeText(), SymbolCategory.ATTRIBUTE, node.getName(), false,
                    structNameOf(field.getType(), field.getTypeText()), field.getRank(),
                    field.getLine(), field.getColumn());

            attribute.setVisibility(field.getVisibility());
            if (!struct.addAttribute(attribute))
            {
                error(field, "El atributo '" + field.getName()
                        + "' esta repetido en la " + (classType ? "clase" : structWord()) + " '"
                        + node.getName() + "'.",
                        field.getName());
            }
        }
        symbolTable.declare(struct);
    }

    /** Paso 2: ya estan todas registradas, un campo puede citar una declarada despues. */
    private void validateStructFieldTypes(StructDeclaration node)
    {
        for (StructField field : node.getFields())
        {
            if (field.getType() == DataType.ESTRUCTURA
                    && symbolTable.lookupStruct(field.getTypeText()) == null)
            {
                error(field, "El tipo '" + field.getTypeText() + "' del atributo '"
                        + field.getName() + "' no existe.", field.getTypeText());
            }
            // Y? writes the size of an array field: it is reserved with the structure.
            if (language == Language.Y && field.isArray() && field.getSize() < 1)
            {
                error(field, "El tamano del arreglo '" + field.getName() + "' debe ser un entero entre 1 y "
                        + Integer.MAX_VALUE + ".", field.getName());
            }
        }
    }

    private void registerFunctionSignature(FunctionDeclaration node)
    {
        List<VariableSymbol> parameters = new ArrayList<>();

        for (Parameter parameter : node.getParameters())
        {
            parameters.add(new VariableSymbol(parameter.getName(), parameter.getType(),
                    parameter.getTypeText(), SymbolCategory.PARAMETER, node.getName(), true,
                    structNameOf(parameter.getType(), parameter.getTypeText()), parameter.getRank(),
                    parameter.getLine(), parameter.getColumn()));
        }

        // A method is named after its class: Pila.apilar never meets a free apilar.
        String         name     = node.getOwnerClass() == null ? node.getName()
                                  : node.getOwnerClass() + "." + node.getName();
        FunctionSymbol function = new FunctionSymbol(name, node.getReturnType(),
                node.getReturnTypeText(), symbolTable.getCurrentScopeName(), parameters,
                node.returnsValue(), structNameOf(node.getReturnType(), node.getReturnTypeText()),
                node.getOwnerClass(), node.isConstructor(), node.getLine(), node.getColumn());

        function.setVisibility(node.getVisibility());

        // Sobrecarga permitida: solo choca otra version con los mismos tipos.
        if (!symbolTable.declareFunction(function))
        {
            error(node, functionWord(node, true) + " '" + function.getSignature()
                    + (node.getOwnerClass() == null ? "' ya fue declarada" : "' ya fue declarado")
                    + " con los mismos parametros.", node.getName());
            return;
        }
        node.setSymbol(function);
    }

    /* =================================================================
     * Structure literals
     * ================================================================= */

    /**
     * Valida un literal contra su structura, en sus dos formas: con nombre,
     * { nombre: "Yennifer", edad: 999 }, donde el orden no importa, o
     * posicional, {"Valeria", 25, {"Avenida Central", 500}}, que es la del
     * enunciado y va en el orden en que se declararon los atributos.
     */
    private void validateStructLiteral(CompositeLiteralExpression literal, StructSymbol struct)
    {
        if (struct == null)
        {
            return;
        }
        if (literal.isNamed())
        {
            validateNamedLiteral(literal, struct);
            return;
        }

        List<VariableSymbol> attributes = new ArrayList<>(struct.getAttributes().values());
        List<Expression>     values     = literal.getValues();

        if (values.size() != attributes.size())
        {
            error(literal, "La " + structWord() + " '" + struct.getName() + "' tiene " + attributes.size()
                    + " atributo(s) y el literal trae " + values.size() + " valor(es).",
                    struct.getName());
        }
        for (int i = 0; i < Math.min(values.size(), attributes.size()); i++)
        {
            validateAttributeValue(values.get(i), attributes.get(i));
        }
    }

    /**
     * El mapa 'pending' cumple las tres reglas a la vez: nombre obligatorio,
     * todos los atributos presentes, y el orden no importa.
     */
    private void validateNamedLiteral(CompositeLiteralExpression literal, StructSymbol struct)
    {
        Map<String, VariableSymbol> pending = new LinkedHashMap<>(struct.getAttributes());

        for (int i = 0; i < literal.getFieldNames().size(); i++)
        {
            String         fieldName = literal.getFieldNames().get(i);
            Expression     value     = literal.getValues().get(i);
            VariableSymbol attribute = pending.remove(fieldName);

            if (attribute == null)
            {
                error(value, "'" + fieldName + "' no es un atributo de '"
                        + struct.getName() + "'.", fieldName);
                continue;
            }
            validateAttributeValue(value, attribute);
        }

        for (String missing : pending.keySet())
        {
            error(literal, "Falta el atributo obligatorio '" + missing + "' de '"
                    + struct.getName() + "'.", missing);
        }
    }

    /** One value of a literal against the attribute it fills. A nested { } recurses through checkValue. */
    private void validateAttributeValue(Expression value, VariableSymbol attribute)
    {
        if (attribute.isArray())
        {
            validateArrayAttribute(value, attribute);
            return;
        }

        DataType valueType = checkValue(value, attribute.getType(), attribute.getStructName(), false);

        if (!assignable(attribute.getType(), attribute.getStructName(), valueType,
                        value.getStructName()))
        {
            error(value, "El atributo '" + attribute.getName() + "' es '" + attribute.getTypeText()
                    + "' y recibio '" + describe(valueType, value.getStructName()) + "'.",
                    attribute.getName());
        }
    }

    /**
     * An array attribute takes either an existing array of the same element
     * type, the form of the statement ({"Carlos", mis_enteros}), or the
     * PigLatin form "animales: Animal[7]" that creates one of that size.
     */
    private void validateArrayAttribute(Expression value, VariableSymbol attribute)
    {
        // "Animal[7]" indexa el nombre del TIPO, no una variable: no se resuelve
        // contra la tabla de simbolos, solo se valida el tamano.
        if (value instanceof ArrayAccessExpression access
                && access.getArray() instanceof IdentifierExpression typeName
                && typeName.getName().equals(attribute.getTypeText()))
        {
            DataType sizeType = access.getIndex().accept(this);
            Integer  size     = extractInteger(access.getIndex());

            if (size != null && size < 0)
            {
                error(access, "El tamano del arreglo '" + attribute.getName() + "' no puede ser negativo.",
                        attribute.getName());
            }
            if (sizeType != DataType.NUMERUS && sizeType != DataType.ERROR)
            {
                error(access, "El tamano del arreglo '" + attribute.getName()
                        + "' debe ser '" + describe(DataType.NUMERUS, null) + "', se recibio '"
                        + describe(sizeType, null) + "'.", attribute.getName());
            }

            typeName.setComputedType(attribute.getType());
            typeName.setStructName(attribute.getStructName());
            access.setComputedType(attribute.getType());
            access.setStructName(attribute.getStructName());
            return;
        }

        DataType valueType = checkValue(value, attribute.getType(), attribute.getStructName(), true);

        if (!fits(true, attribute.getType(), attribute.getStructName(), valueType, value.getStructName()))
        {
            error(value, "El atributo '" + attribute.getName() + "' es un arreglo de '"
                    + attribute.getTypeText() + "' y recibio '"
                    + describe(valueType, value.getStructName()) + "'.", attribute.getName());
        }
    }

    /* =================================================================
     * Support
     * ================================================================= */

    /**
     * Verifica un valor contra su destino: desvia el literal { } al validador
     * de structuras, le da a leer() el tipo que se espera, y es el UNICO lugar
     * que distingue un arreglo completo de un valor.
     */
    private DataType checkValue(Expression value, DataType targetType, String targetStruct,
                                boolean targetIsArray)
    {
        if (value instanceof CompositeLiteralExpression literal)
        {
            if (targetIsArray)
            {
                error(literal, "Un literal '{ ... }' no puede asignarse a un arreglo completo: "
                        + "asigne cada posicion por separado.", "{}");
                return typeCheck(literal, DataType.ERROR);
            }
            StructSymbol struct = symbolTable.lookupStruct(targetStruct);

            if (targetType == DataType.ESTRUCTURA && struct != null && struct.isClass())
            {
                error(literal, "Un objeto de '" + targetStruct + "' se crea con "
                        + keyword("novus", "novus", "new") + ": un literal '{ ... }' no ejecuta su constructor.", "{}");
                return typeCheck(literal, DataType.ERROR);
            }
            if (targetType == DataType.ESTRUCTURA)
            {
                validateStructLiteral(literal, struct);
                literal.setComputedType(DataType.ESTRUCTURA);
                literal.setStructName(targetStruct);
                return DataType.ESTRUCTURA;
            }
        }
        if (value instanceof ReadExpression read && targetType.isPrimitive() && !targetIsArray)
        {
            return typeCheck(read, targetType);
        }
        return checkShape(value, value.accept(this), targetIsArray);
    }

    /** A whole array only goes where an array is expected, and the other way round. */
    private DataType checkShape(Expression value, DataType type, boolean targetIsArray)
    {
        if (type == DataType.ERROR || isArrayValue(value) == targetIsArray
                || targetIsArray && type == DataType.NULO)
        {
            return type;
        }

        String name = nameOf(value);

        error(value, targetIsArray
                ? "Se esperaba un arreglo y se recibio un valor '" + describe(type, value.getStructName()) + "'."
                : "El arreglo '" + name + "' no es un valor primitivo; use '" + name + "[indice]'.",
                name);
        return typeCheck(value, DataType.ERROR);
    }

    /**
     * Enunciado: "no se consideran primitivos los arreglos de datos". El nombre
     * pelado de un arreglo se indexa, no se opera.
     */
    private DataType operandType(Expression operand, String operator)
    {
        DataType type = operand.accept(this);

        if (type != DataType.ERROR && isArrayValue(operand))
        {
            String name = nameOf(operand);

            error(operand, "El arreglo '" + name + "' no es un valor primitivo y no puede operarse "
                    + "con '" + operator + "'; use '" + name + "[indice]'.", name);
            return typeCheck(operand, DataType.ERROR);
        }
        return type;
    }

    /**
     * Is this a whole array rather than one of its elements? A name bound to an
     * array, or an array attribute. Only meaningful once the expression was visited.
     */
    private boolean isArrayValue(Expression expression)
    {
        if (expression instanceof IdentifierExpression identifier)
        {
            // An array attribute used from inside a method is a VariableSymbol with the flag.
            return identifier.getSymbol() instanceof ArraySymbol
                    || identifier.getSymbol() instanceof VariableSymbol variable && variable.isArray();
        }
        if (expression instanceof MemberAccessExpression member)
        {
            VariableSymbol attribute = attributeOf(member);

            return attribute != null && attribute.isArray();
        }
        return expression instanceof NewArrayExpression;
    }

    /** The attribute a member access names, once its owner was visited; null when there is none. */
    private VariableSymbol attributeOf(MemberAccessExpression member)
    {
        StructSymbol struct = symbolTable.lookupStruct(member.getOwner().getStructName());

        return struct == null ? null : struct.getAttribute(member.getMemberName());
    }

    /** Dimensions of a whole array; 0 for any other value. */
    private int rankOf(Expression expression)
    {
        return isArrayValue(expression) ? dimensionsOf(expression).size() : 0;
    }

    /** How an expression is named in a message: the variable or attribute, when there is one. */
    private static String nameOf(Expression expression)
    {
        if (expression instanceof IdentifierExpression identifier)
        {
            return identifier.getName();
        }
        if (expression instanceof MemberAccessExpression member)
        {
            return member.getMemberName();
        }
        return expression.getLabel();
    }

    /**
     * Same as TypeSystem.isAssignable, plus the rule TypeSystem cannot express:
     * two different structuras are both ESTRUCTURA, but a Persona is not a Carro.
     */
    private boolean assignable(DataType target, String targetStruct,
                               DataType source, String sourceStruct)
    {
        if (!TypeSystem.isAssignable(target, source))
        {
            return false;
        }
        if (target == DataType.ESTRUCTURA && source == DataType.ESTRUCTURA)
        {
            return targetStruct != null && targetStruct.equals(sourceStruct);
        }
        return true;
    }

    /**
     * assignable(), except for a whole array: it is shared, not copied, so a
     * numerus array seen as decimalis would let 2.5 into a numerus cell. Its
     * elements need the exact type.
     */
    private boolean fits(boolean wholeArray, DataType target, String targetStruct,
                         DataType source, String sourceStruct)
    {
        if (!wholeArray)
        {
            return assignable(target, targetStruct, source, sourceStruct);
        }
        return target == DataType.ERROR || source == DataType.ERROR || source == DataType.NULO
                || target == source && Objects.equals(targetStruct, sourceStruct);
    }

    private void requireBooleanCondition(Expression condition, String statement)
    {
        if (condition == null)
        {
            return;
        }

        DataType type = condition.accept(this);

        if (type != DataType.BOOLEANO && type != DataType.ERROR)
        {
            error(condition, "La condicion de '" + statement
                    + "' debe ser booleana, se recibio '" + describe(type, null) + "'.", statement);
        }
    }

    /**
     * Enunciado de PigLatin: las variables solo se definen al principio de la
     * funcion. Reporta, pero el simbolo se declara igual para no encadenar un
     * "no declarada" en cada uso posterior. Y? declara en cualquier parte.
     *
     * MAIOR> queda fuera a proposito (desviacion declarada en docs/05): alli
     * visitProgram nunca levanta la bandera, y los si / ciclos la heredan.
     */
    private void requireDeclarationPlace(AstNode node, String name)
    {
        if (declarationsForbidden && language == Language.PIG)
        {
            error(node, "'" + name + "' no puede declararse aqui. Las declaraciones van "
                    + "al inicio de la funcion, en la seccion VARIABILES[ ].", name);
        }
    }

    /**
     * m = w and f(m): a whole array only goes where one of the same rank goes.
     * Its sizes travel with it, in the heap, so its rows may be of any length.
     */
    private void checkArrayShape(Expression value, int expectedRank)
    {
        int rank = rankOf(value);

        if (rank > 0 && rank != expectedRank)
        {
            error(value, "El arreglo '" + nameOf(value) + "' tiene " + rank + " dimension(es) y aqui "
                    + "se espera uno de " + expectedRank + ".", nameOf(value));
        }
    }

    private static boolean isNull(Expression expression)
    {
        return expression instanceof LiteralExpression literal && literal.getType() == DataType.NULO;
    }

    /** A whole array, visited: an array compared against null. */
    private boolean isArrayOperand(Expression expression)
    {
        return expression.accept(this) != DataType.ERROR && isArrayValue(expression);
    }

    /** "int" is a type in Zetariano only: each language accepts its own primitive names. */
    private void checkTypeName(AstNode node, String typeText)
    {
        if (typeText != null && DataType.fromText(typeText) != DataType.ESTRUCTURA
                && !language.hasTypeName(typeText))
        {
            error(node, "El tipo '" + typeText + "' no existe.", typeText);
        }
    }

    private void walkLoopBody(Block body)
    {
        loopDepth++;
        body.accept(this);
        loopDepth--;
    }

    /** The same statement is spelled differently in each language: the message echoes the user's. */
    private String keyword(String pig, String y, String z)
    {
        return switch (language)
        {
            case Y   -> y;
            case Z   -> z;
            default  -> pig;
        };
    }

    /** A structure in a message, named as the file being read names it. */
    private String structWord()
    {
        return keyword("structura", "estructura", "estructura");
    }

    /**
     * Constant value of an integer expression, or null when it is not constant.
     * Integer and not int: -1 is a valid index, so it cannot double as the
     * "not constant" marker.
     */
    private Integer extractInteger(Expression expression)
    {
        if (expression instanceof LiteralExpression literal
                && literal.getType() == DataType.NUMERUS)
        {
            try
            {
                return Integer.parseInt(literal.getText());
            }
            catch (NumberFormatException ignored)
            {
                return null;   // out of int range
            }
        }
        if (expression instanceof UnaryExpression unary && "-".equals(unary.getOperator()))
        {
            Integer value = extractInteger(unary.getOperand());
            return value == null || value == Integer.MIN_VALUE ? null : -value;
        }
        // Statement: an index "que se puede evaluar al hacer la verificacion
        // de semantica" has to be range checked, so numeros[1 + 1] is folded.
        if (expression instanceof BinaryExpression binary)
        {
            Integer left  = extractInteger(binary.getLeft());
            Integer right = extractInteger(binary.getRight());

            if (left == null || right == null)
            {
                return null;
            }

            try
            {
                return switch (binary.getOperator())
                {
                    case "+" -> Math.addExact(left, right);
                    case "-" -> Math.subtractExact(left, right);
                    case "*" -> Math.multiplyExact(left, right);
                    case "/" -> Math.divideExact(left, right);
                    default  -> null;
                };
            }
            catch (ArithmeticException overflow)
            {
                return null;   // 65536 * 65536 does not fit an int, x / 0 has no value: nothing it can check
            }
        }
        return null;
    }

    /**
     * Como se nombra un tipo en un mensaje: "Carro", no "structura", y con las
     * palabras del lenguaje que se esta leyendo: int en un .z, entero en un .y.
     */
    private String describe(DataType type, String structName)
    {
        if (type == DataType.ESTRUCTURA && structName != null)
        {
            return structName;
        }
        return switch (language)
        {
            case Y -> switch (type)
            {
                case NUMERUS   -> "entero";
                case DECIMALIS -> "flotante";
                case TEXTUM    -> "cadena";
                case LITTERA   -> "caracter";
                default        -> type.toString();
            };
            case Z -> switch (type)
            {
                case NUMERUS   -> "int";
                case DECIMALIS -> "double";
                case TEXTUM    -> "String";
                case LITTERA   -> "char";
                case BOOLEANO  -> "boolean";
                default        -> type.toString();
            };
            default -> type.toString();
        };
    }

    /** "La funcion" or "El metodo": a Zetariano message speaks of methods. */
    private static String functionWord(FunctionDeclaration function, boolean capitalized)
    {
        String word = function.getOwnerClass() == null ? "la funcion" : "el metodo";

        return capitalized ? Character.toUpperCase(word.charAt(0)) + word.substring(1) : word;
    }

    private String structNameOf(DataType type, String typeText)
    {
        return type == DataType.ESTRUCTURA ? typeText : null;
    }

    private DataType typeCheck(Expression node, DataType type)
    {
        node.setComputedType(type);
        return type;
    }

    private void error(AstNode node, String description, String lexeme)
    {
        errorManager.addSemantic(description, lexeme, node.getLine(), node.getColumn());
    }
}
