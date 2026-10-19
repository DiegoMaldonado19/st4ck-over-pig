package com.dmaldonado.st4ck.over.pig.view;

import com.dmaldonado.st4ck.over.pig.model.ast.AstNode;
import com.dmaldonado.st4ck.over.pig.model.codegen.Quadruple;
import com.dmaldonado.st4ck.over.pig.model.errors.CompilerError;
import com.dmaldonado.st4ck.over.pig.model.symbols.Symbol;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * LA VISTA (MVC). Arma la ventana, expone sus controles y pinta un resultado ya
 * calculado. No compila ni lee archivos: ApplicationController es el unico
 * puente al modelo.
 */
public final class MainView extends BorderPane {
    private final TreeView<Path> workspaceTree = new TreeView<>();
    private final TabPane editors = new TabPane();
    private final TableView<CompilerError> errorTable = new TableView<>();
    private final TableView<Symbol> symbolTable = new TableView<>();
    private final TreeView<AstNode> astTree = new TreeView<>();
    private final ProcessStackPanel stackPanel = new ProcessStackPanel();
    private final TableView<Quadruple> quadrupleTable = new TableView<>();
    private final TextArea threeAddressCode = new TextArea();
    private final TextArea assembly    = new TextArea();
    private final TextArea programInput  = new TextArea();
    private final TextArea programOutput = new TextArea();
    private final TabPane tabs = new TabPane();

    // Como campos y no como indices: reordenar las pestanas no puede mandar al
    // usuario a la equivocada.
    private final Tab errorTab    = new Tab("Errores", errorTable);
    private final Tab assemblyTab = new Tab("RISC-V", assembly);
    private final Tab consoleTab  = new Tab("Consola");

    private final Button openFolderButton = new Button("Abrir carpeta");
    private final Button newButton = new Button("Nuevo archivo");
    private final Button newFolderButton = new Button("Nueva carpeta");
    private final Button openButton = new Button("Abrir");
    private final Button saveButton = new Button("Guardar");
    private final Button saveAsButton = new Button("Guardar como");
    private final Button saveAllButton = new Button("Guardar todo");
    private final Button exportFolderButton = new Button("Descargar carpeta");
    private final Button compileButton = new Button("Compilar");
    private final Button runButton = new Button("Ejecutar");
    private final Button exportAssemblyButton = new Button("Descargar .asm");
    private final Button exportTreeButton = new Button("Exportar arbol");

    private final Label statusLabel = new Label("Listo");
    private final Label fileLabel = new Label("Sin archivo");

    public MainView() {
        buildErrorTable();
        buildSymbolTable();
        buildQuadrupleTable();
        buildTrees();
        buildConsole();

        for (TextArea output : List.of(threeAddressCode, assembly, programOutput))
        {
            output.setEditable(false);
            output.getStyleClass().add("code-output");
        }
        exportAssemblyButton.setDisable(true); // nothing valid has been compiled yet
        exportTreeButton.setDisable(true);

        statusLabel.setPadding(new Insets(6, 12, 6, 12));

        setTop(buildToolBar());
        setCenter(buildCenter());
        setBottom(statusLabel);
    }

    /*
     * ================================================================
     * Construction
     * ================================================================
     */

    private ToolBar buildToolBar() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        compileButton.getStyleClass().add("compile-button");
        runButton.getStyleClass().add("compile-button");

        return new ToolBar(openFolderButton, newButton, newFolderButton, openButton, new Separator(),
                saveButton, saveAsButton, saveAllButton, exportFolderButton, new Separator(),
                compileButton, runButton, new Separator(), exportAssemblyButton, exportTreeButton, spacer, fileLabel);
    }

    /** Folder tree | open files | results, left to right. */
    private SplitPane buildCenter() {
        editors.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(errorTab,
                new Tab("AST", astTree),
                new Tab("Tabla de simbolos", symbolTable),
                new Tab("Pila de procesos", stackPanel),
                new Tab("Cuartetas", quadrupleTable),
                new Tab("Codigo 3D", threeAddressCode),
                assemblyTab,
                consoleTab);

        SplitPane split = new SplitPane(workspaceTree, editors, tabs);
        split.setDividerPositions(0.16, 0.56);
        return split;
    }

    /**
     * The workspace shows names, not full paths; the AST shows each node with
     * the line:column it was written at, dimmed to its right.
     */
    private void buildTrees()
    {
        workspaceTree.setCellFactory(tree -> new TreeCell<>()
        {
            @Override
            protected void updateItem(Path item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty || item == null ? null
                        : item.getFileName() == null ? item.toString() : item.getFileName().toString());
            }
        });

        astTree.getStyleClass().add("ast-tree");
        astTree.setCellFactory(tree -> new TreeCell<>()
        {
            private final Label position = new Label();

            @Override
            protected void updateItem(AstNode item, boolean empty)
            {
                super.updateItem(item, empty);

                if (empty || item == null)
                {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                position.setText(item.getLine() + ":" + item.getColumn());
                position.getStyleClass().setAll("ast-position");
                setText(item.getLabel());
                setGraphic(position);
                setContentDisplay(ContentDisplay.RIGHT);
                setGraphicTextGap(12);
            }
        });
    }

    private void buildErrorTable() {
        errorTable.setPlaceholder(new Label("Sin errores."));
        addColumn(errorTable, "Tipo", 85, CompilerError::getType);
        addColumn(errorTable, "Archivo", 85, CompilerError::getSource);
        addColumn(errorTable, "Linea", 50, CompilerError::getLine);
        addColumn(errorTable, "Columna", 60, CompilerError::getColumn);
        addColumn(errorTable, "Lexema", 80, CompilerError::getLexeme);
        addColumn(errorTable, "Descripcion", 620, CompilerError::getDescription);
    }

    private void buildSymbolTable() {
        symbolTable.setPlaceholder(new Label("Sin simbolos: el programa no llego al analisis semantico."));
        addColumn(symbolTable, "Nombre", 160, Symbol::getName);
        addColumn(symbolTable, "Categoria", 100, Symbol::getCategory);
        addColumn(symbolTable, "Tipo", 110, Symbol::getTypeText);
        addColumn(symbolTable, "Ambito", 130, Symbol::getScopeName);
        addColumn(symbolTable, "Linea", 70, Symbol::getLine);
        addColumn(symbolTable, "Columna", 80, Symbol::getColumn);
        addColumn(symbolTable, "Detalle", 300, Symbol::getDetail);
    }

    /** op / arg1 / arg2 / result, plus the position, which is what a goto reads as. */
    private void buildQuadrupleTable()
    {
        quadrupleTable.setPlaceholder(new Label("Sin cuartetas: solo un programa .pig sin errores se traduce."));

        TableColumn<Quadruple, Void> index = new TableColumn<>("#");

        index.setPrefWidth(60);
        index.setSortable(false);
        index.setCellFactory(column -> new TableCell<>()
        {
            @Override
            protected void updateItem(Void item, boolean empty)
            {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex()));
            }
        });
        quadrupleTable.getColumns().add(index);

        addColumn(quadrupleTable, "Operador", 130, Quadruple::op);
        addColumn(quadrupleTable, "Arg 1", 150, Quadruple::arg1);
        addColumn(quadrupleTable, "Arg 2", 150, Quadruple::arg2);
        addColumn(quadrupleTable, "Resultado", 180, Quadruple::result);
    }

    /** What the program reads, written before it runs, over what it printed. */
    private void buildConsole()
    {
        SplitPane console = new SplitPane(ProcessStackPanel.titled("Entrada: una linea por cada lectura", programInput),
                                          ProcessStackPanel.titled("Salida", programOutput));

        programInput.getStyleClass().add("code-output");
        console.setOrientation(Orientation.VERTICAL);
        console.setDividerPositions(0.25);
        consoleTab.setContent(console);
    }

    /**
     * Lambda y no PropertyValueFactory: esa resuelve el getter por reflexion
     * sobre un String y fallaria en ejecucion con una columna vacia. La columna
     * conserva el tipo real del valor, asi el orden por Linea es numerico.
     */
    private static <S, T> void addColumn(TableView<S> table, String title, double width,
            Function<S, T> value) {
        TableColumn<S, T> column = new TableColumn<>(title);

        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    /*
     * ================================================================
     * Painting a result
     * ================================================================
     */

    public void showErrors(List<CompilerError> errors) {
        errorTable.setItems(FXCollections.observableArrayList(errors));
    }

    public void showSymbols(List<Symbol> symbols) {
        symbolTable.setItems(FXCollections.observableArrayList(symbols));
    }

    /** Raiz null limpia la pestana: un programa con errores no se grafica. */
    public void showAst(TreeItem<AstNode> root) {
        astTree.setRoot(root);
        exportTreeButton.setDisable(root == null);
    }

    public void showWorkspace(TreeItem<Path> root)
    {
        workspaceTree.setRoot(root);
    }

    /**
     * A new tab for the file, selected so it is the one the user sees and
     * compiles. It takes the place of the blank tab the window starts with.
     */
    public EditorTab openEditor(Path path, String content)
    {
        EditorTab editor = new EditorTab(path, content);

        editors.getTabs().removeIf(tab -> tab instanceof EditorTab blank && blank.getPath() == null
                && !blank.isModified() && blank.getCodeArea().getText().isEmpty());

        editors.getTabs().add(editor);
        editors.getSelectionModel().select(editor);
        return editor;
    }

    public void showThreeAddressCode(String code)
    {
        threeAddressCode.setText(code);
    }

    public void showAssembly(String code)
    {
        assembly.setText(code);
    }

    public void showQuadruples(List<Quadruple> quadruples)
    {
        quadrupleTable.setItems(FXCollections.observableArrayList(quadruples));
    }

    public void showProgramOutput(String output)
    {
        programOutput.setText(output);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-ok", "status-error");
    }

    public void setStatus(String message, boolean successful) {
        setStatus(message);
        statusLabel.getStyleClass().add(successful ? "status-ok" : "status-error");
    }

    public void setFileName(String name) {
        fileLabel.setText(name);
    }

    /** Adelanta lo que el usuario necesita: los errores, o el resultado. */
    public void showResultTab(boolean successful) {
        tabs.getSelectionModel().select(successful ? assemblyTab : errorTab);
    }

    public void showConsoleTab()
    {
        tabs.getSelectionModel().select(consoleTab);
    }

    /*
     * ================================================================
     * Accessors for the controller
     * ================================================================
     */

    public String getAssemblyText()
    {
        return assembly.getText();
    }

    /** What the program will read: one line per read. */
    public String getProgramInput()
    {
        return programInput.getText();
    }

    /** The file the user is looking at; null when no file is open. */
    public EditorTab getActiveEditor()
    {
        return (EditorTab) editors.getSelectionModel().getSelectedItem();
    }

    public List<EditorTab> getEditors()
    {
        return editors.getTabs().stream().map(EditorTab.class::cast).toList();
    }

    public TabPane getEditorTabs()
    {
        return editors;
    }

    public TreeView<Path> getWorkspaceTree()
    {
        return workspaceTree;
    }

    public TreeView<AstNode> getAstTree()
    {
        return astTree;
    }

    public TableView<CompilerError> getErrorTable() {
        return errorTable;
    }

    public ProcessStackPanel getProcessStackPanel() {
        return stackPanel;
    }

    public Button getNewButton() {
        return newButton;
    }

    public Button getOpenFolderButton()
    {
        return openFolderButton;
    }

    public Button getNewFolderButton()
    {
        return newFolderButton;
    }

    public Button getSaveAsButton()
    {
        return saveAsButton;
    }

    public Button getSaveAllButton()
    {
        return saveAllButton;
    }

    public Button getExportFolderButton()
    {
        return exportFolderButton;
    }

    public Button getExportTreeButton()
    {
        return exportTreeButton;
    }

    public Button getOpenButton() {
        return openButton;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Button getCompileButton() {
        return compileButton;
    }

    public Button getRunButton()
    {
        return runButton;
    }

    public Button getExportAssemblyButton()
    {
        return exportAssemblyButton;
    }
}
