package com.dmaldonado.st4ck.over.pig.controller;

import com.dmaldonado.st4ck.over.pig.model.CompilerPipeline;
import com.dmaldonado.st4ck.over.pig.model.CompilerPipeline.CompilationResult;
import com.dmaldonado.st4ck.over.pig.model.RiscVRunner;
import com.dmaldonado.st4ck.over.pig.model.RiscVRunner.RunResult;
import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.errors.CompilerError;
import com.dmaldonado.st4ck.over.pig.util.Constants;
import com.dmaldonado.st4ck.over.pig.util.FileManager;
import com.dmaldonado.st4ck.over.pig.view.AstTreeBuilder;
import com.dmaldonado.st4ck.over.pig.view.EditorTab;
import com.dmaldonado.st4ck.over.pig.view.MainView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableRow;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

/**
 * EL PUENTE (MVC). Unico punto donde la vista y el modelo se encuentran: la
 * vista no sabe de ANTLR y CompilerPipeline no sabe de JavaFX.
 *
 * No hay un CompilerController envolviendo a CompilerPipeline: el modelo ya corre
 * el pipeline y ya devuelve un CompilationResult agnostico de la interfaz.
 */
public class ApplicationController
{
    private static final Logger LOGGER = Logger.getLogger(ApplicationController.class.getName());

    private final MainView         view;
    private final Stage            stage;
    private final CompilerPipeline compiler = new CompilerPipeline();
    private final RiscVRunner      runner   = new RiscVRunner();
    private final FileController   files;

    private CompilationResult lastResult;
    /** The editor lastResult came from: its AST and its errors point into it. */
    private EditorTab         compiled;
    /** The open folder; null until the user opens one. */
    private Path              workspace;

    public ApplicationController(MainView view, Stage stage)
    {
        this.view  = view;
        this.stage = stage;
        this.files = new FileController(stage);

        registerEvents();
        view.openEditor(null, "");
        showFileName();
    }

    private void registerEvents()
    {
        view.getOpenFolderButton().setOnAction(event -> openFolder());
        view.getNewButton().setOnAction(event -> newFile());
        view.getNewFolderButton().setOnAction(event -> newFolder());
        view.getOpenButton().setOnAction(event -> open());
        view.getSaveButton().setOnAction(event -> save(view.getActiveEditor(), false));
        view.getSaveAsButton().setOnAction(event -> save(view.getActiveEditor(), true));
        view.getSaveAllButton().setOnAction(event -> saveAll());
        view.getExportFolderButton().setOnAction(event -> exportFolder());
        view.getCompileButton().setOnAction(event -> compile());
        view.getRunButton().setOnAction(event -> run());
        view.getExportAssemblyButton().setOnAction(event -> exportAssembly());
        view.getExportTreeButton().setOnAction(event -> exportTree());
        view.getEditorTabs().getSelectionModel().selectedItemProperty()
                .addListener((observable, previous, current) -> showFileName());

        // Closing the window must not lose unsaved edits any more than closing one tab does.
        stage.setOnCloseRequest(event ->
        {
            if (view.getEditors().stream().anyMatch(EditorTab::isModified)
                    && new Alert(Alert.AlertType.CONFIRMATION,
                            "Hay archivos con cambios sin guardar. Salir de todos modos?")
                            .showAndWait().filter(ButtonType.OK::equals).isEmpty())
            {
                event.consume();
            }
        });

        // Doble clic en un error lleva el cursor hasta el, aunque este en un archivo importado.
        view.getErrorTable().setRowFactory(table ->
        {
            TableRow<CompilerError> row = new TableRow<>();

            row.setOnMouseClicked(event ->
            {
                if (event.getClickCount() == 2 && !row.isEmpty())
                {
                    jumpTo(row.getItem());
                }
            });
            return row;
        });

        view.getWorkspaceTree().setOnMouseClicked(event ->
        {
            TreeItem<Path> selected = view.getWorkspaceTree().getSelectionModel().getSelectedItem();

            if (event.getClickCount() == 2 && selected != null && Files.isRegularFile(selected.getValue()))
            {
                openFile(selected.getValue());
            }
        });

        // Doble clic en un nodo del arbol lleva a donde esta escrito.
        view.getAstTree().setOnMouseClicked(event ->
        {
            TreeItem<AstNode> selected = view.getAstTree().getSelectionModel().getSelectedItem();

            if (event.getClickCount() == 2 && selected != null && compiled != null)
            {
                moveCaret(compiled, selected.getValue().getLine(), selected.getValue().getColumn());
            }
        });
    }

    /* ================================================================
     * Compilation
     * ================================================================ */

    /**
     * Compiles the file the user is looking at. The imports are read from
     * disk, so the other open files are saved first: what the tabs show is
     * what gets compiled.
     *
     * @return true when it was translated to RISC-V, which only a .pig without errors is.
     */
    private boolean compile()
    {
        EditorTab editor = view.getActiveEditor();

        if (editor == null)
        {
            view.setStatus("Abra o cree un archivo para compilar.", false);
            return false;
        }
        for (EditorTab open : view.getEditors())
        {
            if (open.isModified() && open.getPath() != null)
            {
                save(open, false);
            }
        }

        compiled   = editor;
        lastResult = compiler.compile(editor.getCodeArea().getText(), editor.getPath());

        view.showErrors(lastResult.errors());
        view.showSymbols(lastResult.symbolTable().getAllSymbols());

        // El enunciado prohibe graficar y generar codigo de un programa con
        // errores: se limpian ambos paneles en vez de dejar uno viejo en pantalla.
        view.showAst(lastResult.isValid() ? AstTreeBuilder.build(lastResult.ast()) : null);
        view.showQuadruples(lastResult.quadruples());
        view.showThreeAddressCode(lastResult.threeAddressCode());
        view.showAssembly(lastResult.assembly());
        view.showProgramOutput("");

        // Un .y o un .z valido no se traduce: no tiene MAIOR que ejecutar.
        boolean translated = !lastResult.assembly().isEmpty();
        view.getExportAssemblyButton().setDisable(!translated);

        // La pila se muestra compile o no: el archivo invalido es justo aquel
        // cuyos pasos ERROR hay que leer.
        view.getProcessStackPanel().load(lastResult.steps());

        view.setStatus(summaryOf(lastResult), lastResult.isValid());
        view.showResultTab(translated);
        return translated;
    }

    /**
     * "Ejecutar": compiles what the user sees and runs its RISC-V inside the
     * app, on what the console's input holds. RARS runs apart from the window,
     * which stays usable, and one program at a time: the button waits for it.
     */
    private void run()
    {
        if (!compile())
        {
            return;
        }

        String          assembly  = lastResult.assembly();
        String          input     = view.getProgramInput();
        Task<RunResult> execution = new Task<>()
        {
            @Override
            protected RunResult call()
            {
                return runner.run(assembly, input);
            }
        };

        execution.setOnSucceeded(event -> showRun(execution.getValue()));
        execution.setOnFailed(event ->
        {
            LOGGER.log(Level.SEVERE, "Fallo interno al ejecutar el RISC-V", execution.getException());
            showRun(new RunResult("", "Error interno al ejecutar: " + execution.getException(), false));
        });
        view.getRunButton().setDisable(true);
        view.setStatus("Ejecutando el RISC-V en RARS...");

        Thread thread = new Thread(execution, "riscv-run");

        thread.setDaemon(true);   // closing the window does not wait for a program that never ends
        thread.start();
    }

    /** The output, closed by how the program ended, the way RARS closes its own: "-- ... --". */
    private void showRun(RunResult result)
    {
        String output = result.output();

        view.getRunButton().setDisable(false);
        view.showProgramOutput(output + (output.isEmpty() || output.endsWith("\n") ? "" : "\n")
                + "-- " + result.status() + " --");
        view.setStatus(result.status(), result.successful());
        view.showConsoleTab();
    }

    private static String summaryOf(CompilationResult result)
    {
        String steps = result.steps().size() + " paso(s) de analisis";

        if (result.isValid())
        {
            return "Compilacion exitosa  |  " + result.symbolTable().getAllSymbols().size()
                 + " simbolo(s)  |  " + result.quadruples().size() + " cuarteta(s)  |  " + steps;
        }
        return "Compilacion con " + result.errors().size() + " error(es)  |  " + steps;
    }

    private void jumpTo(CompilerError error)
    {
        EditorTab editor = editorOf(error.getSource());

        if (editor == null)
        {
            view.setStatus("No se encontro el archivo " + error.getSource() + " para mostrar el error.");
            return;
        }
        moveCaret(editor, error.getLine(), error.getColumn());
    }

    /**
     * The tab of the file an error names, opened again if it was closed: the
     * compiled one, or an import, whose source is its path relative to the
     * compiled file's folder.
     */
    private EditorTab editorOf(String source)
    {
        if (compiled == null || compiled.getPath() == null)
        {
            return source.isEmpty() ? compiled : null;
        }

        boolean root = source.isEmpty() || source.equals(compiled.getFileName());

        return openFile(root ? compiled.getPath() : compiled.getPath().toAbsolutePath().getParent().resolve(source));
    }

    private void moveCaret(EditorTab editor, int line, int column)
    {
        CodeArea codeArea = editor.getCodeArea();

        view.getEditorTabs().getSelectionModel().select(editor);

        if (codeArea.getParagraphs().isEmpty())
        {
            return;
        }

        // El error es 1-based y el editor 0-based, y hay que acotar: un error al
        // final del archivo puede traer una columna mayor al largo de su linea.
        int paragraph = Math.max(0, Math.min(line - 1, codeArea.getParagraphs().size() - 1));
        int position  = Math.max(0, Math.min(column - 1, codeArea.getParagraphLength(paragraph)));

        codeArea.moveTo(paragraph, position);
        codeArea.requestFollowCaret();
        codeArea.requestFocus();
    }

    /* ================================================================
     * Files and folders
     * ================================================================ */

    private void openFolder()
    {
        Path folder = files.chooseFolder("Abrir carpeta de trabajo");

        if (folder != null)
        {
            workspace = folder.toAbsolutePath().normalize();
            refreshWorkspace();
            view.setStatus("Carpeta de trabajo: " + workspace);
        }
    }

    /** Without a folder it is an untitled tab; with one, a file created where the tree points. */
    private void newFile()
    {
        if (workspace == null)
        {
            view.openEditor(null, "");
            view.setStatus("Nuevo archivo");
            return;
        }

        Path created = files.newFile(selectedFolder());

        if (created != null)
        {
            refreshWorkspace();
            openFile(created);
        }
    }

    private void newFolder()
    {
        if (workspace == null)
        {
            view.setStatus("Abra primero una carpeta de trabajo.", false);
            return;
        }
        if (files.newFolder(selectedFolder()) != null)
        {
            refreshWorkspace();
        }
    }

    /** The folder selected in the tree, the folder of the selected file, or the workspace itself. */
    private Path selectedFolder()
    {
        TreeItem<Path> selected = view.getWorkspaceTree().getSelectionModel().getSelectedItem();

        if (selected == null)
        {
            return workspace;
        }
        return Files.isDirectory(selected.getValue()) ? selected.getValue() : selected.getValue().getParent();
    }

    private void open()
    {
        Path chosen = files.chooseFile();

        if (chosen != null && openFile(chosen) != null)
        {
            compile();
        }
    }

    /** Selects the file's tab, opening it first when it is not open yet. */
    private EditorTab openFile(Path path)
    {
        Path file = path.toAbsolutePath().normalize();

        for (EditorTab editor : view.getEditors())
        {
            if (file.equals(editor.getPath()))
            {
                view.getEditorTabs().getSelectionModel().select(editor);
                return editor;
            }
        }

        String content = files.read(file);

        return content == null ? null : view.openEditor(file, content);
    }

    /**
     * "Guardar como" is also how a single file is downloaded. A file that gets
     * a new name is reopened: its tab colors by the extension it now has.
     */
    private void save(EditorTab editor, boolean asNewFile)
    {
        if (editor == null)
        {
            return;
        }

        boolean renamed = asNewFile || editor.getPath() == null;
        Path    saved   = renamed ? files.saveAs(editor) : files.save(editor);

        if (saved == null)
        {
            return;
        }
        if (renamed)
        {
            String content = editor.getCodeArea().getText();
            Path   file    = saved.toAbsolutePath().normalize();

            // A tab already open on that file holds what the disk no longer has.
            view.getEditorTabs().getTabs().removeIf(tab -> tab == editor || file.equals(((EditorTab) tab).getPath()));

            EditorTab reopened = view.openEditor(file, content);

            if (compiled == editor)
            {
                compiled = reopened;
            }
        }
        else
        {
            editor.savedAs(saved);
        }
        refreshWorkspace();
        view.setStatus("Guardado en " + saved.toAbsolutePath());
    }

    private void saveAll()
    {
        for (EditorTab editor : view.getEditors())
        {
            if (editor.isModified())
            {
                save(editor, false);
            }
        }
    }

    private void exportFolder()
    {
        if (workspace == null)
        {
            view.setStatus("Abra primero una carpeta de trabajo.", false);
            return;
        }

        Path copy = files.exportFolder(workspace);

        if (copy != null)
        {
            view.setStatus("Carpeta descargada en " + copy);
        }
    }

    /** The RISC-V the user sees, next to the file it came from: ready for RARS. */
    private void exportAssembly()
    {
        if (lastResult == null || lastResult.assembly().isEmpty())
        {
            return;
        }

        Path saved = files.saveOutput(compiled.getPath(), view.getAssemblyText(), Constants.ASSEMBLY_EXTENSION);

        if (saved != null)
        {
            refreshWorkspace();
            view.setStatus("Ensamblador RISC-V descargado en " + saved.toAbsolutePath());
        }
    }

    private void exportTree()
    {
        if (lastResult == null || !lastResult.isValid())
        {
            return;
        }

        Path saved = files.saveText("Exportar arbol", AstTreeBuilder.toText(lastResult.ast()));

        if (saved != null)
        {
            view.setStatus("Arbol exportado en " + saved.toAbsolutePath());
        }
    }

    /**
     * Rebuilt from disk, keeping open the folders that were open and selected
     * the entry that was: "Nuevo archivo" creates where the selection points.
     */
    private void refreshWorkspace()
    {
        if (workspace == null)
        {
            return;
        }

        TreeView<Path> tree     = view.getWorkspaceTree();
        TreeItem<Path> selected = tree.getSelectionModel().getSelectedItem();
        Set<Path>      expanded = new HashSet<>(Set.of(workspace));

        for (int row = 0; row < tree.getExpandedItemCount(); row++)
        {
            if (tree.getTreeItem(row).isExpanded())
            {
                expanded.add(tree.getTreeItem(row).getValue());
            }
        }
        view.showWorkspace(treeOf(workspace, expanded));

        for (int row = 0; selected != null && row < tree.getExpandedItemCount(); row++)
        {
            if (tree.getTreeItem(row).getValue().equals(selected.getValue()))
            {
                tree.getSelectionModel().select(row);
            }
        }
    }

    private static TreeItem<Path> treeOf(Path path, Set<Path> expanded)
    {
        TreeItem<Path> item = new TreeItem<>(path);

        item.setExpanded(expanded.contains(path));
        if (Files.isDirectory(path))
        {
            try
            {
                for (Path child : FileManager.list(path))
                {
                    item.getChildren().add(treeOf(child, expanded));
                }
            }
            catch (IOException exception)
            {
                LOGGER.log(Level.SEVERE, "No se pudo leer la carpeta " + path, exception);
            }
        }
        return item;
    }

    private void showFileName()
    {
        EditorTab editor = view.getActiveEditor();
        String    name   = editor == null ? "Sin archivo" : editor.getFileName();

        view.setFileName(name);
        stage.setTitle(Constants.APP_TITLE + "  -  " + name);
    }
}
