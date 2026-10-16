package com.dmaldonado.st4ck.over.pig.model;

import com.dmaldonado.st4ck.over.pig.grammar.PigLexer;
import com.dmaldonado.st4ck.over.pig.grammar.PigParser;
import com.dmaldonado.st4ck.over.pig.grammar.YLexer;
import com.dmaldonado.st4ck.over.pig.grammar.YParser;
import com.dmaldonado.st4ck.over.pig.grammar.ZLexer;
import com.dmaldonado.st4ck.over.pig.grammar.ZParser;
import com.dmaldonado.st4ck.over.pig.model.analysis.PigAstBuilder;
import com.dmaldonado.st4ck.over.pig.model.analysis.SemanticAnalyzer;
import com.dmaldonado.st4ck.over.pig.model.analysis.YAstBuilder;
import com.dmaldonado.st4ck.over.pig.model.analysis.ZAstBuilder;
import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.ast.Language;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ClassDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.ImportDeclaration;
import com.dmaldonado.st4ck.over.pig.model.ast.declaration.Program;
import com.dmaldonado.st4ck.over.pig.model.codegen.CEmitter;
import com.dmaldonado.st4ck.over.pig.model.codegen.Quadruple;
import com.dmaldonado.st4ck.over.pig.model.codegen.QuadrupleGenerator;
import com.dmaldonado.st4ck.over.pig.model.codegen.RiscVEmitter;
import com.dmaldonado.st4ck.over.pig.model.codegen.ThreeAddressGenerator;
import com.dmaldonado.st4ck.over.pig.model.codegen.ThreeAddressPrinter;
import com.dmaldonado.st4ck.over.pig.model.errors.CompilerError;
import com.dmaldonado.st4ck.over.pig.model.errors.ErrorManager;
import com.dmaldonado.st4ck.over.pig.model.errors.ErrorType;
import com.dmaldonado.st4ck.over.pig.model.errors.SyntaxErrorListener;
import com.dmaldonado.st4ck.over.pig.model.stack.ProcessStackListener;
import com.dmaldonado.st4ck.over.pig.model.stack.ProcessStep;
import com.dmaldonado.st4ck.over.pig.model.symbols.SymbolTable;
import com.dmaldonado.st4ck.over.pig.util.FileManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * Corre el pipeline completo y decide donde cortar: nada se grafica ni se
 * traduce si hay errores.
 *
 * Solo corta por errores LEXICOS, donde el flujo de tokens ya no es confiable.
 * Tras un error de sintaxis sigue: la auxiliar mezcla a proposito errores
 * sintacticos y semanticos en el mismo archivo, y cortar en el primero se
 * llevaria todos los semanticos del reporte.
 *
 * No sabe nada de la interfaz: no imprime, devuelve un CompilationResult.
 */
public class CompilerPipeline
{
    private static final Logger LOGGER = Logger.getLogger(CompilerPipeline.class.getName());

    /** The error token of every lexer and what it means. The editor colors these same tokens as errors. */
    public static final Map<String, String> LEXICAL_ERRORS = Map.of(
            "TEXTO_SIN_CERRAR",           "Cadena sin cerrar: falta la comilla doble final.",
            "CARACTER_SIN_CERRAR",        "Caracter sin cerrar: falta la comilla simple final.",
            "COMENTARIO_SIN_CERRAR",      "Comentario de bloque sin cerrar: falta '*/'.",
            "COMENTARIO_HASH_SIN_CERRAR", "Comentario de bloque sin cerrar: falta '##'.",
            "CARACTER_LARGO",             "Un caracter lleva un solo simbolo: un texto va entre comillas dobles.",
            "SANGRIA_INVALIDA",           "La sangria no coincide con la de ningun bloque abierto.",
            "CARACTER_MAL_FORMADO",       "Caracter vacio o escape invalido: validos \\n \\t \\r \\' \\\" \\\\.",
            "CARACTER_INVALIDO",          "Simbolo no reconocido por el lenguaje.");

    /**
     * @param ast          null when the source did not get past the lexer.
     * @param symbolTable  empty when the AST was never built.
     * @param steps        empty only when the file was never parsed at all.
     * @param quadruples   empty unless the program is valid and has a main.
     * @param cCode        the C translation of the quadruples, or "".
     * @param threeAddressCode the three address code written by hand, what is still pending, or "".
     * @param assembly     its RISC-V translation, or "" while the three address code is incomplete.
     */
    public record CompilationResult(Program ast, SymbolTable symbolTable, List<ProcessStep> steps,
                                    List<CompilerError> errors, List<Quadruple> quadruples,
                                    String cCode, String threeAddressCode, String assembly)
    {
        /** Only a valid program may be graphed and translated. */
        public boolean isValid()
        {
            return errors.isEmpty() && ast != null;
        }
    }

    /** One front-end run: the AST (null after a lexical error) and the steps of its parse. */
    private record ParsedUnit(Program ast, List<ProcessStep> steps)
    {
    }

    /** A program and the file its errors are reported against. */
    private record Unit(String source, Program ast)
    {
    }

    /**
     * @param sourcePath where the source lives, for its language and to resolve
     *                   its imports; null for a file that was never saved.
     */
    public CompilationResult compile(String source, Path sourcePath)
    {
        try
        {
            return compileSource(source, sourcePath);
        }
        catch (StackOverflowError error)
        {
            // Each nesting level is one more level of recursion in the parser and in every tree walk.
            LOGGER.log(Level.WARNING, "Programa demasiado anidado para analizarlo", error);
            return failure(sourcePath, errors -> errors.addSyntactic("El programa anida demasiadas expresiones "
                    + "o instrucciones para analizarlo: divida la expresion mas larga en varias.", "", 1, 1));
        }
        catch (RuntimeException exception)
        {
            // A compiler bug still gives a result: without one the screen keeps the previous file's.
            LOGGER.log(Level.SEVERE, "Fallo interno al compilar " + sourcePath, exception);
            return failure(sourcePath, errors -> errors.addSemantic("Error interno del compilador (" + exception
                    + "): el programa no se pudo analizar.", "", 1, 1));
        }
    }

    /** A result that only carries the error that stopped the compilation. */
    private static CompilationResult failure(Path sourcePath, Consumer<ErrorManager> report)
    {
        ErrorManager errorManager = new ErrorManager();

        errorManager.setSource(sourcePath == null ? "" : sourcePath.getFileName().toString());
        report.accept(errorManager);
        return new CompilationResult(null, new SymbolTable(), List.of(), errorManager.getErrors(), List.of(), "",
                "", "");
    }

    private CompilationResult compileSource(String source, Path sourcePath)
    {
        ErrorManager errorManager = new ErrorManager();
        SymbolTable  symbolTable  = new SymbolTable();
        String       fileName     = sourcePath == null ? "" : sourcePath.getFileName().toString();
        String       extension    = FileManager.extensionOf(fileName);

        errorManager.setSource(fileName);

        Language   language = Objects.requireNonNullElse(Language.fromExtension(extension), Language.PIG);
        ParsedUnit root     = parse(source, language, errorManager);

        if (root.ast() == null)
        {
            return new CompilationResult(null, symbolTable, root.steps(), errorManager.getErrors(),
                    List.of(), "", "", "");
        }

        checkClassName(root.ast(), fileName, errorManager);

        // Los importados van primero: el .pig necesita ver lo que declaran.
        List<Unit> units = loadImports(root.ast(), sourcePath, errorManager);
        units.add(new Unit(fileName, root.ast()));

        // Un import que no paso de su lexer no declara nada: analizar el resto
        // reportaria cada uso de lo que declaraba como inexistente.
        if (errorManager.hasErrorsOf(ErrorType.LEXICAL))
        {
            return new CompilationResult(root.ast(), symbolTable, root.steps(), errorManager.getErrors(),
                    List.of(), "", "", "");
        }

        // Dos pasadas sobre TODOS los archivos: primero se registra lo que cada
        // uno declara y despues se leen los cuerpos. Asi Pila.z usa a Nodo sin
        // importarlo, sea cual sea el orden de los import del .pig.
        List<SemanticAnalyzer> analyzers = units.stream()
                .map(unit -> new SemanticAnalyzer(errorManager, symbolTable))
                .toList();

        for (int i = 0; i < units.size(); i++)
        {
            Unit             unit     = units.get(i);
            SemanticAnalyzer analyzer = analyzers.get(i);

            errorManager.setSource(unit.source());
            guarded(() ->
            {
                analyzer.register(unit.ast());
                return null;
            }, errorManager);
        }

        // Un .z no tiene import: como las clases de un paquete de Java, ve a
        // las demas clases de su carpeta. Solo se registran, y sus errores son
        // de su propia compilacion, no de esta. Van despues del archivo: si un
        // vecino mal nombrado repite su clase, la del archivo es la que queda.
        if (language == Language.Z)
        {
            for (Program sibling : siblingClasses(sourcePath))
            {
                new SemanticAnalyzer(new ErrorManager(), symbolTable).register(sibling);
            }
        }
        for (int i = 0; i < units.size(); i++)
        {
            Unit             unit     = units.get(i);
            SemanticAnalyzer analyzer = analyzers.get(i);

            errorManager.setSource(unit.source());
            guarded(() -> analyzer.visitProgram(unit.ast()), errorManager);
        }
        errorManager.setSource(fileName);

        List<Quadruple> quadruples       = List.of();
        String          cCode            = "";
        String          threeAddressCode = "";
        String          assembly         = "";

        // Solo un programa sin errores se traduce, y solo un .pig tiene MAIOR
        // que ejecutar: un .y o un .z se analiza y ahi termina.
        if (errorManager.getErrors().isEmpty() && language == Language.PIG)
        {
            quadruples = new QuadrupleGenerator().generate(units.stream().map(Unit::ast).toList(),
                                                           symbolTable.getGlobalSize());
            cCode      = CEmitter.emit(quadruples);

            // The new translation still grows phase by phase: what it lacks is shown, not failed.
            try
            {
                List<Quadruple> code = new ThreeAddressGenerator(symbolTable)
                        .generate(units.stream().map(Unit::ast).toList());

                threeAddressCode = ThreeAddressPrinter.print(code);
                assembly         = new RiscVEmitter().emit(code);
            }
            catch (UnsupportedOperationException pending)
            {
                threeAddressCode = "// " + pending.getMessage();
            }
        }
        return new CompilationResult(root.ast(), symbolTable, root.steps(), errorManager.getErrors(),
                quadruples, cCode, threeAddressCode, assembly);
    }

    /* =================================================================
     * Front-ends
     * ================================================================= */

    private ParsedUnit parse(String source, Language language, ErrorManager errorManager)
    {
        CharStream chars = CharStreams.fromString(source == null ? "" : source);
        Lexer      lexer = switch (language)
        {
            case PIG -> new PigLexer(chars);
            case Y   -> new YLexer(chars);
            case Z   -> new ZLexer(chars);
        };
        lexer.removeErrorListeners();   // silence ANTLR's ConsoleErrorListener

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();

        // Con errores lexicos no se parsea: todo error de sintaxis posterior
        // seria ruido derivado de este.
        if (reportLexicalErrors(tokens, lexer.getVocabulary(), errorManager))
        {
            return new ParsedUnit(null, List.of());
        }

        return switch (language)
        {
            case PIG ->
            {
                PigParser                 parser = listenedBy(new PigParser(tokens), errorManager);

                parser.setErrorHandler(new KeepFinisStrategy());
                PigParser.ProgramaContext tree   = parser.programa();
                yield finish(parser, tree, () -> new PigAstBuilder().build(tree), errorManager);
            }
            case Y ->
            {
                YParser                 parser = listenedBy(new YParser(tokens), errorManager);
                YParser.ProgramaContext tree   = parser.programa();
                yield finish(parser, tree, () -> new YAstBuilder().build(tree), errorManager);
            }
            case Z ->
            {
                ZParser                 parser = listenedBy(new ZParser(tokens), errorManager);
                ZParser.ProgramaContext tree   = parser.programa();
                yield finish(parser, tree, () -> new ZAstBuilder().build(tree), errorManager);
            }
        };
    }

    /**
     * ">> x" and then "FINIS;": ANTLR would rather drop FINIS than insert the
     * missing ';', and then report FINIS missing as well. Never dropping it makes
     * the error the ';' that really is missing.
     */
    private static final class KeepFinisStrategy extends DefaultErrorStrategy
    {
        @Override
        protected Token singleTokenDeletion(Parser recognizer)
        {
            return recognizer.getCurrentToken().getType() == PigLexer.FIN_PROGRAMA
                   ? null : super.singleTokenDeletion(recognizer);
        }
    }

    private static <P extends Parser> P listenedBy(P parser, ErrorManager errorManager)
    {
        parser.removeErrorListeners();
        parser.addErrorListener(new SyntaxErrorListener(errorManager));
        return parser;
    }

    /** Los pasos van antes del AST: el archivo que no parsea es justo aquel cuyos pasos ERROR se leen. */
    private ParsedUnit finish(Parser parser, ParserRuleContext tree, Supplier<Program> builder,
                              ErrorManager errorManager)
    {
        ProcessStackListener stackListener = new ProcessStackListener(parser);
        ParseTreeWalker.DEFAULT.walk(stackListener, tree);

        return new ParsedUnit(guarded(builder, errorManager), stackListener.getSteps());
    }

    /**
     * No hay LexicalErrorListener a proposito: las reglas de error del lexer
     * cubren todo lo que no encaja, la ultima de ellas con "CARACTER_INVALIDO
     * : . ;", asi que el lexer nunca falla y un BaseErrorListener sobre el
     * jamas dispararia. El mensaje sale del tipo de token, no del texto; se
     * compara por NOMBRE para que sirva a todos los lexers.
     *
     * @return true if the stream holds any lexical error.
     */
    private boolean reportLexicalErrors(CommonTokenStream tokens, Vocabulary vocabulary,
                                        ErrorManager errorManager)
    {
        boolean found = false;

        for (Token token : tokens.getTokens())
        {
            String description = LEXICAL_ERRORS.get(String.valueOf(vocabulary.getSymbolicName(token.getType())));

            if (description != null)
            {
                errorManager.addLexical(description, token.getText(),
                        token.getLine(), token.getCharPositionInLine() + 1);
                found = true;
            }
        }
        return found;
    }

    /**
     * El AST ya se construye aunque haya errores de sintaxis, y un null-guard
     * olvidado ante un arbol reparado no debe tumbar la interfaz: con errores
     * sintacticos previos se registra completo y se sigue con lo reunido. Sin
     * ellos es un bug real, y se relanza.
     */
    private static <T> T guarded(Supplier<T> phase, ErrorManager errorManager)
    {
        try
        {
            return phase.get();
        }
        catch (RuntimeException exception)
        {
            if (!errorManager.hasErrorsOf(ErrorType.SYNTACTIC))
            {
                throw exception;
            }
            LOGGER.log(Level.WARNING, "Fase interrumpida por un arbol reparado tras errores de sintaxis",
                    exception);
            return null;
        }
    }

    /* =================================================================
     * Imports
     * ================================================================= */

    /**
     * Resuelve cada import contra la carpeta del .pig y lo compila con su propio
     * front-end. No hace falta detectar ciclos: solo el .pig importa, y un .pig
     * no se puede importar. Un archivo importado dos veces se lee una.
     */
    private List<Unit> loadImports(Program root, Path rootPath, ErrorManager errorManager)
    {
        List<Unit>  units      = new ArrayList<>();
        // As written, not as a Path: Windows would take sub.bien.y for sub.Bien.y and skip the case check.
        Set<String> seen       = new HashSet<>();
        String      rootSource = rootPath == null ? "" : rootPath.getFileName().toString();

        for (ImportDeclaration declaration : root.getImports())
        {
            String   extension = declaration.getExtension();
            Language language  = Language.fromExtension(extension);

            if (rootPath == null)
            {
                importError(declaration, "Guarde el archivo para poder resolver sus importaciones.",
                        errorManager);
                break;
            }
            if (language == null || language == Language.PIG)
            {
                importError(declaration, "Solo se pueden importar archivos .y o .z, no '." + extension
                        + "'.", errorManager);
                continue;
            }

            Path file = rootPath.toAbsolutePath().getParent()
                    .resolve(declaration.getRelativePath()).normalize();

            if (!seen.add(declaration.getRelativePath()))
            {
                continue;
            }
            if (!Files.isRegularFile(file))
            {
                importError(declaration, "No se encontro el archivo importado '"
                        + declaration.getRelativePath() + "'.", errorManager);
                continue;
            }

            String content;
            String actualPath;

            try
            {
                Path real  = file.toRealPath(LinkOption.NOFOLLOW_LINKS);
                int  names = Path.of(declaration.getRelativePath()).getNameCount();

                actualPath = real.subpath(real.getNameCount() - names, real.getNameCount()).toString()
                                 .replace(real.getFileSystem().getSeparator(), "/");
                content    = FileManager.read(file);
            }
            catch (IOException exception)
            {
                LOGGER.log(Level.SEVERE, "No se pudo leer el archivo importado " + file, exception);
                importError(declaration, "No se pudo leer '" + declaration.getRelativePath() + "': "
                        + exception.getMessage(), errorManager);
                continue;
            }

            // Windows finds bien.z for Bien.z and sub/ for Sub/; the languages are case sensitive, and Linux would not.
            if (!actualPath.equals(declaration.getRelativePath()))
            {
                importError(declaration, "El archivo importado es '" + actualPath + "', no '"
                        + declaration.getRelativePath() + "': los nombres distinguen mayusculas.", errorManager);
                continue;
            }

            // Its path, not just its name: a.Funciones.y and b.Funciones.y are two files.
            String source = declaration.getRelativePath();

            errorManager.setSource(source);
            Program program = parse(content, language, errorManager).ast();
            checkClassName(program, file.getFileName().toString(), errorManager);
            errorManager.setSource(rootSource);

            if (program != null)
            {
                units.add(new Unit(source, program));
            }
        }
        return units;
    }

    /** The other .z files of the folder, parsed apart: their errors are not this file's. */
    private List<Program> siblingClasses(Path rootPath)
    {
        List<Program> siblings = new ArrayList<>();

        if (rootPath == null || rootPath.toAbsolutePath().getParent() == null)
        {
            return siblings;
        }
        try
        {
            for (Path file : FileManager.list(rootPath.toAbsolutePath().getParent()))
            {
                if (file.getFileName().toString().endsWith(".z") && !Files.isSameFile(file, rootPath))
                {
                    Program program = parse(FileManager.read(file), Language.Z, new ErrorManager()).ast();

                    if (program != null)
                    {
                        siblings.add(program);
                    }
                }
            }
        }
        catch (IOException exception)
        {
            LOGGER.log(Level.WARNING, "No se pudieron leer las clases vecinas de " + rootPath, exception);
        }
        return siblings;
    }

    /** Enunciado: "El archivo tiene que llamarse como la clase definida en el". */
    private static void checkClassName(Program program, String fileName, ErrorManager errorManager)
    {
        if (program == null || program.getLanguage() != Language.Z || fileName.isEmpty())
        {
            return;
        }

        String expected = fileName.substring(0, fileName.length() - ".z".length());

        for (AstNode global : program.getGlobals())
        {
            if (global instanceof ClassDeclaration type && !type.getName().equals(expected))
            {
                errorManager.addSemantic("La clase '" + type.getName() + "' debe estar en un archivo "
                        + "llamado '" + type.getName() + ".z', no en '" + fileName + "'.",
                        type.getName(), type.getLine(), type.getColumn());
            }
        }
    }

    private static void importError(ImportDeclaration declaration, String description,
                                    ErrorManager errorManager)
    {
        errorManager.addSemantic(description, declaration.getRelativePath(),
                declaration.getLine(), declaration.getColumn());
    }
}
