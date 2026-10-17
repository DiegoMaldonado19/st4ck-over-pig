package com.dmaldonado.st4ck.over.pig.util;

import java.util.List;

/**
 * Values shared by the view and the file dialogs.
 *
 * The palette of the editor is NOT here: it lives in st4ck.css, because
 * the one thing that reads it is the style sheet of the CodeArea.
 */
public final class Constants
{
    /** El lenguaje de entrada: un .pig importa los .y y .z que necesite. */
    public static final String MAIN_EXTENSION = "pig";

    /** Los tres lenguajes que el editor abre y colorea. */
    public static final List<String> SOURCE_EXTENSIONS = List.of("pig", "y", "z");

    /** The RISC-V translation, for RARS. */
    public static final String ASSEMBLY_EXTENSION = "asm";

    public static final String APP_TITLE = "St4ck Over-Pig - Compilador a RISC-V";

    private Constants()
    {
    }
}
