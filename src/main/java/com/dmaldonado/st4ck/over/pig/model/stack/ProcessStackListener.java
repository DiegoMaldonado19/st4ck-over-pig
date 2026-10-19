package com.dmaldonado.st4ck.over.pig.model.stack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Registra la pila de procesos para que la interfaz la reproduzca paso a paso.
 * ANTLR4 es LL(*): no hay pila LR que leer, se simula desde los eventos del
 * recorrido (PUSH, SHIFT, REDUCE, ACCEPT, ERROR).
 *
 * Va sobre el arbol YA construido, no con addParseListener(): durante el parse
 * la prediccion ALL(*) puede probar una regla y retroceder, y el mismo paso
 * saldria dos veces o fuera de orden.
 *
 * Mapeo evento a accion: docs/MANUAL_TECNICO.docx, "Pila de procesos".
 */
public class ProcessStackListener implements ParseTreeListener
{
    private final Parser            parser;
    private final List<ProcessStep> steps = new ArrayList<>();
    private final Deque<String>     stack = new ArrayDeque<>();
    private int counter;

    public ProcessStackListener(Parser parser)
    {
        this.parser = parser;
    }

    public List<ProcessStep> getSteps()
    {
        return steps;
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx)
    {
        String rule = parser.getRuleNames()[ctx.getRuleIndex()];

        stack.push(rule);
        record(StepAction.PUSH, "Se activa la regla <" + rule + ">", ctx.getStart());
    }

    /** getSymbolicName devuelve null para EOF, que si es un terminal del arbol. */
    @Override
    public void visitTerminal(TerminalNode node)
    {
        Token  token = node.getSymbol();
        String name  = parser.getVocabulary().getSymbolicName(token.getType());

        record(StepAction.SHIFT, "Se consume el token " + (name == null ? "EOF" : name)
                + "  ->  '" + token.getText() + "'", token);
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx)
    {
        String rule = parser.getRuleNames()[ctx.getRuleIndex()];

        if (!stack.isEmpty())
        {
            stack.pop();
        }
        record(StepAction.REDUCE, "Se reduce <" + rule + "> a un nodo del arbol", stop(ctx));

        // ANTLR se recupera de un error y termina el arbol igual, asi que reducir
        // la regla inicial no basta para aceptar.
        if (stack.isEmpty() && "programa".equals(rule) && parser.getNumberOfSyntaxErrors() == 0)
        {
            record(StepAction.ACCEPT, "Cadena aceptada por la gramatica", stop(ctx));
        }
    }

    @Override
    public void visitErrorNode(ErrorNode node)
    {
        record(StepAction.ERROR, "Token inesperado: '" + node.getText() + "'", node.getSymbol());
    }

    /**
     * Copia la pila en cada paso: es lo que permite saltar a cualquiera sin
     * rehacer el parse. ArrayDeque itera desde la cima, por eso se invierte.
     */
    private void record(StepAction action, String detail, Token token)
    {
        steps.add(new ProcessStep(++counter, action, detail,
                new ArrayList<>(stack).reversed(),
                token == null ? 0 : token.getLine(),
                token == null ? 0 : token.getCharPositionInLine() + 1));
    }

    /** Una regla sin tokens propios no tiene stop, solo start. */
    private Token stop(ParserRuleContext ctx)
    {
        return ctx.getStop() != null ? ctx.getStop() : ctx.getStart();
    }
}
