package com.billcom.drools.camtest.controller;

import com.billcom.drools.camtest.RestaurantApplication;
import com.billcom.drools.camtest.navigation.NavigationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private StackPane restaurantVideoContainer;

    // Flag to track if fallback background has been shown
    private boolean fallbackBackgroundShown = false;

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

    /**
     * Creates a fallback background when the video fails to load
     */
    private void showFallbackBackground() {
        // Only show the fallback once
        if (fallbackBackgroundShown) {
            return;
        }

        fallbackBackgroundShown = true;

        // Run on JavaFX Application Thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Try to load the restaurant logo as a fallback
                Image logoImage = new Image(RestaurantApplication.class.getResourceAsStream("restaurant_logo.png"));

                // Create an ImageView for the logo
                ImageView logoView = new ImageView(logoImage);
                logoView.setPreserveRatio(true);
                logoView.setFitWidth(300);

                // Create a background pane with a gradient
                StackPane backgroundPane = new StackPane();
                backgroundPane.setStyle("-fx-background-color: linear-gradient(to bottom, #1a2a6c, #b21f1f, #fdbb2d);");

                // Add the logo to the background using getChildren().addAll()
                backgroundPane.getChildren().addAll(logoView);

                // Add the background to the container (at index 0 to be behind the content)
                if (!restaurantVideoContainer.getChildren().contains(backgroundPane)) {
                    // Use addAll for consistency
                    restaurantVideoContainer.getChildren().add(0, backgroundPane);
                }

                // Bind the background size to the container
                backgroundPane.prefWidthProperty().bind(restaurantVideoContainer.widthProperty());
                backgroundPane.prefHeightProperty().bind(restaurantVideoContainer.heightProperty());

                System.out.println("Fallback background shown");
            } catch (Exception e) {
                System.err.println("Error showing fallback background: " + e.getMessage());
                e.printStackTrace();

                // Last resort: just set a solid color background
                restaurantVideoContainer.setStyle("-fx-background-color: #2a3a5c;");
            }
        });
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
            // Try to load the video file using different approaches
            URL videoFileUrl = null;

            // First attempt: direct resource loading
            videoFileUrl = RestaurantApplication.class.getResource("Restaurant le Méditerranée.mp4");

            // Second attempt: try with a simpler filename if the first attempt failed
            if (videoFileUrl == null) {
                // Try with a hardcoded filename (without special characters)
                videoFileUrl = RestaurantApplication.class.getResource("Restaurant.mp4");
                if (videoFileUrl != null) {
                    System.out.println("Found video file with simplified name: Restaurant.mp4");
                }
            }

            // Third attempt: try to load from the class path root
            if (videoFileUrl == null) {
                // Try to find the video file in the classpath
                URL classpathRoot = RestaurantApplication.class.getResource("/");
                if (classpathRoot != null) {
                    System.out.println("Classpath root: " + classpathRoot);
                    // Try to list resources in the package
                    try {
                        URL packageUrl = new URL(classpathRoot, "com/billcom/drools/camtest/");
                        System.out.println("Looking for video in: " + packageUrl);

                        // Try direct access with the full path
                        URL fullPathUrl = new URL(packageUrl, "Restaurant le Méditerranée.mp4");
                        // Test if the URL is accessible
                        try {
                            fullPathUrl.openStream().close();
                            videoFileUrl = fullPathUrl;
                            System.out.println("Found video file via direct URL access");
                        } catch (IOException e) {
                            System.err.println("Could not access video file via direct URL: " + e.getMessage());
                        }
                    } catch (Exception e) {
                        System.err.println("Error exploring classpath: " + e.getMessage());
                    }
                } else {
                    System.err.println("Could not find classpath root");
                }
            }

            if (videoFileUrl == null) {
                System.err.println("Video file not found in resources.");
                return;
            }

            System.out.println("Video file URL: " + videoFileUrl);
            String videoFilePath = videoFileUrl.toExternalForm();
            System.out.println("Video file path: " + videoFilePath);

            try {
                Media media = new Media(videoFilePath);
                mediaPlayer = new MediaPlayer(media);

                mediaPlayer.setOnError(() -> {
                    System.err.println("MediaPlayer Error: " + mediaPlayer.getError());
                    // Display error details
                    if (mediaPlayer.getError() != null) {
                        System.err.println("Error type: " + mediaPlayer.getError().getType());
                        System.err.println("Error message: " + mediaPlayer.getError().getMessage());
                        if (mediaPlayer.getError().getCause() != null) {
                            System.err.println("Cause: " + mediaPlayer.getError().getCause().getMessage());
                        }
                    }
                    // Show fallback background
                    showFallbackBackground();
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
                System.err.println("Error creating MediaPlayer: " + e.getMessage());
                e.printStackTrace();
                // Show fallback background
                showFallbackBackground();
            }

        } catch (Exception e) {
            System.err.println("Error during LandingPageController initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
