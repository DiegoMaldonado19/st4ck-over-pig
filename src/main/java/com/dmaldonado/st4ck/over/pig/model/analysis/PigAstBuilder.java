package com.dmaldonado.st4ck.over.pig.model.analysis;

import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.column;
import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.fold;
import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.line;
import static com.dmaldonado.st4ck.over.pig.model.analysis.ParseTreeSupport.missing;

import com.dmaldonado.st4ck.over.pig.grammar.PigParser;
import com.dmaldonado.st4ck.over.pig.grammar.PigParserBaseVisitor;
import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.Expression;
import com.dmaldonado.st4ck.over.pig.model.ast.Language;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ArrayDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.FunctionDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ImportDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.Parameter;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.Program;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.StructDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.StructField;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.VariableDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.ArrayAccessExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.CompositeLiteralExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.FunctionCallExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.IdentifierExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.IncrementExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.LiteralExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.MemberAccessExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.MethodCallExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.NewExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.expression.UnaryExpression;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.Assignment;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.Block;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.BreakStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ContinueStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.DoWhileStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ExpressionStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ForStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.IfStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.IncrementStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.InputStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.PrintStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.ReturnStatement;
import com.dmaldonado.st4ck.over.pig.model.ast.statement.WhileStatement;
import com.dmaldonado.st4ck.over.pig.model.types.DataType;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Construye el AST propio a partir del parse tree de PigLatin, con el visitor
 * que genera ANTLR: visitXxx(ctx) devuelve el nodo de esa regla, y el valor de
 * retorno es el unico canal entre una regla y su padre.
 *
 * Las reglas puente (un solo hijo) no se sobrescriben: visitChildren() heredado
 * ya devuelve el resultado del hijo.
 *
 * El arbol puede venir reparado por la recuperacion de errores de ANTLR: cada
 * visitX devuelve null si le falta algo que usa, y el padre lo omite. Asi un
 * error de sintaxis borra su instruccion, no el analisis semantico entero.
 */
public class PigAstBuilder extends PigParserBaseVisitor<AstNode>
{
    /** Entry point: walks the parse tree and returns the root of the AST. */
    public Program build(PigParser.ProgramaContext parseTree)
    {
        return visit(parseTree) instanceof Program program ? program : null;
    }

    /* =================================================================
     * PROGRAM
     * ================================================================= */
    @Override
    public AstNode visitPrograma(PigParser.ProgramaContext ctx)
    {
        List<ImportDeclaration> imports   = new ArrayList<>();
        List<AstNode> globals             = new ArrayList<>();
        List<FunctionDeclaration> methods = new ArrayList<>();
        List<AstNode> mainStatements      = new ArrayList<>();

        if (ctx.seccionImportaciones() != null)
        {
            for (PigParser.ImportacionContext importation : ctx.seccionImportaciones().importacion())
            {
                if (visit(importation) instanceof ImportDeclaration declaration)
                {
                    imports.add(declaration);
                }
            }
        }
        if (ctx.seccionVariables() != null)
        {
            for (PigParser.DeclaracionGlobalContext global : ctx.seccionVariables().declaracionGlobal())
            {
                add(globals, visit(global));
            }
        }
        if (ctx.seccionFunciones() != null)
        {
            for (PigParser.DeclaracionFuncionContext function : ctx.seccionFunciones().declaracionFuncion())
            {
                if (visit(function) instanceof FunctionDeclaration declaration)
                {
                    methods.add(declaration);
                }
            }
        }
        if (ctx.seccionPrincipal() != null)
        {
            for (PigParser.InstruccionContext statement : ctx.seccionPrincipal().instruccion())
            {
                add(mainStatements, visit(statement));
            }
        }
        return new Program(Language.PIG, imports, globals, methods, mainStatements,
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitImportacion(PigParser.ImportacionContext ctx)
    {
        List<String> segments = new ArrayList<>();

        for (TerminalNode segment : ctx.ID())
        {
            if (missing(segment))
            {
                return null;
            }
            segments.add(segment.getText());
        }
        return segments.size() < 2 ? null : new ImportDeclaration(segments, line(ctx), column(ctx));
    }

    /* =================================================================
     * DECLARATIONS
     * ================================================================= */
    @Override
    public AstNode visitDeclaracionVariableSimple(PigParser.DeclaracionVariableSimpleContext ctx)
    {
        Expression value = expression(ctx.expresion());

        if (missing(ctx.ID(), ctx.tipo()) || broken(ctx.expresion(), value))
        {
            return null;
        }
        if (value == null)
        {
            value = implicitBooleanValue(ctx.tipo());
        }
        return new VariableDeclaration(ctx.ID().getText(), ctx.tipo().getText(), value,
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitDeclaracionVariableEstructura(PigParser.DeclaracionVariableEstructuraContext ctx)
    {
        Expression value = expression(ctx.literalCompuesto());

        if (missing(ctx.ID(), ctx.tipo()) || value == null)
        {
            return null;
        }
        return new VariableDeclaration(ctx.ID().getText(), ctx.tipo().getText(), value,
                line(ctx), column(ctx));
    }

    /** "esto o : novus Persona(12);": the class named after novus is also the type. */
    @Override
    public AstNode visitDeclaracionVariableObjeto(PigParser.DeclaracionVariableObjetoContext ctx)
    {
        if (missing(ctx.ID()) || !(expression(ctx.nuevoObjeto()) instanceof NewExpression object))
        {
            return null;
        }
        return new VariableDeclaration(ctx.ID().getText(), object.getClassName(), object,
                line(ctx), column(ctx));
    }

    /** "esto activo : verum;": el tipo es tambien el valor, se materializa aqui. */
    private Expression implicitBooleanValue(PigParser.TipoContext ctx)
    {
        String text = ctx.getText();

        if ("verum".equals(text) || "falsus".equals(text))
        {
            return new LiteralExpression(text, DataType.BOOLEANO, line(ctx), column(ctx));
        }
        return null;
    }

    @Override
    public AstNode visitDeclaracionArreglo(PigParser.DeclaracionArregloContext ctx)
    {
        List<Expression> dimensions = expressions(ctx.expresion());
        List<Expression> values     = expressionList(ctx.listaExpresiones());

        if (missing(ctx.ID()) || dimensions == null || values == null)
        {
            return null;
        }
        return new ArrayDeclaration(ctx.ID().getText(), dimensions, dimensions.size(),
                arrayTypeText(ctx, values), values, line(ctx), column(ctx));
    }

    /** Sin tipo explicito se deduce del primer valor; sin valores, lo reporta la semantica. */
    private String arrayTypeText(PigParser.DeclaracionArregloContext ctx, List<Expression> values)
    {
        if (ctx.tipo() != null)
        {
            return ctx.tipo().getText();
        }
        if (!values.isEmpty() && values.get(0) instanceof LiteralExpression literal)
        {
            return literal.getType().getLatinName();
        }
        return null;
    }

    @Override
    public AstNode visitCampoSimple(PigParser.CampoSimpleContext ctx)
    {
        if (missing(ctx.ID(), ctx.tipo()))
        {
            return null;
        }
        return new StructField(ctx.ID().getText(), ctx.tipo().getText(), false, -1,
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitCampoArreglo(PigParser.CampoArregloContext ctx)
    {
        if (missing(ctx.ID(), ctx.tipo()))
        {
            return null;
        }
        return new StructField(ctx.ID().getText(), ctx.tipo().getText(), true, -1,
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitDeclaracionEstructura(PigParser.DeclaracionEstructuraContext ctx)
    {
        if (missing(ctx.ID()))
        {
            return null;
        }

        List<StructField> fields = new ArrayList<>();

        for (PigParser.AtributoEstructuraContext attribute : ctx.atributoEstructura())
        {
            if (visit(attribute) instanceof StructField field)
            {
                fields.add(field);
            }
        }
        return new StructDeclaration(ctx.ID().getText(), fields, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitParametro(PigParser.ParametroContext ctx)
    {
        if (missing(ctx.ID(), ctx.tipo()))
        {
            return null;
        }
        return new Parameter(ctx.ID().getText(), ctx.tipo().getText(), false, false, line(ctx), column(ctx));
    }

    /* =================================================================
     * FUNCTIONS
     * ================================================================= */
    @Override
    public AstNode visitFuncionSinRetorno(PigParser.FuncionSinRetornoContext ctx)
    {
        Block body = block(ctx.cuerpoFuncion());

        if (missing(ctx.ID()) || body == null)
        {
            return null;
        }
        return new FunctionDeclaration(ctx.ID().getText(), "void",
                parameters(ctx.listaParametros()), localVariables(ctx.cuerpoFuncion()),
                body, false, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitFuncionConRetorno(PigParser.FuncionConRetornoContext ctx)
    {
        Block body = block(ctx.cuerpoFuncion());

        if (missing(ctx.ID(), ctx.tipo()) || body == null)
        {
            return null;
        }
        return new FunctionDeclaration(ctx.ID().getText(), ctx.tipo().getText(),
                parameters(ctx.listaParametros()), localVariables(ctx.cuerpoFuncion()),
                body, true, line(ctx), column(ctx));
    }

    /** El cuerpo guarda solo instrucciones; VARIABILES[ ] sale aparte en localVariables(). */
    @Override
    public AstNode visitCuerpoFuncion(PigParser.CuerpoFuncionContext ctx)
    {
        return new Block(statements(ctx.instruccion()), line(ctx), column(ctx));
    }

    private List<AstNode> localVariables(PigParser.CuerpoFuncionContext ctx)
    {
        List<AstNode> declarations = new ArrayList<>();

        if (ctx != null && ctx.seccionVariablesLocales() != null)
        {
            for (PigParser.DeclaracionGlobalContext declaration : ctx.seccionVariablesLocales().declaracionGlobal())
            {
                add(declarations, visit(declaration));
            }
        }
        return declarations;
    }

    @Override
    public AstNode visitBloque(PigParser.BloqueContext ctx)
    {
        return new Block(statements(ctx.instruccion()), line(ctx), column(ctx));
    }

    /* =================================================================
     * STATEMENTS
     * ================================================================= */
    @Override
    public AstNode visitAsignacionSimple(PigParser.AsignacionSimpleContext ctx)
    {
        return assignment(expression(ctx.destino()), expression(ctx.expresion()), ctx);
    }

    @Override
    public AstNode visitAsignacionEstructura(PigParser.AsignacionEstructuraContext ctx)
    {
        return assignment(expression(ctx.destino()), expression(ctx.literalCompuesto()), ctx);
    }

    private AstNode assignment(Expression target, Expression value, ParserRuleContext ctx)
    {
        return (target == null || value == null) ? null
                : new Assignment(target, value, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionIncremento(PigParser.InstruccionIncrementoContext ctx)
    {
        Expression target = expression(ctx.destino());

        return target == null ? null
                : new IncrementStatement(target, ctx.INCREMENTO() != null ? "++" : "--",
                        line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionExpresion(PigParser.InstruccionExpresionContext ctx)
    {
        Expression expression = expression(ctx.expresion());

        return expression == null ? null
                : new ExpressionStatement(expression, line(ctx), column(ctx));
    }

    /** Pliega la cadena aliter de derecha a izquierda: si(a) A aliter(b) B => If(a,A,If(b,B)). */
    @Override
    public AstNode visitInstruccionSi(PigParser.InstruccionSiContext ctx)
    {
        // bloque(1) solo existe si un aliter sin condicion cierra la cadena.
        AstNode elseBranch = ctx.ALITER() != null ? block(ctx.bloque(1)) : null;

        if (ctx.ALITER() != null && elseBranch == null)
        {
            return null;
        }

        List<PigParser.AliterCondicionalContext> chain = ctx.aliterCondicional();

        for (int i = chain.size() - 1; i >= 0; i--)
        {
            PigParser.AliterCondicionalContext link = chain.get(i);
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
    public AstNode visitInstruccionMientras(PigParser.InstruccionMientrasContext ctx)
    {
        Expression condition = expression(ctx.expresion());
        Block      body      = block(ctx.bloque());

        return (condition == null || body == null) ? null
                : new WhileStatement(condition, body, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionHacerMientras(PigParser.InstruccionHacerMientrasContext ctx)
    {
        Block      body      = block(ctx.bloque());
        Expression condition = expression(ctx.expresion());

        return (condition == null || body == null) ? null
                : new DoWhileStatement(body, condition, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitActualizacionUnaria(PigParser.ActualizacionUnariaContext ctx)
    {
        Expression target = expression(ctx.destino());

        return target == null ? null
                : new IncrementStatement(target, ctx.INCREMENTO() != null ? "++" : "--",
                        line(ctx), column(ctx));
    }

    @Override
    public AstNode visitActualizacionAsignacion(PigParser.ActualizacionAsignacionContext ctx)
    {
        return assignment(expression(ctx.destino()), expression(ctx.expresion()), ctx);
    }

    @Override
    public AstNode visitInstruccionPara(PigParser.InstruccionParaContext ctx)
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
    public AstNode visitInstruccionRetorno(PigParser.InstruccionRetornoContext ctx)
    {
        Expression value = expression(ctx.expresion());

        return broken(ctx.expresion(), value) ? null
                : new ReturnStatement(value, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionInterrumpe(PigParser.InstruccionInterrumpeContext ctx)
    {
        return new BreakStatement(line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionPerge(PigParser.InstruccionPergeContext ctx)
    {
        return new ContinueStatement(line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionSalida(PigParser.InstruccionSalidaContext ctx)
    {
        List<Expression> values = new ArrayList<>();

        for (PigParser.ExpresionContext value : ctx.expresion())
        {
            Expression printed = expression(value);

            if (printed == null)
            {
                return null;
            }
            values.add(printed);
        }
        return new PrintStatement(values, true, line(ctx), column(ctx));
    }

    /** Without a destino the read discards the value: expression() gives null. */
    @Override
    public AstNode visitInstruccionEntrada(PigParser.InstruccionEntradaContext ctx)
    {
        Expression target = expression(ctx.destino());

        return broken(ctx.destino(), target) ? null
                : new InputStatement(target, line(ctx), column(ctx));
    }

    /* =================================================================
     * ASSIGNMENT TARGETS
     * ================================================================= */
    @Override
    public AstNode visitDestino(PigParser.DestinoContext ctx)
    {
        if (missing(ctx.ID()))
        {
            return null;
        }

        Expression current = new IdentifierExpression(ctx.ID().getText(), line(ctx), column(ctx));

        for (PigParser.SufijoDestinoContext suffix : ctx.sufijoDestino())
        {
            if (suffix instanceof PigParser.AccesoIndiceContext index)
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
                PigParser.AccesoAtributoContext member = (PigParser.AccesoAtributoContext) suffix;

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
    @Override public AstNode visitExpresionOr(PigParser.ExpresionOrContext ctx)                         { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionAnd(PigParser.ExpresionAndContext ctx)                       { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionIgualdad(PigParser.ExpresionIgualdadContext ctx)             { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionRelacional(PigParser.ExpresionRelacionalContext ctx)         { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionAditiva(PigParser.ExpresionAditivaContext ctx)               { return fold(ctx, this::expression); }
    @Override public AstNode visitExpresionMultiplicativa(PigParser.ExpresionMultiplicativaContext ctx) { return fold(ctx, this::expression); }

    @Override
    public AstNode visitUnariaNegacionLogica(PigParser.UnariaNegacionLogicaContext ctx)
    {
        Expression operand = expression(ctx.expresionUnaria());

        return operand == null ? null : new UnaryExpression("non", operand, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitUnariaNegativo(PigParser.UnariaNegativoContext ctx)
    {
        Expression operand = expression(ctx.expresionUnaria());

        return operand == null ? null : new UnaryExpression("-", operand, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitUnariaPrefija(PigParser.UnariaPrefijaContext ctx)
    {
        Expression operand = expression(ctx.expresionUnaria());

        return operand == null ? null
                : new IncrementExpression(operand, ctx.INCREMENTO() != null ? "++" : "--", true,
                        line(ctx), column(ctx));
    }

    /** Each suffix wraps the previous one, so arr[i].prop[j].metodo() chains correctly. */
    @Override
    public AstNode visitExpresionSufijo(PigParser.ExpresionSufijoContext ctx)
    {
        Expression current = expression(ctx.expresionPrimaria());

        for (PigParser.SufijoExpresionContext suffix : ctx.sufijoExpresion())
        {
            if (current == null)
            {
                return null;
            }
            if (suffix instanceof PigParser.SufijoIndiceContext index)
            {
                Expression position = expression(index.expresion());

                current = position == null ? null
                        : new ArrayAccessExpression(current, position, line(index), column(index));
            }
            else if (suffix instanceof PigParser.SufijoMetodoContext method)
            {
                List<Expression> arguments = expressionList(method.listaExpresiones());

                current = (missing(method.ID()) || arguments == null) ? null
                        : new MethodCallExpression(current, method.ID().getText(), arguments,
                                line(method), column(method));
            }
            else if (suffix instanceof PigParser.SufijoAtributoContext member)
            {
                current = missing(member.ID()) ? null
                        : new MemberAccessExpression(current, member.ID().getText(),
                                line(member), column(member));
            }
            else
            {
                String operator = suffix instanceof PigParser.SufijoIncrementoContext ? "++" : "--";
                current = new IncrementExpression(current, operator, false,
                        line(suffix), column(suffix));
            }
        }
        return current;
    }

    /* -------- Primary expressions (leaves of the AST) -------- */
    @Override
    public AstNode visitPrimariaEntero(PigParser.PrimariaEnteroContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.NUMERUS, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaDecimal(PigParser.PrimariaDecimalContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.DECIMALIS, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaTexto(PigParser.PrimariaTextoContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.TEXTUM, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaCaracter(PigParser.PrimariaCaracterContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.LITTERA, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaVerdadero(PigParser.PrimariaVerdaderoContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.BOOLEANO, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaFalso(PigParser.PrimariaFalsoContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.BOOLEANO, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaIdentificador(PigParser.PrimariaIdentificadorContext ctx)
    {
        return new IdentifierExpression(ctx.getText(), line(ctx), column(ctx));
    }

    /** Parentheses add no node: the AST already carries the grouping. */
    @Override
    public AstNode visitPrimariaAgrupacion(PigParser.PrimariaAgrupacionContext ctx)
    {
        return child(ctx.expresion());
    }

    @Override
    public AstNode visitLiteralConNombre(PigParser.LiteralConNombreContext ctx)
    {
        List<String> fieldNames = new ArrayList<>();
        List<Expression> values = new ArrayList<>();

        for (PigParser.CampoLiteralContext field : ctx.campoLiteral())
        {
            Expression value = expression(field.expresion());

            if (missing(field.ID()) || value == null)
            {
                return null;
            }
            fieldNames.add(field.ID().getText());
            values.add(value);
        }
        return new CompositeLiteralExpression(fieldNames, values, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitLiteralPosicional(PigParser.LiteralPosicionalContext ctx)
    {
        List<Expression> values = expressionList(ctx.listaExpresiones());

        return values == null ? null
                : new CompositeLiteralExpression(null, values, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitLlamadaFuncion(PigParser.LlamadaFuncionContext ctx)
    {
        List<Expression> arguments = expressionList(ctx.listaExpresiones());

        return (missing(ctx.ID()) || arguments == null) ? null
                : new FunctionCallExpression(ctx.ID().getText(), arguments, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitNuevoObjeto(PigParser.NuevoObjetoContext ctx)
    {
        List<Expression> arguments = expressionList(ctx.listaExpresiones());

        return (missing(ctx.ID()) || arguments == null) ? null
                : new NewExpression(ctx.ID().getText(), arguments, line(ctx), column(ctx));
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

    /** An optional part that was written but did not build: its statement is dropped. */
    private static boolean broken(ParseTree written, AstNode built)
    {
        return written != null && built == null;
    }

    private List<AstNode> statements(List<PigParser.InstruccionContext> contexts)
    {
        List<AstNode> statements = new ArrayList<>();

        for (PigParser.InstruccionContext statement : contexts)
        {
            add(statements, visit(statement));
        }
        return statements;
    }

    /** Null when an item did not build: dropping it silently would change the arity. */
    private List<Expression> expressionList(PigParser.ListaExpresionesContext ctx)
    {
        return ctx == null ? new ArrayList<>() : expressions(ctx.expresion());
    }

    /** Null when any of them did not build. */
    private List<Expression> expressions(List<PigParser.ExpresionContext> items)
    {
        List<Expression> values = new ArrayList<>();

        for (PigParser.ExpresionContext item : items)
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

    private List<Parameter> parameters(PigParser.ListaParametrosContext ctx)
    {
        List<Parameter> declared = new ArrayList<>();

        if (ctx != null)
        {
            for (PigParser.ParametroContext parameter : ctx.parametro())
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
