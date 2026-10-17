package com.dmaldonado.st4ck.over.pig.model.analysis;

import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.column;
import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.constant;
import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.fold;
import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.line;
import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.missing;

import com.dmaldonado.st4ck.over.pig.grammar.ZParser;
import com.dmaldonado.st4ck.over.pig.grammar.ZParserBaseVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import com.dmaldonado.st4ck.over.pig.model.ast.Language;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ArrayDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ClassDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.FunctionDeclaration;
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
import com.dmaldonado.st4ck.over.pig.model.ast.statement.PrintStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ReturnStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.SwitchStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.WhileStatement;
import com.dmaldonado.st4ck.over.pig.model.symbols.Visibility;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Builds the common AST from Zetariano's parse tree. The class becomes a
 * ClassDeclaration: its attributes are laid out like a structure, and its
 * constructors and methods are functions owned by the class.
 *
 * Desugared here, so no later phase needs a node of its own:
 *   x += e                 x = x + e
 *   int total = 0;         the first statement of every constructor
 *   no constructor         an empty one, as in Java
 *   if (c) una;            the single statement wrapped in a Block
 *
 * Same rules as the other builders: a rule with a single child returns it
 * (ANTLR's default), and a visitX whose parse tree was repaired returns null,
 * and its parent leaves it out.
 */
public class ZAstBuilder extends ZParserBaseVisitor<AstNode>
{
    /** The method that holds the attribute initializers: no one can write that name, so no one calls it. */
    private static final String INITIALIZERS = "<atributos>";

    /** Class being built: every constructor and method belongs to it. */
    private String className;

    public Program build(ZParser.ProgramaContext parseTree)
    {
        return visit(parseTree) instanceof Program program ? program : null;
    }

    /* =================================================================
     * CLASS
     * ================================================================= */
    @Override
    public AstNode visitPrograma(ZParser.ProgramaContext ctx)
    {
        List<AstNode> classes = new ArrayList<>();

        add(classes, child(ctx.declaracionClase()));
        return new Program(Language.Z, List.of(), classes, List.of(), List.of(), line(ctx), column(ctx));
    }

    @Override
    public AstNode visitDeclaracionClase(ZParser.DeclaracionClaseContext ctx)
    {
        if (missing(ctx.ID()))
        {
            return null;
        }
        className = ctx.ID().getText();

        List<StructField>         fields       = new ArrayList<>();
        List<AstNode>             initializers = new ArrayList<>();
        List<FunctionDeclaration> methods      = new ArrayList<>();

        for (ZParser.MiembroContext member : ctx.miembro())
        {
            AstNode built = member instanceof ZParser.AtributoContext attribute
                    ? attribute(attribute, initializers)
                    : visit(member);

            if (built instanceof StructField field)
            {
                fields.add(field);
            }
            else if (built instanceof FunctionDeclaration method)
            {
                methods.add(method);
            }
        }

        List<FunctionDeclaration> members = withInitializers(methods, initializers, ctx);
        StructDeclaration         layout  = new StructDeclaration(className, fields, line(ctx), column(ctx));

        return new ClassDeclaration(className, layout, members, line(ctx), column(ctx));
    }

    /**
     * The attribute initializers run first in every constructor, as Java does;
     * without a constructor, Java's empty default. They live in a method of
     * their own that each constructor calls: there a constructor parameter
     * cannot stand in for an attribute of the same name, and each initializer
     * is analysed once.
     */
    private List<FunctionDeclaration> withInitializers(List<FunctionDeclaration> methods,
                                                       List<AstNode> initializers,
                                                       ParserRuleContext ctx)
    {
        List<FunctionDeclaration> members = new ArrayList<>();

        if (methods.stream().noneMatch(FunctionDeclaration::isConstructor))
        {
            members.add(constructor(className, new ArrayList<>(),
                    new Block(new ArrayList<>(), line(ctx), column(ctx)), Visibility.DEFAULT, line(ctx), column(ctx)));
        }
        members.addAll(methods);

        if (initializers.isEmpty())
        {
            return members;
        }

        AstNode call = new ExpressionStatement(new FunctionCallExpression(INITIALIZERS, List.of(),
                line(ctx), column(ctx)), line(ctx), column(ctx));

        members = new ArrayList<>(members.stream()
                .map(method -> !method.isConstructor() ? method
                        : constructor(method.getName(), method.getParameters(),
                                prepend(List.of(call), method.getBody()), method.getVisibility(), method.getLine(),
                                method.getColumn()))
                .toList());
        members.add(new FunctionDeclaration(INITIALIZERS, "void", List.of(), List.of(),
                new Block(initializers, line(ctx), column(ctx)), false, className, false, Visibility.PRIVATE,
                line(ctx), column(ctx)));
        return members;
    }

    private FunctionDeclaration constructor(String name, List<Parameter> parameters, Block body,
                                            Visibility visibility, int line, int column)
    {
        return new FunctionDeclaration(name, "void", parameters, List.of(), body, false, className,
                true, visibility, line, column);
    }

    /** No modifier is Java's default. */
    private static Visibility visibility(ZParser.VisibilidadContext ctx)
    {
        if (ctx == null)
        {
            return Visibility.DEFAULT;
        }
        return ctx.PRIVATE() != null ? Visibility.PRIVATE
             : ctx.PROTECTED() != null ? Visibility.PROTECTED : Visibility.PUBLIC;
    }

    private static Block prepend(List<AstNode> statements, Block body)
    {
        List<AstNode> all = new ArrayList<>(statements);

        all.addAll(body.getStatements());
        return new Block(all, body.getLine(), body.getColumn());
    }

    @Override
    public AstNode visitConstructor(ZParser.ConstructorContext ctx)
    {
        Block body = block(ctx.bloque());

        return (missing(ctx.ID()) || body == null) ? null
                : constructor(ctx.ID().getText(), parameters(ctx.listaParametros()), body,
                        visibility(ctx.visibilidad()), line(ctx), column(ctx));
    }

    @Override
    public AstNode visitMetodo(ZParser.MetodoContext ctx)
    {
        Block   body         = block(ctx.bloque());
        boolean returnsValue = ctx.VOID() == null;

        if (missing(ctx.ID()) || body == null || (returnsValue && missing(ctx.tipo())))
        {
            return null;
        }
        return new FunctionDeclaration(ctx.ID().getText(), returnsValue ? ctx.tipo().getText() : "void",
                parameters(ctx.listaParametros()), List.of(), body, returnsValue, className, false,
                visibility(ctx.visibilidad()), line(ctx), column(ctx));
    }

    /**
     * An attribute is a field of the object's layout. Its initial value turns
     * into an assignment for the constructors; an array attribute with a
     * constant size is reserved together with the object.
     */
    private StructField attribute(ZParser.AtributoContext ctx, List<AstNode> initializers)
    {
        if (missing(ctx.ID(), ctx.tipo()))
        {
            return null;
        }

        String                       name        = ctx.ID().getText();
        ZParser.InicializadorContext initializer = ctx.inicializador();
        int                          rank        = rank(ctx.tipo());
        boolean                      array       = rank > 0;
        int                          size        = -1;
        boolean                      repeated    = initializers.stream().anyMatch(built ->
                built instanceof Assignment assignment && assignment.getTarget() instanceof MemberAccessExpression
                        member && member.getMemberName().equals(name));

        if (repeated)
        {
            // A repeated attribute is reported by the analyzer; its value would only add noise.
        }
        else if (rank > 1 && initializer != null)
        {
            // A matrix is created when the object is: its sizes may be variables, its rows a literal.
            AstNode matrix = array(name, baseType(ctx.tipo()), rank, initializer, ctx);

            if (matrix != null)
            {
                initializeThroughLocal(name, matrix, ctx, initializers);
            }
        }
        else if (initializer instanceof ZParser.InicializadorNuevoArregloContext created)
        {
            String createdType = created.tipoBase().getText();

            size = rank == 1 && created.expresion().size() == 1 && createdType.equals(baseType(ctx.tipo()))
                   ? constant(created.expresion(0)) : -1;

            // new int[cap] is only known when the object is created, and a mismatch is reported there.
            if (size < 0)
            {
                List<Expression> dimensions = expressions(created.expresion());

                if (dimensions != null)
                {
                    initializeThroughLocal(name, new ArrayDeclaration(name, dimensions, rank, baseType(ctx.tipo()),
                            List.of(), createdType, line(ctx), column(ctx)), ctx, initializers);
                }
            }
        }
        else if (initializer instanceof ZParser.InicializadorLiteralContext literal && array)
        {
            List<Expression> values = elements(literal.literalArreglo());

            if (values == null)
            {
                return null;
            }
            size = values.size();

            for (int i = 0; i < values.size(); i++)
            {
                Expression position = new LiteralExpression(String.valueOf(i), DataType.NUMERUS,
                        line(ctx), column(ctx));

                initializers.add(new Assignment(new ArrayAccessExpression(attributeOf(name, ctx),
                        position, line(ctx), column(ctx)), values.get(i), line(ctx), column(ctx)));
            }
        }
        else if (initializer != null)
        {
            Expression value = initialValue(initializer);

            // A broken value was already reported: the attribute still exists.
            if (value != null)
            {
                initializers.add(new Assignment(attributeOf(name, ctx), value, line(ctx), column(ctx)));
            }
        }
        return new StructField(name, baseType(ctx.tipo()), rank, size, visibility(ctx.visibilidad()), line(ctx),
                column(ctx));
    }

    /**
     * A local with the attribute's name, analysed as any declaration, then stored in it: how an
     * array whose size is only known when the object is created gets into the object.
     */
    private static void initializeThroughLocal(String name, AstNode local, ParserRuleContext ctx,
                                               List<AstNode> initializers)
    {
        initializers.add(local);
        initializers.add(new Assignment(attributeOf(name, ctx),
                new IdentifierExpression(name, line(ctx), column(ctx)), line(ctx), column(ctx)));
    }

    /** this.name, reached through the object even where a parameter has the same name. */
    private static Expression attributeOf(String name, ParserRuleContext ctx)
    {
        return new MemberAccessExpression(new ThisExpression(line(ctx), column(ctx)), name,
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitParametro(ZParser.ParametroContext ctx)
    {
        if (missing(ctx.ID(), ctx.tipo()))
        {
            return null;
        }
        return new Parameter(ctx.ID().getText(), baseType(ctx.tipo()), rank(ctx.tipo()), false,
                line(ctx), column(ctx));
    }

    /* =================================================================
     * STATEMENTS
     * ================================================================= */
    @Override
    public AstNode visitBloque(ZParser.BloqueContext ctx)
    {
        List<AstNode> statements = new ArrayList<>();

        for (ZParser.InstruccionContext statement : ctx.instruccion())
        {
            add(statements, visit(statement));
        }
        return new Block(statements, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionDeclaracion(ZParser.InstruccionDeclaracionContext ctx)
    {
        return child(ctx.declaracionLocal());
    }

    @Override
    public AstNode visitInstruccionAsignacion(ZParser.InstruccionAsignacionContext ctx)
    {
        return child(ctx.asignacion());
    }

    /** "int[] a" is an array whether its size comes from new, from its values, or not at all. */
    @Override
    public AstNode visitDeclaracionLocal(ZParser.DeclaracionLocalContext ctx)
    {
        if (missing(ctx.ID(), ctx.tipo()))
        {
            return null;
        }

        String                       name        = ctx.ID().getText();
        String                       type        = baseType(ctx.tipo());
        int                          rank        = rank(ctx.tipo());
        ZParser.InicializadorContext initializer = ctx.inicializador();

        if (rank > 0 || initializer instanceof ZParser.InicializadorNuevoArregloContext)
        {
            return array(name, type, rank, initializer, ctx);
        }

        // A broken value was already reported: the variable is still declared, or every use cascades.
        Expression value = initializer == null ? null : initialValue(initializer);

        return new VariableDeclaration(name, type, value, line(ctx), column(ctx));
    }

    private AstNode array(String name, String type, int rank, ZParser.InicializadorContext initializer,
                          ParserRuleContext ctx)
    {
        List<Expression> dimensions = new ArrayList<>();
        List<Expression> values     = new ArrayList<>();

        String createdType = type;

        // The declared rank stays: new int[2][3] into an int[] is reported, not taken as a matrix.
        if (initializer instanceof ZParser.InicializadorNuevoArregloContext created)
        {
            dimensions  = expressions(created.expresion());
            createdType = created.tipoBase().getText();
        }
        else if (initializer instanceof ZParser.InicializadorLiteralContext literal)
        {
            values     = elements(literal.literalArreglo());
            dimensions = values == null ? null : shape(values, rank, ctx);
        }
        else if (initializer != null)
        {
            Expression value = initialValue(initializer);

            values = value == null ? null : new ArrayList<>(List.of(value));
        }

        if (dimensions == null || values == null)
        {
            return null;
        }
        return new ArrayDeclaration(name, dimensions, rank, type, values, createdType, line(ctx), column(ctx));
    }

    /** Java takes the size of every dimension from the literal: the first row of each level. */
    private static List<Expression> shape(List<Expression> values, int rank, ParserRuleContext ctx)
    {
        List<Expression> dimensions = new ArrayList<>();
        List<Expression> level      = values;

        for (int i = 0; i < rank; i++)
        {
            dimensions.add(new LiteralExpression(String.valueOf(level.size()), DataType.NUMERUS,
                    line(ctx), column(ctx)));
            level = !level.isEmpty() && level.get(0) instanceof CompositeLiteralExpression row
                    ? row.getValues() : List.of();
        }
        return dimensions;
    }

    /** The value of a non array initializer; a stray { } stays a literal for the analyzer to report. */
    private Expression initialValue(ZParser.InicializadorContext ctx)
    {
        if (ctx instanceof ZParser.InicializadorLiteralContext literal)
        {
            return expression(literal.literalArreglo());
        }
        if (ctx instanceof ZParser.InicializadorExpresionContext value)
        {
            return expression(value.expresion());
        }
        return null;
    }

    @Override
    public AstNode visitLiteralArreglo(ZParser.LiteralArregloContext ctx)
    {
        List<Expression> values = elements(ctx);

        return values == null ? null : new CompositeLiteralExpression(null, values, line(ctx), column(ctx));
    }

    private List<Expression> elements(ZParser.LiteralArregloContext ctx)
    {
        List<Expression> values = new ArrayList<>();

        for (ZParser.ElementoArregloContext element : ctx.elementoArreglo())
        {
            Expression value = expression(element.getChild(0));

            if (value == null)
            {
                return null;
            }
            values.add(value);
        }
        return values;
    }

    /**
     * "x op= e" is "x = x op e" with ONE x: the operation shares the target node,
     * so x[i++] += e evaluates its index once, as Java does.
     */
    @Override
    public AstNode visitAsignacion(ZParser.AsignacionContext ctx)
    {
        Expression target = expression(ctx.destino());
        Expression value  = expression(ctx.expresion());

        if (target == null || value == null || ctx.op == null)
        {
            return null;
        }
        if (ctx.op.getType() != ZParser.ASIGNACION)
        {
            String operator = ctx.op.getText().substring(0, 1);

            value = new BinaryExpression(target, operator, value, line(ctx), column(ctx));
        }
        return new Assignment(target, value, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitDestino(ZParser.DestinoContext ctx)
    {
        if (missing(ctx.ID()))
        {
            return null;
        }

        Expression current = ctx.THIS() != null
                ? new MemberAccessExpression(new ThisExpression(line(ctx), column(ctx)),
                        ctx.ID().getText(), line(ctx), column(ctx))
                : new IdentifierExpression(ctx.ID().getText(), line(ctx), column(ctx));

        for (ZParser.SufijoDestinoContext suffix : ctx.sufijoDestino())
        {
            if (suffix instanceof ZParser.AccesoIndiceContext index)
            {
                Expression position = expression(index.expresion());

                if (position == null)
                {
                    return null;
                }
                current = new ArrayAccessExpression(current, position, line(index), column(index));
            }
            else
            {
                ZParser.AccesoAtributoContext member = (ZParser.AccesoAtributoContext) suffix;

                if (missing(member.ID()))
                {
                    return null;
                }
                current = new MemberAccessExpression(current, member.ID().getText(),
                        line(member), column(member));
            }
        }
        return current;
    }

    /** The else binds to the nearest if: the grammar's optional ELSE is greedy. */
    @Override
    public AstNode visitInstruccionSi(ZParser.InstruccionSiContext ctx)
    {
        Expression condition  = expression(ctx.expresion());
        Block      thenBranch = body(ctx.instruccion(0));
        AstNode    elseBranch = null;

        if (ctx.ELSE() != null)
        {
            AstNode written = child(ctx.instruccion(1));

            // else if stays an IfStatement: the same chain the other languages build.
            elseBranch = written instanceof IfStatement ? written : body(ctx.instruccion(1));

            if (elseBranch == null)
            {
                return null;
            }
        }
        return (condition == null || thenBranch == null) ? null
                : new IfStatement(condition, thenBranch, elseBranch, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionSwitch(ZParser.InstruccionSwitchContext ctx)
    {
        Expression       discriminant = expression(ctx.expresion());
        List<CaseClause> cases        = new ArrayList<>();

        if (discriminant == null)
        {
            return null;
        }
        for (ZParser.CasoSwitchContext clause : ctx.casoSwitch())
        {
            if (!(visit(clause) instanceof CaseClause built))
            {
                return null;
            }
            cases.add(built);
        }
        return new SwitchStatement(discriminant, cases, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitCaso(ZParser.CasoContext ctx)
    {
        Expression value = expression(ctx.expresion());
        Block      body  = statements(ctx.instruccion(), ctx);

        return (value == null || body == null) ? null : new CaseClause(value, body, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitCasoDefault(ZParser.CasoDefaultContext ctx)
    {
        Block body = statements(ctx.instruccion(), ctx);

        return body == null ? null : new CaseClause(null, body, line(ctx), column(ctx));
    }

    /** for ( ; ; ): every part may be missing, and a missing part is not a broken one. */
    @Override
    public AstNode visitInstruccionFor(ZParser.InstruccionForContext ctx)
    {
        AstNode    initialization = child(ctx.inicioFor());
        Expression condition      = expression(ctx.expresion());
        AstNode    update         = child(ctx.actualizacionFor());
        Block      body           = body(ctx.instruccion());

        if (broken(ctx.inicioFor(), initialization) || broken(ctx.expresion(), condition)
                || broken(ctx.actualizacionFor(), update) || body == null)
        {
            return null;
        }
        return new ForStatement(initialization, condition, update, body, line(ctx), column(ctx));
    }

    /** i++ is an expression: as the update of a for it stands alone, like a statement. */
    @Override
    public AstNode visitActualizacionFor(ZParser.ActualizacionForContext ctx)
    {
        AstNode update = child(ctx.getChild(0));

        return update instanceof Expression expression
                ? new ExpressionStatement(expression, line(ctx), column(ctx)) : update;
    }

    @Override
    public AstNode visitInstruccionWhile(ZParser.InstruccionWhileContext ctx)
    {
        Expression condition = expression(ctx.expresion());
        Block      body      = body(ctx.instruccion());

        return (condition == null || body == null) ? null
                : new WhileStatement(condition, body, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionDoWhile(ZParser.InstruccionDoWhileContext ctx)
    {
        Block      body      = body(ctx.instruccion());
        Expression condition = expression(ctx.expresion());

        return (condition == null || body == null) ? null
                : new DoWhileStatement(body, condition, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionRetorno(ZParser.InstruccionRetornoContext ctx)
    {
        Expression value = expression(ctx.expresion());

        return broken(ctx.expresion(), value) ? null : new ReturnStatement(value, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionBreak(ZParser.InstruccionBreakContext ctx)
    {
        return new BreakStatement(line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionContinue(ZParser.InstruccionContinueContext ctx)
    {
        return new ContinueStatement(line(ctx), column(ctx));
    }

    /** println ends the line, print does not. */
    @Override
    public AstNode visitInstruccionImprimir(ZParser.InstruccionImprimirContext ctx)
    {
        Expression       value  = expression(ctx.expresion());
        List<Expression> values = new ArrayList<>();

        if (broken(ctx.expresion(), value))
        {
            return null;
        }
        if (value != null)
        {
            values.add(value);
        }
        return new PrintStatement(values, ctx.PRINTLN() != null, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionExpresion(ZParser.InstruccionExpresionContext ctx)
    {
        Expression expression = expression(ctx.expresion());

        return expression == null ? null : new ExpressionStatement(expression, line(ctx), column(ctx));
    }

    /** A lone ';' does nothing, which is not the same as failing to build. */
    @Override
    public AstNode visitInstruccionVacia(ZParser.InstruccionVaciaContext ctx)
    {
        return new Block(new ArrayList<>(), line(ctx), column(ctx));
    }

    /* =================================================================
     * EXPRESSIONS
     * ================================================================= */
    @Override
    public AstNode visitExpresionTernaria(ZParser.ExpresionTernariaContext ctx)
    {
        Expression condition = expression(ctx.expresionOr());

        if (ctx.INTERROGA() == null)
        {
            return condition;
        }

        Expression whenTrue  = expression(ctx.expresion());
        Expression whenFalse = expression(ctx.expresionTernaria());

        return (condition == null || whenTrue == null || whenFalse == null) ? null
                : new TernaryExpression(condition, whenTrue, whenFalse, line(ctx), column(ctx));
    }

    @Override public AstNode visitExpresionOr(ZParser.ExpresionOrContext ctx)                         { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionAnd(ZParser.ExpresionAndContext ctx)                       { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionIgualdad(ZParser.ExpresionIgualdadContext ctx)             { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionRelacional(ZParser.ExpresionRelacionalContext ctx)         { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionAditiva(ZParser.ExpresionAditivaContext ctx)               { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionMultiplicativa(ZParser.ExpresionMultiplicativaContext ctx) { return fold(ctx, this::expression); }

    @Override
    public AstNode visitUnariaNegacion(ZParser.UnariaNegacionContext ctx)
    {
        Expression operand = expression(ctx.expresionUnaria());

        return operand == null ? null : new UnaryExpression("!", operand, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitUnariaNegativo(ZParser.UnariaNegativoContext ctx)
    {
        Expression operand = expression(ctx.expresionUnaria());

        return operand == null ? null : new UnaryExpression("-", operand, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitUnariaPrefija(ZParser.UnariaPrefijaContext ctx)
    {
        Expression operand = expression(ctx.expresionUnaria());

        return operand == null ? null
                : new IncrementExpression(operand, ctx.INCREMENTO() != null ? "++" : "--", true,
                        line(ctx), column(ctx));
    }

    /** Each suffix wraps the previous one, so a[i].b.metodo() chains correctly. */
    @Override
    public AstNode visitExpresionSufijo(ZParser.ExpresionSufijoContext ctx)
    {
        Expression current = expression(ctx.expresionPrimaria());

        for (ZParser.SufijoExpresionContext suffix : ctx.sufijoExpresion())
        {
            if (current == null)
            {
                return null;
            }
            if (suffix instanceof ZParser.SufijoIndiceContext index)
            {
                Expression position = expression(index.expresion());

                current = position == null ? null
                        : new ArrayAccessExpression(current, position, line(index), column(index));
            }
            else if (suffix instanceof ZParser.SufijoMetodoContext method)
            {
                List<Expression> arguments = expressionList(method.listaExpresiones());

                current = (missing(method.ID()) || arguments == null) ? null
                        : new MethodCallExpression(current, method.ID().getText(), arguments,
                                line(method), column(method));
            }
            else if (suffix instanceof ZParser.SufijoAtributoContext member)
            {
                current = missing(member.ID()) ? null
                        : new MemberAccessExpression(current, member.ID().getText(),
                                line(member), column(member));
            }
            else
            {
                String operator = suffix instanceof ZParser.SufijoIncrementoContext ? "++" : "--";

                current = new IncrementExpression(current, operator, false, line(suffix), column(suffix));
            }
        }
        return current;
    }

    /* -------- Primary expressions -------- */
    @Override
    public AstNode visitPrimariaEntero(ZParser.PrimariaEnteroContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.NUMERUS, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaDecimal(ZParser.PrimariaDecimalContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.DECIMALIS, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaTexto(ZParser.PrimariaTextoContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.TEXTUM, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaCaracter(ZParser.PrimariaCaracterContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.LITTERA, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaVerdadero(ZParser.PrimariaVerdaderoContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.BOOLEANO, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaFalso(ZParser.PrimariaFalsoContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.BOOLEANO, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaNulo(ZParser.PrimariaNuloContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.NULO, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaThis(ZParser.PrimariaThisContext ctx)
    {
        return new ThisExpression(line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaLeer(ZParser.PrimariaLeerContext ctx)
    {
        return new ReadExpression(line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaNuevoArreglo(ZParser.PrimariaNuevoArregloContext ctx)
    {
        List<Expression> dimensions = expressions(ctx.expresion());

        return (missing(ctx.tipoBase()) || dimensions == null) ? null
                : new NewArrayExpression(ctx.tipoBase().getText(), dimensions, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaNuevo(ZParser.PrimariaNuevoContext ctx)
    {
        List<Expression> arguments = expressionList(ctx.listaExpresiones());

        return (missing(ctx.ID()) || arguments == null) ? null
                : new NewExpression(ctx.ID().getText(), arguments, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaLlamada(ZParser.PrimariaLlamadaContext ctx)
    {
        List<Expression> arguments = expressionList(ctx.listaExpresiones());

        return (missing(ctx.ID()) || arguments == null) ? null
                : new FunctionCallExpression(ctx.ID().getText(), arguments, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaIdentificador(ZParser.PrimariaIdentificadorContext ctx)
    {
        return new IdentifierExpression(ctx.getText(), line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaAgrupacion(ZParser.PrimariaAgrupacionContext ctx)
    {
        return child(ctx.expresion());
    }

    /* =================================================================
     * Support
     * ================================================================= */
    private AstNode child(ParseTree ctx)
    {
        return ctx == null ? null : visit(ctx);
    }

    private Expression expression(ParseTree ctx)
    {
        return child(ctx) instanceof Expression expression ? expression : null;
    }

    private Block block(ParseTree ctx)
    {
        return child(ctx) instanceof Block block ? block : null;
    }

    /** The body of if, while, for or do: a single statement becomes a Block of one. */
    private Block body(ZParser.InstruccionContext ctx)
    {
        AstNode built = child(ctx);

        if (built == null || built instanceof Block)
        {
            return (Block) built;
        }
        return new Block(new ArrayList<>(List.of(built)), line(ctx), column(ctx));
    }

    /** The statements of a case: none at all is an empty body that falls into the next one. */
    private Block statements(List<ZParser.InstruccionContext> written, ParserRuleContext ctx)
    {
        List<AstNode> statements = new ArrayList<>();

        for (ZParser.InstruccionContext statement : written)
        {
            add(statements, visit(statement));
        }
        return new Block(statements, line(ctx), column(ctx));
    }

    private static boolean broken(ParseTree written, AstNode built)
    {
        return written != null && built == null;
    }

    private List<Expression> expressionList(ZParser.ListaExpresionesContext ctx)
    {
        return ctx == null ? new ArrayList<>() : expressions(ctx.expresion());
    }

    /** Null when any of them did not build. */
    private List<Expression> expressions(List<ZParser.ExpresionContext> items)
    {
        List<Expression> values = new ArrayList<>();

        for (ZParser.ExpresionContext item : items)
        {
            Expression value = expression(item);

            if (value == null)
            {
                return null;
            }
            values.add(value);
        }
        return values;
    }

    private List<Parameter> parameters(ZParser.ListaParametrosContext ctx)
    {
        List<Parameter> declared = new ArrayList<>();

        if (ctx != null)
        {
            for (ZParser.ParametroContext parameter : ctx.parametro())
            {
                if (visit(parameter) instanceof Parameter declaration)
                {
                    declared.add(declaration);
                }
            }
        }
        return declared;
    }

    /** "int[][]" is an int of rank 2. */
    private static String baseType(ZParser.TipoContext ctx)
    {
        return ctx.tipoBase().getText();
    }

    private static int rank(ZParser.TipoContext ctx)
    {
        return ctx.COR_IZQ().size();
    }

    private static void add(List<AstNode> target, AstNode node)
    {
        if (node != null)
        {
            target.add(node);
        }
    }
}
