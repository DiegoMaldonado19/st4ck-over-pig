package com.dmaldonado.st4ck.over.pig.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Pure I/O helpers: no dialogs and no JavaFX.
 *
 * FileController owns the conversation with the user; this only touches the
 * disk, which is what makes both halves testable on their own.
 */
public final class FileManager
{
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private FileManager()
    {
    }

    /**
     * UTF-8, as the app writes. A file saved as ANSI by a Windows editor still
     * opens, and the BOM Notepad may put first is not code: the lexer would
     * report it as an unknown symbol.
     */
    public static String read(Path path) throws IOException
    {
        String text;

        try
        {
            text = Files.readString(path, StandardCharsets.UTF_8);
        }
        catch (MalformedInputException notUtf8)
        {
            text = new String(Files.readAllBytes(path), WINDOWS_1252);
        }
        return !text.isEmpty() && text.charAt(0) == 0xFEFF ? text.substring(1) : text;
    }

    public static void write(Path path, String content) throws IOException
    {
        if (path.getParent() != null)
        {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    /** Keeps an extension the dialog allows ("utils.y" from a .pig tab); otherwise adds the default one. */
    public static Path ensureExtension(Path path, List<String> allowed, String extension)
    {
        String name = path.getFileName().toString();

        return allowed.contains(extensionOf(name)) ? path : path.resolveSibling(name + "." + extension);
    }

    /** A folder's entries, folders first and then by name, the way a file explorer lists them. */
    public static List<Path> list(Path folder) throws IOException
    {
        try (Stream<Path> entries = Files.list(folder))
        {
            return entries.sorted(Comparator.comparing((Path entry) -> !Files.isDirectory(entry))
                            .thenComparing(entry -> entry.getFileName().toString().toLowerCase()))
                          .toList();
        }
    }

    /**
     * "Descargar" a whole folder: a copy of everything inside it, subfolders included.
     * A target inside the source would be walked while it is written, copying itself forever.
     * Files.walk reports an unreadable subfolder unchecked; it leaves as the IOException it wraps.
     */
    public static void copyFolder(Path source, Path target) throws IOException
    {
        if (target.toAbsolutePath().normalize().startsWith(source.toAbsolutePath().normalize()))
        {
            throw new IOException("La carpeta no se puede descargar dentro de si misma: elija otro destino.");
        }
        try (Stream<Path> paths = Files.walk(source))
        {
            for (Path path : (Iterable<Path>) paths::iterator)
            {
                Path destination = target.resolve(source.relativize(path).toString());

                if (Files.isDirectory(path))
                {
                    Files.createDirectories(destination);
                }
                else
                {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        catch (UncheckedIOException exception)
        {
            throw exception.getCause();
        }
    }

    /** "Pila.z" gives "z"; a name without a dot gives "". */
    public static String extensionOf(String fileName)
    {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    /** Swaps the source extension for .c keeping the base name. */
    public static Path outputPathFor(Path sourcePath)
    {
        String name = sourcePath.getFileName().toString();
        int    dot  = name.lastIndexOf('.');
        String base = (dot > 0) ? name.substring(0, dot) : name;

        return sourcePath.resolveSibling(base + "." + Constants.OUTPUT_EXTENSION);
    }
}
