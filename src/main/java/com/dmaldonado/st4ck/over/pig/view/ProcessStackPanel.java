package com.dmaldonado.st4ck.over.pig.view;

import com.dmaldonado.st4ck.over.pig.model.stack.ProcessStep;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Replays the simulated parse step by step, with the Anterior / Siguiente
 * buttons the statement asks for.
 *
 * Nothing is recomputed while navigating: every ProcessStep already carries its
 * own copy of the stack and knows how to format it, so moving is just moving an
 * index. The log is rebuilt from the first step up to the current one, which is
 * literally what the statement asks for -- "cada estado y el log de procesos que
 * se realizo para llegar a ese estado" -- and is what makes going backwards as
 * correct as going forwards: appending on every click would duplicate lines the
 * moment the user goes back and forth.
 *
 * It is loaded on every compilation, valid or not: a file that does not parse is
 * exactly the one whose ERROR steps the user needs to read.
 */
public final class ProcessStackPanel extends BorderPane
{
    private final ListView<String> logView        = new ListView<>();
    private final Label            stackLabel     = new Label();
    private final Label            positionLabel  = new Label();
    private final Button           previousButton = new Button("< Anterior");
    private final Button           nextButton     = new Button("Siguiente >");

    private List<ProcessStep> steps        = List.of();
    private List<String>      labels       = List.of();
    private int               currentIndex = -1;

    public ProcessStackPanel()
    {
        previousButton.setOnAction(event -> previous());
        nextButton.setOnAction(event -> next());

        HBox controls = new HBox(8, previousButton, nextButton, positionLabel);
        controls.setPadding(new Insets(8));
        controls.setAlignment(Pos.CENTER_LEFT);

        stackLabel.getStyleClass().add("process-stack");
        stackLabel.setMaxWidth(Double.MAX_VALUE);

        ScrollPane stackScroll = new ScrollPane(stackLabel);
        stackScroll.setFitToWidth(true);

        setTop(controls);
        setCenter(new SplitPane(titled("Pila de llamadas", stackScroll),
                                titled("Log de procesos", logView)));

        clear();
    }

    /** Loads a new run and shows its first step right away. */
    public void load(List<ProcessStep> newSteps)
    {
        this.steps  = newSteps;
        this.labels = describe(newSteps);

        clear();
        next();
    }

    public void next()
    {
        if (currentIndex + 1 < steps.size())
        {
            currentIndex++;
            show();
        }
    }

    public void previous()
    {
        if (currentIndex > 0)
        {
            currentIndex--;
            show();
        }
    }

    private void show()
    {
        ProcessStep step = steps.get(currentIndex);

        logView.getItems().setAll(labels.subList(0, currentIndex + 1));
        logView.getSelectionModel().select(currentIndex);
        logView.scrollTo(currentIndex);

        stackLabel.setText(step.formattedStack());
        positionLabel.setText("Paso " + step.number() + " de " + steps.size());

        previousButton.setDisable(currentIndex == 0);
        nextButton.setDisable(currentIndex + 1 == steps.size());
    }

    private void clear()
    {
        currentIndex = -1;
        logView.getItems().clear();
        stackLabel.setText("");
        positionLabel.setText(steps.isEmpty() ? "Sin pasos que mostrar" : "");
        previousButton.setDisable(true);
        nextButton.setDisable(true);
    }

    private static List<String> describe(List<ProcessStep> steps)
    {
        List<String> descriptions = new ArrayList<>(steps.size());

        for (ProcessStep step : steps)
        {
            descriptions.add("[" + step.number() + "] " + step.action()
                    + "  (" + step.line() + ":" + step.column() + ")  " + step.detail());
        }
        return descriptions;
    }

    /** A panel under its title: the console of MainView is built the same way. */
    static VBox titled(String title, Node content)
    {
        Label label = new Label(title);
        label.getStyleClass().add("panel-title");

        VBox box = new VBox(label, content);
        box.setPadding(new Insets(4));
        VBox.setVgrow(content, Priority.ALWAYS);
        return box;
    }
}
