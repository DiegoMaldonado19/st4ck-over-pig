package com.dmaldonado.st4ck.over.pig.view;

import com.dmaldonado.st4ck.over.pig.util.FileManager;
import java.nio.file.Path;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

/**
 * One open file: its editor, colored by the lexer of its extension, and the
 * path it is saved to (null until the first save). The title carries a '*'
 * while there are changes the disk does not have.
 */
public final class EditorTab extends Tab
{
    private final CodeArea codeArea;
    private Path           path;
    private boolean        modified;

    public EditorTab(Path path, String content)
    {
        this.path     = path;
        this.codeArea = HighlightingCodeArea.create(path == null ? ""
                : FileManager.extensionOf(path.getFileName().toString()));

        codeArea.replaceText(content);
        // The file opens at its first line, and Ctrl+Z cannot undo the loading: it would empty the tab, and
        // compiling saves every modified tab.
        codeArea.moveTo(0);
        codeArea.requestFollowCaret();
        codeArea.getUndoManager().forgetHistory();
        codeArea.textProperty().addListener((observable, oldText, newText) -> setModified(true));
        setContent(new VirtualizedScrollPane<>(codeArea));
        setModified(false);

        // Closing a tab must not throw away what was never saved without asking.
        setOnCloseRequest(event ->
        {
            if (modified && new Alert(Alert.AlertType.CONFIRMATION,
                    "'" + getFileName() + "' tiene cambios sin guardar. Cerrarlo de todos modos?")
                    .showAndWait().filter(ButtonType.OK::equals).isEmpty())
            {
                event.consume();
            }
        });
    }

    public CodeArea getCodeArea()
    {
        return codeArea;
    }

    /** Null for a file that was never saved. */
    public Path getPath()
    {
        return path;
    }

    /** After a "save as": the new name, and the text is now what the disk has. */
    public void savedAs(Path path)
    {
        this.path = path;
        setModified(false);
    }

    public boolean isModified()
    {
        return modified;
    }

    public String getFileName()
    {
        return path == null ? "Sin titulo" : path.getFileName().toString();
    }

    private void setModified(boolean modified)
    {
        this.modified = modified;
        setText((modified ? "*" : "") + getFileName());
    }
}
