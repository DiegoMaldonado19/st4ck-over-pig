package com.dmaldonado.st4ck.over.pig.controller;

import com.dmaldonado.st4ck.over.pig.util.Constants;
import com.dmaldonado.st4ck.over.pig.util.FileManager;
import com.dmaldonado.st4ck.over.pig.view.EditorTab;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/**
 * The verbs the statement asks for, over files AND folders: abrir, guardar y
 * descargar. It talks to the user through dialogs; the disk itself is
 * FileManager's.
 *
 * Every method returns null when the user cancels or the disk fails; a
 * failure is also shown, because that is the case the user must not miss.
 */
public class FileController
{
    private static final Logger LOGGER = Logger.getLogger(FileController.class.getName());

    private static final String SOURCES = "Archivos fuente (*.pig, *.y, *.z)";

    private final Window owner;
    /** Where the last dialog was left, so the next one opens there. */
    private Path         lastFolder;

    public FileController(Window owner)
    {
        this.owner = owner;
    }

    public Path chooseFile()
    {
        FileChooser chooser = chooser("Abrir archivo fuente", SOURCES, Constants.SOURCE_EXTENSIONS);

        // The assistant's test files are PigLatin saved as .lat.
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Todos los archivos (*.*)", "*.*"));
        return remember(toPath(chooser.showOpenDialog(owner)));
    }

    public Path chooseFolder(String title)
    {
        DirectoryChooser chooser = new DirectoryChooser();

        chooser.setTitle(title);
        if (lastFolder != null && Files.isDirectory(lastFolder))
        {
            chooser.setInitialDirectory(lastFolder.toFile());
        }
        return remember(toPath(chooser.showDialog(owner)));
    }

    /** @return the content, or null when it could not be read. */
    public String read(Path path)
    {
        try
        {
            return FileManager.read(path);
        }
        catch (IOException exception)
        {
            report("No se pudo abrir el archivo", path, exception);
            return null;
        }
    }

    /** A file never saved asks where to go; the rest go back where they came from. */
    public Path save(EditorTab editor)
    {
        if (editor.getPath() == null)
        {
            return saveAs(editor);
        }
        return write(editor.getPath(), editor.getCodeArea().getText()) ? editor.getPath() : null;
    }

    /** Also how a single file is "descargado": a copy wherever the user wants it. */
    public Path saveAs(EditorTab editor)
    {
        Path destination = chooseDestination("Guardar archivo como", SOURCES, Constants.SOURCE_EXTENSIONS,
                editor.getPath() == null ? Constants.MAIN_EXTENSION
                        : FileManager.extensionOf(editor.getPath().getFileName().toString()));

        return destination != null && write(destination, editor.getCodeArea().getText()) ? destination : null;
    }

    /** Downloads generated code next to its source, with the same base name and the given extension. */
    public Path saveOutput(Path source, String content, String extension)
    {
        Path destination = source != null ? FileManager.outputPathFor(source, extension)
                : chooseDestination("Guardar ." + extension, "Codigo (*." + extension + ")", List.of(extension),
                        extension);

        return destination != null && write(destination, content) ? destination : null;
    }

    public Path saveText(String title, String content)
    {
        Path destination = chooseDestination(title, "Texto (*.txt)", List.of("txt"), "txt");

        return destination != null && write(destination, content) ? destination : null;
    }

    /** A new empty file inside the folder, named by the user. */
    public Path newFile(Path folder)
    {
        Path file = askPath(folder, "Nuevo archivo", "Nombre del archivo (.pig, .y o .z):", "nuevo.pig");

        if (file == null)
        {
            return null;
        }
        if (Files.exists(file))
        {
            report("Ya existe", file, new IOException(file.getFileName() + " ya existe en la carpeta."));
            return null;
        }
        return write(file, "") ? file : null;
    }

    public Path newFolder(Path parent)
    {
        Path folder = askPath(parent, "Nueva carpeta", "Nombre de la carpeta:", "carpeta");

        if (folder == null)
        {
            return null;
        }
        try
        {
            return Files.createDirectories(folder);
        }
        catch (IOException exception)
        {
            report("No se pudo crear la carpeta", folder, exception);
            return null;
        }
    }

    /** "Descargar" the workspace: a full copy inside the folder the user picks. */
    public Path exportFolder(Path workspace)
    {
        Path destination = chooseFolder("Descargar carpeta en...");

        if (destination == null)
        {
            return null;
        }

        // A drive root (E:\) has no name of its own.
        Path copy = destination.resolve(workspace.getFileName() == null ? "carpeta"
                : workspace.getFileName().toString());

        try
        {
            FileManager.copyFolder(workspace, copy);
            return copy;
        }
        catch (IOException exception)
        {
            report("No se pudo copiar la carpeta", copy, exception);
            return null;
        }
    }

    /** The name the user types, inside the folder; null when cancelled or not a valid name ("Y?" on Windows). */
    private Path askPath(Path folder, String title, String prompt, String suggestion)
    {
        TextInputDialog dialog = new TextInputDialog(suggestion);

        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(prompt);

        String name = dialog.showAndWait().map(String::trim).filter(typed -> !typed.isEmpty()).orElse(null);

        if (name == null)
        {
            return null;
        }
        try
        {
            return folder.resolve(name);
        }
        catch (InvalidPathException exception)
        {
            report("Nombre no valido", folder, new IOException(exception.getMessage(), exception));
            return null;
        }
    }

    private Path chooseDestination(String title, String description, List<String> extensions,
                                   String extension)
    {
        Path selected = toPath(chooser(title, description, extensions).showSaveDialog(owner));

        return selected == null ? null : remember(FileManager.ensureExtension(selected, extensions, extension));
    }

    private FileChooser chooser(String title, String description, List<String> extensions)
    {
        FileChooser chooser = new FileChooser();

        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description,
                extensions.stream().map(extension -> "*." + extension).toList()));

        // FileChooser throws if the initial directory no longer exists, which is
        // exactly what happens when the last file was opened from a removed USB.
        if (lastFolder != null && Files.isDirectory(lastFolder))
        {
            chooser.setInitialDirectory(lastFolder.toFile());
        }
        return chooser;
    }

    private Path remember(Path chosen)
    {
        if (chosen != null)
        {
            lastFolder = Files.isDirectory(chosen) ? chosen : chosen.getParent();
        }
        return chosen;
    }

    private static Path toPath(File file)
    {
        return file == null ? null : file.toPath();
    }

    /**
     * A successful save reports through the status bar, not through a dialog:
     * a modal window on every save is one extra click every time. A failure
     * DOES open one.
     */
    private boolean write(Path path, String content)
    {
        try
        {
            FileManager.write(path, content);
            return true;
        }
        catch (IOException exception)
        {
            report("No se pudo guardar el archivo", path, exception);
            return false;
        }
    }

    /**
     * Rule 6 of CLAUDE.md: the dialog tells the user what failed in one line,
     * and the log keeps the whole exception. Logger prints the throwable with
     * its stack trace, so the message, the file and the line number are all
     * recorded and none of it is swallowed.
     */
    private void report(String message, Path path, IOException exception)
    {
        LOGGER.log(Level.SEVERE, message + ": " + path.toAbsolutePath(), exception);

        Alert alert = new Alert(Alert.AlertType.ERROR, message + ":\n" + exception.getMessage());

        alert.initOwner(owner);
        alert.showAndWait();
    }
}
