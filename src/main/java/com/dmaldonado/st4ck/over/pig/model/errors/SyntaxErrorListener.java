package com.dmaldonado.st4ck.over.pig.model.errors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

/**
 * Reemplaza al ConsoleErrorListener de ANTLR: en vez de imprimir en ingles a
 * System.err, cada error de sintaxis va al ErrorManager en espanol y con la
 * columna 1-based.
 *
 * No existe su equivalente lexico: ver CompilerPipeline.reportLexicalErrors.
 */
public class SyntaxErrorListener extends BaseErrorListener
{
    private static final String MISSING_FINIS = "El programa termina sin cerrarse: falta 'FINIS;' al final.";

    private final ErrorManager errorManager;

    public SyntaxErrorListener(ErrorManager errorManager)
    {
        this.errorManager = errorManager;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String message,
                            RecognitionException exception)
    {
        String lexeme = (offendingSymbol instanceof Token token)
                ? token.getText() : String.valueOf(offendingSymbol);

        // The INDENT / DEDENT of Y? cover no text: the error is in the indentation itself.
        if (lexeme.isEmpty() && offendingSymbol instanceof Token token && token.getType() != Token.EOF)
        {
            lexeme = "(sangria)";
        }

        Token previous = missingAfter(recognizer, offendingSymbol, message);

        if (previous != null)
        {
            // Lo que falta pertenece a la linea donde termino lo anterior, no a
            // la siguiente instruccion: un "finis;" olvidado se senala en su "}".
            line               = previous.getLine();
            charPositionInLine = previous.getCharPositionInLine() + previous.getText().length();
        }
        String description = endOfFile(offendingSymbol, message) ? MISSING_FINIS : translate(message);

        // "x = a = b": the second '=' was meant as a comparison, where one would fit.
        if ("=".equals(lexeme) && message != null && message.contains("'=='"))
        {
            description += " Para comparar use '=='.";
        }
        errorManager.addSyntactic(description, lexeme, line, charPositionInLine + 1);
    }

    /**
     * A PigLatin program cut short: ANTLR would list every token that could
     * follow, when the fix is always the same closing line.
     */
    private static boolean endOfFile(Object offendingSymbol, String message)
    {
        return offendingSymbol instanceof Token token && token.getType() == Token.EOF
                && message != null && message.contains("'FINIS'");
    }

    /**
     * The token after which something is missing, when the parser only found
     * out on a later line; null otherwise. Only ANTLR's "missing" and
     * "mismatched" messages mean that: an "extraneous" token IS the error.
     */
    private Token missingAfter(Recognizer<?, ?> recognizer, Object offendingSymbol, String message)
    {
        if (message == null || !(message.startsWith("missing") || message.startsWith("mismatched"))
                || !(recognizer instanceof Parser parser) || !(offendingSymbol instanceof Token offending))
        {
            return null;
        }

        Token previous = parser.getInputStream().LT(-1);

        return (previous != null && previous.getLine() < offending.getLine()) ? previous : null;
    }

    /** ANTLR only speaks English; the interface has to read in Spanish. */
    private String translate(String message)
    {
        if (message == null)
        {
            return "Error de sintaxis.";
        }
        return message
                // ANTLR quotes the input it could not parse with its line breaks escaped: 'int a\n    int'.
                .replaceAll("(\\\\[nrt])+ *", " ")
                .replace("extraneous input ''", "sangria inesperada")
                .replace("mismatched input ''", "no se esperaba un cambio de sangria")
                .replace("DEDENT", "fin de bloque")
                .replace("INDENT", "sangria")
                .replace("mismatched input", "no se esperaba")
                .replace("no viable alternative at input", "construccion invalida en")
                .replace("extraneous input", "sobra el token")
                .replace(" expecting", ", se esperaba")
                .replace("missing", "falta")
                .replace("alternative", "alternativa")
                // ANTLR appends "at '<token>'"; only that exact shape is
                // replaced, so the word "at" inside a lexeme is left alone.
                .replace(" at ''", " al final de la linea")
                .replace(" at '", " en '");
    }
}
