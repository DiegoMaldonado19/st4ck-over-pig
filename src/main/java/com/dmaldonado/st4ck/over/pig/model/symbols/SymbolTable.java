package com.dmaldonado.st4ck.over.pig.model.symbols;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tabla de simbolos con ambitos anidados. Guarda TRES vistas de los mismos
 * datos, para tres consumidores:
 *
 *  - currentScope: la cadena viva, para el analizador (cerrar oculta locales);
 *  - allSymbols:   historial plano, para graficar cuando ya todo esta cerrado;
 *  - functions:    las sobrecargas por nombre, lo que permite el uso adelantado.
 *
 * Las funciones no viven en los ambitos: tienen su propio espacio de nombres,
 * asi dos sobrecargas no chocan entre si ni con una structura homonima.
 *
 * Tambien reparte el stack: cada variable recibe su slot al declararse. Las
 * globales cuentan desde stack[0]; dentro de un marco (una funcion o MAIOR)
 * se cuenta desde 1, porque stack[P] guarda el valor de retorno.
 */
public class SymbolTable
{
    private final Scope                             globalScope  = new Scope("global", null);
    private Scope                                   currentScope = globalScope;
    private final List<Symbol>                      allSymbols   = new ArrayList<>();
    private final Map<String, List<FunctionSymbol>> functions    = new LinkedHashMap<>();

    private int globalSize;
    /** Next free slot of the frame being analyzed. Frames never nest, so one counter is enough. */
    private int nextSlot;

    /* ----------------- Scopes and frames ----------------- */

    public void openScope(String name)
    {
        currentScope = new Scope(name, currentScope);
    }

    public void closeScope()
    {
        if (currentScope.getParent() != null)
        {
            currentScope = currentScope.getParent();
        }
    }

    /** A function body or MAIOR. Nested scopes inside it keep counting, so no slot is reused. */
    public void openFrame(String name)
    {
        openScope(name);
        nextSlot = 1;
    }

    /** @return the size of the frame just closed. */
    public int closeFrame()
    {
        closeScope();
        return nextSlot;
    }

    public String getCurrentScopeName()
    {
        return currentScope.getName();
    }

    /* ----------------- Declaration and lookup ----------------- */

    /** @return false if the identifier already exists in the CURRENT scope. */
    public boolean declare(Symbol symbol)
    {
        boolean added = currentScope.declare(symbol);

        if (added)
        {
            allSymbols.add(symbol);

            // An attribute declared in a class scope lives in the object, not in the stack.
            if ((symbol instanceof VariableSymbol || symbol instanceof ArraySymbol)
                    && symbol.getCategory() != SymbolCategory.ATTRIBUTE)
            {
                boolean global = currentScope == globalScope;
                symbol.setStorage(global ? globalSize++ : nextSlot++, global);
            }
        }
        return added;
    }

    /** @return false if an overload with the same parameter types already exists. */
    public boolean declareFunction(FunctionSymbol function)
    {
        List<FunctionSymbol> overloads = functions.computeIfAbsent(function.getName(),
                name -> new ArrayList<>());

        if (overloads.stream().anyMatch(function::hasSameParameters))
        {
            return false;
        }
        overloads.add(function);
        allSymbols.add(function);
        return true;
    }

    public Symbol lookup(String name)
    {
        return currentScope.lookup(name);
    }

    public Symbol lookupLocal(String name)
    {
        return currentScope.lookupLocal(name);
    }

    /** Walks the scope chain, so a structura declared inside a function stays inside it. */
    public StructSymbol lookupStruct(String name)
    {
        for (Scope scope = currentScope; scope != null; scope = scope.getParent())
        {
            if (scope.lookupLocal(name) instanceof StructSymbol struct)
            {
                return struct;
            }
        }
        return null;
    }

    /** Every overload with that name; empty when there is none. */
    public List<FunctionSymbol> lookupFunctions(String name)
    {
        return functions.getOrDefault(name, List.of());
    }

    /* ----------------- Reports ----------------- */

    public List<Symbol> getAllSymbols()
    {
        return allSymbols;
    }

    /** Slots taken by the globals: MAIOR's frame starts right after them. */
    public int getGlobalSize()
    {
        return globalSize;
    }
}
