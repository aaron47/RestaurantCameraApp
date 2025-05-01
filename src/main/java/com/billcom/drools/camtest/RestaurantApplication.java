package com.billcom.drools.camtest;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;

public class RestaurantApplication extends Application {

    static {
        Loader.load(opencv_java.class);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("fxml/landing-page.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("Restaurant Camera App");
        primaryStage.setScene(new Scene(root));
        primaryStage.setFullScreen(true);
        primaryStage.setResizable(false);
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}