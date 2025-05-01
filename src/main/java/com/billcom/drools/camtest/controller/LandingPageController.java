package com.billcom.drools.camtest.controller;

import com.billcom.drools.camtest.RestaurantApplication;
import com.billcom.drools.camtest.navigation.NavigationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LandingPageController implements Shutdown, Initializable {

    private final NavigationService navigationService;
    private MediaPlayer mediaPlayer;

    @FXML
    private MediaView restaurantVideoView;

    @FXML
    private StackPane restaurantVideoContainer; // You should give the StackPane an fx:id in the FXML

    public LandingPageController() {
        this.navigationService = new NavigationService();
    }

    @FXML
    private void navigateToCameraView(ActionEvent e) throws IOException {
        this.navigationService.navigateToView(e, this, "fxml/camera-view.fxml", "Take your photo!", true);
    }

    @FXML
    private void navigateToGalleryView(ActionEvent e) throws IOException {
        this.navigationService.navigateToView(e, this, "fxml/gallery-view.fxml", "Gallery", true);
    }

    @Override
    public void shutdown() {
        System.out.println("Shutdown called on LandingPageController");
        if (mediaPlayer != null) {
            System.out.println("Stopping MediaPlayer...");
            mediaPlayer.stop();
            System.out.println("Disposing MediaPlayer...");
            mediaPlayer.dispose();
            mediaPlayer = null;
            System.out.println("MediaPlayer stopped and disposed.");
        } else {
            System.out.println("MediaPlayer was already null during shutdown.");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("Initializing LandingPageController...");
        try {
            URL videoFileUrl = RestaurantApplication.class.getResource("Restaurant le Méditerranée.mp4");

            if (videoFileUrl == null) {
                System.err.println("Video file not found in resources.");
                return;
            }

            String videoFilePath = videoFileUrl.toExternalForm();

            Media media = new Media(videoFilePath);
            mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setOnError(() -> {
                System.err.println("MediaPlayer Error: " + mediaPlayer.getError());
            });

            mediaPlayer.setOnReady(() -> {
                System.out.println("MediaPlayer Ready. Assigning to MediaView.");
                restaurantVideoView.setMediaPlayer(mediaPlayer);
                mediaPlayer.play();
                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            });

            // Configure MediaView for full-screen display
            restaurantVideoView.setPreserveRatio(false); // Allow stretching to fill the entire screen
            restaurantVideoView.getStyleClass().add("media-view"); // Add the CSS class for styling

            // 🔁 Bind the MediaView size to the StackPane
            restaurantVideoView.fitWidthProperty().bind(restaurantVideoContainer.widthProperty());
            restaurantVideoView.fitHeightProperty().bind(restaurantVideoContainer.heightProperty());

        } catch (Exception e) {
            System.err.println("Error during LandingPageController initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
