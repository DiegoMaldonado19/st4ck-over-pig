package com.dmaldonado.st4ck.over.pig.model.stack;

import java.util.List;

/**
 * Snapshot of one step of the simulated parse.
 *
 * The stack is stored already copied, from the base to the top, so navigating
 * back and forth in the interface never has to recompute anything: the whole
 * sequence is calculated once, during the walk, and read many times.
 *
 * @param stack the rules that were active at that exact moment, base first.
 */
public record ProcessStep(int number, StepAction action, String detail,
                          List<String> stack, int line, int column)
{
    /** Stack drawn from the top down to the base, for the panel of module 10. */
    public String formattedStack()
    {
        StringBuilder out = new StringBuilder();

        for (int i = stack.size() - 1; i >= 0; i--)
        {
            out.append(i == stack.size() - 1 ? "  > " : "    ")
               .append(stack.get(i)).append('\n');
        }
        return out.toString();
    }
}
