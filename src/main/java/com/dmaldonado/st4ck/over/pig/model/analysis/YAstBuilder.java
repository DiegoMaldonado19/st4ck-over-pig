package com.dmaldonado.st4ck.over.pig.model.analysis;

import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.column;
import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.constant;
import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.fold;
import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.line;
import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.missing;

import com.dmaldonado.st4ck.over.pig.grammar.YParser;
import com.dmaldonado.st4ck.over.pig.grammar.YParserBaseVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import com.dmaldonado.st4ck.over.pig.model.ast.Language;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ArrayDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.FunctionDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.Parameter;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.Program;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.StructDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.StructField;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.VariableDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.ArrayAccessExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.CompositeLiteralExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.FunctionCallExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.IdentifierExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.LiteralExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.MemberAccessExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.ReadExpression;
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
import com.dmaldonado.st4ck.over.pig.model.ast.statement.PrintStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ReturnStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.SwitchStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.WhileStatement;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Construye el AST comun a partir del parse tree de Y?. Es el mismo AST que
 * produce PigAstBuilder, asi el semantico y el generador de cuartetas se
 * escriben una sola vez para los dos lenguajes.
 *
 * Mismas reglas que PigAstBuilder: una regla puente devuelve a su hijo, y un
 * visitX con una parte que el parser tuvo que reparar devuelve null.
 */
public class YAstBuilder extends YParserBaseVisitor<AstNode>
{
    public Program build(YParser.ProgramaContext parseTree)
    {
        return visit(parseTree) instanceof Program program ? program : null;
    }

    /* =================================================================
     * PROGRAM: only structures and functions, no globals and no main
     * ================================================================= */
    @Override
    public AstNode visitPrograma(YParser.ProgramaContext ctx)
    {
        List<AstNode>             structs   = new ArrayList<>();
        List<FunctionDeclaration> functions = new ArrayList<>();

        if (ctx.seccionEstructuras() != null)
        {
            for (YParser.DeclaracionEstructuraContext struct : ctx.seccionEstructuras().declaracionEstructura())
            {
                add(structs, visit(struct));
            }
        }
        if (ctx.seccionFunciones() != null)
        {
            for (YParser.DeclaracionFuncionContext function : ctx.seccionFunciones().declaracionFuncion())
            {
                if (visit(function) instanceof FunctionDeclaration declaration)
                {
                    functions.add(declaration);
                }
            }
        }
        return new Program(Language.Y, List.of(), structs, functions, List.of(), line(ctx), column(ctx));
    }

    /* =================================================================
     * STRUCTURES
     * ================================================================= */
    @Override
    public AstNode visitEstructuraIndentada(YParser.EstructuraIndentadaContext ctx)
    {
        return missing(ctx.ID()) ? null : struct(ctx.ID().getText(), ctx.campoEstructura(), ctx);
    }

    /** The parser already reported the form; building it spares every later use a "tipo no existe". */
    @Override
    public AstNode visitEstructuraEstiloC(YParser.EstructuraEstiloCContext ctx)
    {
        return missing(ctx.ID()) ? null : struct(ctx.ID().getText(), ctx.campoEstructura(), ctx);
    }

    private StructDeclaration struct(String name, List<YParser.CampoEstructuraContext> contexts,
                                     ParserRuleContext ctx)
    {
        List<StructField> fields = new ArrayList<>();

        for (YParser.CampoEstructuraContext field : contexts)
        {
            if (visit(field) instanceof StructField built)
            {
                fields.add(built);
            }
        }
        return new StructDeclaration(name, fields, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitCampoEstructura(YParser.CampoEstructuraContext ctx)
    {
        if (missing(ctx.ID(), ctx.tipo()))
        {
            return null;
        }

        boolean array = ctx.COR_IZQ() != null;

        if (array && missing(ctx.ENTERO()))
        {
            return null;
        }
        return new StructField(ctx.ID().getText(), ctx.tipo().getText(), array,
                array ? constant(ctx.ENTERO()) : -1, line(ctx), column(ctx));
    }

    /* =================================================================
     * FUNCTIONS
     * ================================================================= */
    @Override
    public AstNode visitDeclaracionFuncion(YParser.DeclaracionFuncionContext ctx)
    {
        Block   body         = block(ctx.bloque());
        boolean returnsValue = ctx.FLECHA() != null;

        if (missing(ctx.ID()) || body == null || (returnsValue && missing(ctx.tipo())))
        {
            return null;
        }
        return new FunctionDeclaration(ctx.ID().getText(),
                returnsValue ? ctx.tipo().getText() : "void", parameters(ctx.listaParametros()),
                List.of(), body, returnsValue, line(ctx), column(ctx));
    }

    /** "[] entero a" marks an array and "{} Persona p" a structure; the semantic pass checks the pair. */
    @Override
    public AstNode visitParametro(YParser.ParametroContext ctx)
    {
        if (missing(ctx.ID(), ctx.tipo()))
        {
            return null;
        }
        return new Parameter(ctx.ID().getText(), ctx.tipo().getText(), ctx.COR_IZQ() != null, ctx.LLAVE_IZQ() != null,
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitBloque(YParser.BloqueContext ctx)
    {
        List<AstNode> statements = new ArrayList<>();

        for (YParser.InstruccionContext statement : ctx.instruccion())
        {
            AstNode built = visit(statement);

            if (statement.bloqueInesperado() != null && built instanceof Block stray)
            {
                statements.addAll(stray.getStatements());
            }
            else
            {
                add(statements, built);
            }
        }
        return new Block(statements, line(ctx), column(ctx));
    }

    /** A stray indented block was reported by the parser; its statements stay in the enclosing one. */
    @Override
    public AstNode visitBloqueInesperado(YParser.BloqueInesperadoContext ctx)
    {
        List<AstNode> statements = new ArrayList<>();

        for (YParser.InstruccionContext statement : ctx.instruccion())
        {
            add(statements, visit(statement));
        }
        return new Block(statements, line(ctx), column(ctx));
    }

    /* =================================================================
     * STATEMENTS
     * ================================================================= */

    /** The optional ';' comes after the real statement, so the first child is the one to build. */
    @Override
    public AstNode visitInstruccion(YParser.InstruccionContext ctx)
    {
        return child(ctx.getChild(0));
    }

    @Override
    public AstNode visitDeclaracionArreglo(YParser.DeclaracionArregloContext ctx)
    {
        List<Expression> dimensions = expressions(ctx.expresion());
        AstNode          list       = child(ctx.literalCompuesto());

        if (missing(ctx.ID(), ctx.tipo()) || dimensions == null || broken(ctx.literalCompuesto(), list))
        {
            return null;
        }

        List<Expression> values = list instanceof CompositeLiteralExpression literal
                ? literal.getValues() : new ArrayList<>();

        return new ArrayDeclaration(ctx.ID().getText(), dimensions, dimensions.size(),
                ctx.tipo().getText(), values, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitDeclaracionSimple(YParser.DeclaracionSimpleContext ctx)
    {
        Expression value = expression(ctx.expresion());

        if (missing(ctx.ID(), ctx.tipo()))
        {
            return null;
        }
        // A broken value was already reported: the variable is still declared, or every use cascades.
        return new VariableDeclaration(ctx.ID().getText(), ctx.tipo().getText(),
                broken(ctx.expresion(), value) ? null : value, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitAsignacion(YParser.AsignacionContext ctx)
    {
        return assignment(expression(ctx.destino()), expression(ctx.expresion()), ctx);
    }

    private AstNode assignment(Expression target, Expression value, ParserRuleContext ctx)
    {
        return (target == null || value == null) ? null
                : new Assignment(target, value, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionIncremento(YParser.InstruccionIncrementoContext ctx)
    {
        return increment(expression(ctx.destino()), ctx.INCREMENTO() != null, ctx);
    }

    private AstNode increment(Expression target, boolean increment, ParserRuleContext ctx)
    {
        return target == null ? null
                : new IncrementStatement(target, increment ? "++" : "--", line(ctx), column(ctx));
    }

    /** Pliega la cadena sino de derecha a izquierda, igual que el aliter de PigLatin. */
    @Override
    public AstNode visitInstruccionSi(YParser.InstruccionSiContext ctx)
    {
        AstNode elseBranch = ctx.CONTRARIO() != null ? block(ctx.bloque(1)) : null;

        if (ctx.CONTRARIO() != null && elseBranch == null)
        {
            return null;
        }

        List<YParser.SinoCondicionalContext> chain = ctx.sinoCondicional();

        for (int i = chain.size() - 1; i >= 0; i--)
        {
            YParser.SinoCondicionalContext link = chain.get(i);
            Expression condition = expression(link.expresion());
            Block      body      = block(link.bloque());

            if (condition == null || body == null)
            {
                return null;
            }
            elseBranch = new IfStatement(condition, body, elseBranch, line(link), column(link));
        }

        Expression condition  = expression(ctx.expresion());
        Block      thenBranch = block(ctx.bloque(0));

        return (condition == null || thenBranch == null) ? null
                : new IfStatement(condition, thenBranch, elseBranch, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionElegir(YParser.InstruccionElegirContext ctx)
    {
        Expression       discriminant = expression(ctx.expresion());
        List<CaseClause> cases        = new ArrayList<>();

        if (discriminant == null)
        {
            return null;
        }
        for (YParser.CasoElegirContext clause : ctx.casoElegir())
        {
            if (!(visit(clause) instanceof CaseClause built))
            {
                return null;
            }
            cases.add(built);
        }
        if (ctx.casoSiempre() != null)
        {
            if (!(visit(ctx.casoSiempre()) instanceof CaseClause built))
            {
                return null;
            }
            cases.add(built);
        }
        return new SwitchStatement(discriminant, cases, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitCasoElegir(YParser.CasoElegirContext ctx)
    {
        Expression value = expression(ctx.expresion());
        Block      body  = caseBody(ctx.bloque(), ctx);

        return (value == null || body == null) ? null : new CaseClause(value, body, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitCasoSiempre(YParser.CasoSiempreContext ctx)
    {
        Block body = caseBody(ctx.bloque(), ctx);

        return body == null ? null : new CaseClause(null, body, line(ctx), column(ctx));
    }

    /** A case without a body is an empty block: it falls through into the next one. */
    private Block caseBody(YParser.BloqueContext written, ParserRuleContext ctx)
    {
        return written == null ? new Block(new ArrayList<>(), line(ctx), column(ctx)) : block(written);
    }

    @Override
    public AstNode visitInstruccionPara(YParser.InstruccionParaContext ctx)
    {
        AstNode    initialization = child(ctx.inicializacionPara());
        Expression condition      = expression(ctx.expresion());
        AstNode    update         = child(ctx.actualizacionPara());
        Block      body           = block(ctx.bloque());

        if (initialization == null || condition == null || update == null || body == null)
        {
            return null;
        }
        return new ForStatement(initialization, condition, update, body, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInicioDeclaracion(YParser.InicioDeclaracionContext ctx)
    {
        Expression value = expression(ctx.expresion());

        if (missing(ctx.ID(), ctx.tipo()) || value == null)
        {
            return null;
        }
        return new VariableDeclaration(ctx.ID().getText(), ctx.tipo().getText(), value,
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInicioAsignacion(YParser.InicioAsignacionContext ctx)
    {
        return assignment(expression(ctx.destino()), expression(ctx.expresion()), ctx);
    }

    @Override
    public AstNode visitActualizacionUnaria(YParser.ActualizacionUnariaContext ctx)
    {
        return increment(expression(ctx.destino()), ctx.INCREMENTO() != null, ctx);
    }

    @Override
    public AstNode visitActualizacionAsignacion(YParser.ActualizacionAsignacionContext ctx)
    {
        return assignment(expression(ctx.destino()), expression(ctx.expresion()), ctx);
    }

    @Override
    public AstNode visitInstruccionMientras(YParser.InstruccionMientrasContext ctx)
    {
        Expression condition = expression(ctx.expresion());
        Block      body      = block(ctx.bloque());

        return (condition == null || body == null) ? null
                : new WhileStatement(condition, body, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionHacer(YParser.InstruccionHacerContext ctx)
    {
        Block      body      = block(ctx.bloque());
        Expression condition = expression(ctx.expresion());

        return (condition == null || body == null) ? null
                : new DoWhileStatement(body, condition, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionRetorno(YParser.InstruccionRetornoContext ctx)
    {
        Expression value = expression(ctx.expresion());

        return broken(ctx.expresion(), value) ? null
                : new ReturnStatement(value, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionRomper(YParser.InstruccionRomperContext ctx)
    {
        return new BreakStatement(line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionContinuar(YParser.InstruccionContinuarContext ctx)
    {
        return new ContinueStatement(line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionImprimir(YParser.InstruccionImprimirContext ctx)
    {
        List<Expression> values = expressionList(ctx.listaExpresiones());

        return values == null ? null : new PrintStatement(values, true, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionExpresion(YParser.InstruccionExpresionContext ctx)
    {
        Expression expression = expression(ctx.expresion());

        return expression == null ? null : new ExpressionStatement(expression, line(ctx), column(ctx));
    }

    /* =================================================================
     * ASSIGNMENT TARGETS
     * ================================================================= */
    @Override
    public AstNode visitDestino(YParser.DestinoContext ctx)
    {
        if (missing(ctx.ID()))
        {
            return null;
        }

        Expression current = new IdentifierExpression(ctx.ID().getText(), line(ctx), column(ctx));

        for (YParser.SufijoDestinoContext suffix : ctx.sufijoDestino())
        {
            if (suffix instanceof YParser.AccesoIndiceContext index)
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
                YParser.AccesoAtributoContext member = (YParser.AccesoAtributoContext) suffix;

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

    /* =================================================================
     * EXPRESSIONS
     * ================================================================= */
    @Override public AstNode visitExpresionOr(YParser.ExpresionOrContext ctx)                         { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionAnd(YParser.ExpresionAndContext ctx)                       { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionIgualdad(YParser.ExpresionIgualdadContext ctx)             { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionRelacional(YParser.ExpresionRelacionalContext ctx)         { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionAditiva(YParser.ExpresionAditivaContext ctx)               { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionMultiplicativa(YParser.ExpresionMultiplicativaContext ctx) { return fold(ctx, this::expression); }

    @Override
    public AstNode visitUnariaNegacion(YParser.UnariaNegacionContext ctx)
    {
        Expression operand = expression(ctx.expresionUnaria());

        return operand == null ? null : new UnaryExpression("!", operand, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitUnariaNegativo(YParser.UnariaNegativoContext ctx)
    {
        Expression operand = expression(ctx.expresionUnaria());

        return operand == null ? null : new UnaryExpression("-", operand, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitExpresionSufijo(YParser.ExpresionSufijoContext ctx)
    {
        Expression current = expression(ctx.expresionPrimaria());

        for (YParser.SufijoExpresionContext suffix : ctx.sufijoExpresion())
        {
            if (current == null)
            {
                return null;
            }
            if (suffix instanceof YParser.SufijoIndiceContext index)
            {
                Expression position = expression(index.expresion());

                current = position == null ? null
                        : new ArrayAccessExpression(current, position, line(index), column(index));
            }
            else
            {
                YParser.SufijoAtributoContext member = (YParser.SufijoAtributoContext) suffix;

                current = missing(member.ID()) ? null
                        : new MemberAccessExpression(current, member.ID().getText(),
                                line(member), column(member));
            }
        }
        return current;
    }

    /* -------- Primary expressions -------- */
    @Override
    public AstNode visitPrimariaEntero(YParser.PrimariaEnteroContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.NUMERUS, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaDecimal(YParser.PrimariaDecimalContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.DECIMALIS, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaTexto(YParser.PrimariaTextoContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.TEXTUM, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaCaracter(YParser.PrimariaCaracterContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.LITTERA, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaVerdadero(YParser.PrimariaVerdaderoContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.BOOLEANO, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaFalso(YParser.PrimariaFalsoContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.BOOLEANO, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaLeer(YParser.PrimariaLeerContext ctx)
    {
        return new ReadExpression(line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaLlamada(YParser.PrimariaLlamadaContext ctx)
    {
        List<Expression> arguments = expressionList(ctx.listaExpresiones());

        return (missing(ctx.ID()) || arguments == null) ? null
                : new FunctionCallExpression(ctx.ID().getText(), arguments, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaIdentificador(YParser.PrimariaIdentificadorContext ctx)
    {
        return new IdentifierExpression(ctx.getText(), line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaAgrupacion(YParser.PrimariaAgrupacionContext ctx)
    {
        return child(ctx.expresion());
    }

    @Override
    public AstNode visitLiteralCompuesto(YParser.LiteralCompuestoContext ctx)
    {
        List<Expression> values = expressionList(ctx.listaExpresiones());

        return values == null ? null
                : new CompositeLiteralExpression(null, values, line(ctx), column(ctx));
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

    private static boolean broken(ParseTree written, AstNode built)
    {
        return written != null && built == null;
    }

    private List<Expression> expressionList(YParser.ListaExpresionesContext ctx)
    {
        return ctx == null ? new ArrayList<>() : expressions(ctx.expresion());
    }

    /** Null when any of them did not build. */
    private List<Expression> expressions(List<YParser.ExpresionContext> items)
    {
        List<Expression> values = new ArrayList<>();

        for (YParser.ExpresionContext item : items)
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

    private List<Parameter> parameters(YParser.ListaParametrosContext ctx)
    {
        List<Parameter> declared = new ArrayList<>();

        if (ctx != null)
        {
            for (YParser.ParametroContext parameter : ctx.parametro())
            {
                if (visit(parameter) instanceof Parameter declaration)
                {
                    declared.add(declaration);
                }
            }
        }
        return declared;
    }

    private void add(List<AstNode> target, AstNode node)
    {
        if (node != null)
        {
            target.add(node);
        }
    }
}
