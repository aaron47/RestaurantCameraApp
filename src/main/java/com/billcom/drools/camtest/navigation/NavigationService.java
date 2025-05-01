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

    private void navigate(ActionEvent e, Object controllerToShutdown, String fxmlViewPath, String title, ControllerShutdownCallback shutdownCallback) throws IOException {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();

        // --- Step 1: Shut down the PASSED controller (if applicable) ---
        // Use the explicitly passed controllerToShutdown instead of stage.getUserData()
        if (shutdownCallback != null && controllerToShutdown instanceof Shutdown oldController) {
            System.out.println("Attempting to shutdown passed controller: " + oldController.getClass().getName());
            try {
                // Execute the shutdown logic passed via the callback on the OLD controller
                shutdownCallback.shutdown(oldController);
                System.out.println("Passed controller shutdown successful.");
                // We don't need to clear UserData here, as we didn't rely on it for shutdown
            } catch (Exception ex) {
                System.err.println("Error during shutdown of " + oldController.getClass().getName() + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        } else if (shutdownCallback != null) {
            System.out.println("Shutdown requested, but passed controller is not an instance of Shutdown or is null.");
            System.out.println("Passed controller: " + controllerToShutdown);
        }

        // --- Step 2: Load the NEW view and controller ---
        FXMLLoader loader = new FXMLLoader(RestaurantApplication.class.getResource(fxmlViewPath));
        Parent root = loader.load();
        Object newControllerInstance = loader.getController(); // Get the NEW controller

        // --- Step 3: Prepare the stage for the NEW view ---
        Scene scene = new Scene(root);
        stage.setTitle(title);
        stage.setScene(scene); // Set the NEW scene
        stage.setFullScreen(true);
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setResizable(false);

        // --- Step 4: Store the NEW controller instance in UserData ---
        // This is still useful if you want to access the *current* controller
        // from elsewhere, or potentially for the window's OnCloseRequest.
        if (newControllerInstance instanceof Shutdown newShutdownController) {
            System.out.println("Setting UserData for new controller: " + newShutdownController.getClass().getName());
            stage.setUserData(newShutdownController); // Store the NEW controller

            // Set OnCloseRequest for the *window* closing event, using the NEW controller
            // This handles closing the entire application window.
            stage.setOnCloseRequest(windowEvent -> {
                System.out.println("Window close requested. Shutting down current controller: " + newShutdownController.getClass().getName());
                try {
                    newShutdownController.shutdown();
                } catch (Exception ex) {
                    System.err.println("Error during window close shutdown: " + ex.getMessage());
                }
            });

        } else {
            // If the new controller doesn't implement Shutdown, clear UserData and remove the handler
            stage.setUserData(null);
            stage.setOnCloseRequest(null); // Remove previous handler if any
            System.out.println("New controller does not implement Shutdown. UserData cleared.");
        }

        // --- Step 5: Show the stage ---
        stage.show();
    }

    // Modified signature: Added 'Object currentController' parameter
    public void navigateToView(ActionEvent e, Object currentController, String fxmlViewPath, String title, boolean shutdown) throws IOException {
        if (shutdown) {
            navigate(e, currentController, fxmlViewPath, title, Shutdown::shutdown);
        } else {
            navigate(e, currentController, fxmlViewPath, title, null);
        }
    }


}
