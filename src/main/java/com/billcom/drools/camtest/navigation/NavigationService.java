package com.billcom.drools.camtest.navigation;

import com.billcom.drools.camtest.RestaurantApplication;
import com.billcom.drools.camtest.controller.Shutdown;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

import java.io.IOException;

public class NavigationService {

    /**
     * Navigation triggered by a UI event (e.g. button click)
     */
    public void navigateToView(ActionEvent event, Object currentController, String fxmlViewPath, String title, boolean shutdown) throws IOException {
        Stage stage = extractStageFromEvent(event);
        navigate(stage, currentController, fxmlViewPath, title, shutdown ? Shutdown::shutdown : null);
    }

    /**
     * Navigation triggered programmatically (e.g. from a timer or other logic)
     */
    public void navigateWithoutEvent(Node anyNodeInScene, Object currentController, String fxmlViewPath, String title, boolean shutdown) throws IOException {
        Stage stage = (Stage) anyNodeInScene.getScene().getWindow();
        navigate(stage, currentController, fxmlViewPath, title, shutdown ? Shutdown::shutdown : null);
    }

    /**
     * Internal method that performs the actual navigation
     */
    private void navigate(Stage stage, Object oldController, String fxmlViewPath, String title, ControllerShutdownCallback shutdownCallback) throws IOException {
        // Shutdown logic
        if (shutdownCallback != null && oldController instanceof Shutdown shutdownController) {
            try {
                System.out.println("Shutting down controller: " + shutdownController.getClass().getName());
                shutdownCallback.shutdown(shutdownController);
            } catch (Exception ex) {
                System.err.println("Error during shutdown: " + ex.getMessage());
                ex.printStackTrace();
            }
        }

        // Load new scene
        FXMLLoader loader = new FXMLLoader(RestaurantApplication.class.getResource(fxmlViewPath));
        Parent root = loader.load();
        Object newController = loader.getController();

        // Configure scene and stage
        Scene scene = new Scene(root);
        stage.setTitle(title);
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setResizable(false);

        // Store controller for later shutdown
        if (newController instanceof Shutdown newShutdownController) {
            stage.setUserData(newShutdownController);
            stage.setOnCloseRequest(e -> {
                try {
                    newShutdownController.shutdown();
                } catch (Exception ex) {
                    System.err.println("Error during window close: " + ex.getMessage());
                }
            });
        } else {
            stage.setUserData(null);
            stage.setOnCloseRequest(null);
        }

        stage.show();
    }

    /**
     * Extracts the JavaFX Stage from an ActionEvent
     */
    private Stage extractStageFromEvent(ActionEvent e) {
        Object source = e.getSource();
        if (source instanceof Node node) {
            return (Stage) node.getScene().getWindow();
        }
        throw new IllegalArgumentException("Event source is not a JavaFX Node");
    }
}
