package com.dmaldonado.st4ck.over.pig;

import com.dmaldonado.st4ck.over.pig.controller.ApplicationController;
import com.dmaldonado.st4ck.over.pig.view.MainView;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Entry point: builds the view, hands it to the controller and shows it.
 *
 * Everything else lives one layer down. This class exists to satisfy the
 * lifecycle of JavaFX and nothing more.
 */
public class App extends Application
{
    @Override
    public void start(Stage stage)
    {
        MainView view  = new MainView();
        Scene    scene = new Scene(view, 1440, 880);

        scene.getStylesheets().add(App.class.getResource("/st4ck.css").toExternalForm());

        new ApplicationController(view, stage);

        stage.setScene(scene);
        stage.show();
        fitToScreen(stage);
    }

    /**
     * 1440x880 es el tamano deseado, no una promesa: con las decoraciones de la
     * ventana son 918 px de alto, y en una pantalla de 1536x816 (1920x1080 al
     * 125 %) Windows sube la ventana hasta dejar la barra de titulo -- y con
     * ella el boton de cerrar -- fuera del monitor.
     *
     * Se ajusta despues de show() a proposito: hasta ese momento getWidth() y
     * getHeight() no incluyen el marco, y es el marco lo que no cabe.
     */
    private static void fitToScreen(Stage stage)
    {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        stage.setWidth(Math.min(stage.getWidth(), screen.getWidth()));
        stage.setHeight(Math.min(stage.getHeight(), screen.getHeight()));
        stage.centerOnScreen();
    }

    public static void main(String[] args)
    {
        launch();
    }
}
