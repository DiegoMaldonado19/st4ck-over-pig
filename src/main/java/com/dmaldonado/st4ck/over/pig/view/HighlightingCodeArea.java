package com.dmaldonado.st4ck.over.pig.view;

import com.dmaldonado.st4ck.over.pig.grammar.PigLexer;
import com.dmaldonado.st4ck.over.pig.grammar.YLexer;
import com.dmaldonado.st4ck.over.pig.grammar.ZLexer;
import com.dmaldonado.st4ck.over.pig.model.CompilerPipeline;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/**
 * El editor colorea con el lexer REAL de su lenguaje, elegido por la extension
 * del archivo: PigLexer, YLexer o ZLexer. El catedratico exigio que el color
 * saliera del analisis lexico propio y no de un plugin: RichTextFX solo pinta
 * los spans.
 *
 * Lo que se compara es el TIPO de token, nunca el texto de la palabra: una
 * palabra reservada es un token cuyo nombre literal en la gramatica es una
 * palabra ('si', 'class', 'VARIABILES>'), asi los tres lenguajes comparten una
 * sola regla y ninguna tabla por lenguaje.
 */
public final class HighlightingCodeArea
{
    private static final Logger LOGGER = Logger.getLogger(HighlightingCodeArea.class.getName());

    /** 'si', 'class', '%funciones', '@Override', 'VARIABILES>': the literal of a reserved word. */
    private static final Pattern KEYWORD = Pattern.compile("'[%@]?[A-Za-z]+[>\\[]?'");

    /** Types and literal values of the three languages, written as their grammar literals. */
    private static final Set<String> TYPES = Set.of(
            "'numerus'", "'decimalis'", "'textum'", "'littera'", "'bool'", "'verum'", "'falsus'",
            "'entero'", "'flotante'", "'cadena'", "'caracter'", "'verdadero'", "'falso'",
            "'int'", "'double'", "'String'", "'char'", "'boolean'", "'void'", "'true'", "'false'",
            "'null'");

    private HighlightingCodeArea()
    {
    }

    /** @param extension pig, y or z; anything else is colored as PigLatin, the main language. */
    public static CodeArea create(String extension)
    {
        CodeArea codeArea = new CodeArea();

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("source-editor");
        codeArea.textProperty().addListener((observable, oldText, newText) ->
                applyHighlighting(codeArea, newText, extension));

        return codeArea;
    }

    /**
     * Regla 6 de CLAUDE.md. Sin este try/catch una excepcion aqui muere dentro
     * del listener de textProperty: el editor deja de colorear en cada tecla y
     * no queda rastro de por que. Se registra con la excepcion completa (que
     * lleva mensaje, archivo y numero de linea) y el texto se queda sin estilo
     * en vez de dejar el editor mudo.
     */
    private static void applyHighlighting(CodeArea codeArea, String text, String extension)
    {
        try
        {
            codeArea.setStyleSpans(0, computeHighlighting(text, extension));
        }
        catch (RuntimeException exception)
        {
            LOGGER.log(Level.SEVERE, "No se pudo colorear el texto del editor.", exception);
        }
    }

    /**
     * setStyleSpans exige que los spans cubran el texto EXACTAMENTE.
     *
     * ponytail: se re-lexa el documento entero en cada tecla. Si alguna vez se
     * siente lento, alimentarlo desde codeArea.multiPlainChanges().
     */
    static StyleSpans<Collection<String>> computeHighlighting(String text, String extension)
    {
        // create() lanza si no se agrego ni un span, y el editor arranca vacio.
        if (text.isEmpty())
        {
            return StyleSpans.<Collection<String>>singleton(Collections.emptyList(), 0);
        }

        Lexer lexer = lexerFor(extension, CharStreams.fromString(text));

        lexer.removeErrorListeners(); // half typed code is the normal state here

        StyleSpansBuilder<Collection<String>> builder    = new StyleSpansBuilder<>();
        Vocabulary                            vocabulary = lexer.getVocabulary();

        // getAllTokens() conserva el canal HIDDEN: por eso las gramaticas mandan
        // comentarios y espacios ahi en vez de descartarlos con skip. Y como
        // ninguna regla usa skip, el flujo es contiguo y cubre todo el texto.
        //
        // La longitud sale del texto del token y NO de stopIndex - startIndex:
        // CharStreams.fromString indexa por puntos de codigo y String por
        // chars, asi que con un emoji los indices del lexer se desalinean y
        // todo lo que va detras se colorearia corrido.
        for (Token token : lexer.getAllTokens())
        {
            // The INDENT / DEDENT of Y? cover no text at all.
            if (!token.getText().isEmpty())
            {
                builder.add(Collections.singleton(styleClassFor(token.getType(), vocabulary)),
                        token.getText().length());
            }
        }
        return builder.create();
    }

    private static Lexer lexerFor(String extension, CharStream chars)
    {
        return switch (extension)
        {
            case "y" -> new YLexer(chars);
            case "z" -> new ZLexer(chars);
            default  -> new PigLexer(chars);
        };
    }

    /** By token type: its literal name for the words, its symbolic name for everything else. */
    private static String styleClassFor(int tokenType, Vocabulary vocabulary)
    {
        String literal = String.valueOf(vocabulary.getLiteralName(tokenType));

        if (TYPES.contains(literal))
        {
            return "type";
        }
        if (KEYWORD.matcher(literal).matches())
        {
            return "keyword";
        }

        String name = String.valueOf(vocabulary.getSymbolicName(tokenType));

        if (CompilerPipeline.LEXICAL_ERRORS.containsKey(name))
        {
            return "invalid";
        }
        return switch (name)
        {
            case "ENTERO", "DECIMAL"                          -> "number";
            case "TEXTO", "CARACTER"                          -> "string";
            case "COMENTARIO_LINEA", "COMENTARIO_BLOQUE",
                 "COMENTARIO_HASH"                            -> "comment";
            default                                           -> "plain";
        };
    }
}
