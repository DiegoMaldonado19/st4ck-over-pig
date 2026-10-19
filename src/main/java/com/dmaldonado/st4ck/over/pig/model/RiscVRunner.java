package com.dmaldonado.st4ck.over.pig.model;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import rars.AssemblyException;
import rars.SimulationException;
import rars.api.Options;
import rars.api.Program;
import rars.simulator.Simulator;

/**
 * Runs the RISC-V of a program inside the app, on the simulator of RARS 1.6
 * (rars.api), so nothing has to be run in a terminal. The same .asm runs
 * outside too, in RARS's own interface or its command line. As on the command
 * line, the input is given up front: one line per read.
 *
 * RARS keeps the machine in static fields, so one program runs at a time.
 */
public class RiscVRunner
{
    private static final Logger LOGGER    = Logger.getLogger(RiscVRunner.class.getName());

    /**
     * About 10 times the heaviest example (Laberinto, 1.08 M instructions). A
     * program that never ends, or that waits for a line the input does not
     * have, stops here instead of running forever.
     *
     * ponytail: a fixed cap, 4 to 13 s of RARS's time; a stop button if programs ever need longer.
     */
    private static final int    MAX_STEPS = 11_000_000;

    /**
     * @param output     what the program printed, also when it stopped halfway.
     * @param status     how it ended, in one line.
     * @param successful whether it ran to its end.
     */
    public record RunResult(String output, String status, boolean successful)
    {
    }

    public RunResult run(String assembly, String input)
    {
        Options options = new Options();

        options.startAtMain = true;   // the command line's "sm"
        options.maxSteps    = MAX_STEPS;

        Program program = new Program(options);

        try
        {
            program.assembleString(assembly);
            program.setup(new ArrayList<>(), input);

            if (program.simulate() == Simulator.Reason.MAX_STEPS)
            {
                return new RunResult(program.getSTDOUT(), "Se detuvo tras " + MAX_STEPS / 1_000_000
                        + " millones de instrucciones: un ciclo que no termina, o una lectura sin su linea en la "
                        + "entrada", false);
            }
            return new RunResult(program.getSTDOUT(), "Programa terminado (codigo de salida "
                    + program.getExitCode() + ")", true);
        }
        catch (AssemblyException exception)
        {
            LOGGER.log(Level.SEVERE, "RARS no pudo ensamblar el RISC-V generado", exception);
            return new RunResult(exception.errors().generateErrorReport(), "RARS no pudo ensamblar el RISC-V generado",
                    false);
        }
        catch (SimulationException exception)
        {
            LOGGER.log(Level.SEVERE, "Error de ejecucion en RARS", exception);
            return new RunResult(program.getSTDOUT(), "Error de ejecucion: " + (exception.error() == null
                    ? exception.getMessage() : exception.error().getMessage()), false);
        }
    }
}
