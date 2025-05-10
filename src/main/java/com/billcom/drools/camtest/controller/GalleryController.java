package com.billcom.drools.camtest.controller;


import com.billcom.drools.camtest.inactivity.IdleMonitor;
import com.billcom.drools.camtest.navigation.NavigationService;
import com.billcom.drools.camtest.util.Constants;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class GalleryController implements Initializable, Shutdown {
    // Thumbnail dimensions for touch-friendly interaction
    private static final int THUMBNAIL_SIZE = 225;
    private static final Logger logger = LoggerFactory.getLogger(GalleryController.class);
    private final NavigationService navigationService = new NavigationService();

    @FXML
    private StackPane galleryRoot;

    @FXML
    private ScrollPane galleryScroll;

    @FXML
    private FlowPane galleryFlow;

    @FXML
    private StackPane fullViewOverlay;

    @FXML
    private ImageView fullImageView;

    // List of image files
    private final List<File> imageFiles = new ArrayList<>();

    // Index of the current image shown in full view
    private int currentIndex = -1;

    // Inactivity Monitor
    private IdleMonitor idleMonitor;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind full image view to parent size
        fullImageView.fitWidthProperty().bind(galleryRoot.widthProperty());
        fullImageView.fitHeightProperty().bind(galleryRoot.heightProperty());

        // Responsive wrapping for FlowPane
        galleryScroll.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            galleryFlow.setPrefWrapLength(newBounds.getWidth() - 30); // minor padding compensation
        });

        populateGallery();
        setupFullViewGestureHandlers();

        javafx.application.Platform.runLater(this::setupIdleMonitor);
    }

    private void setupIdleMonitor() {
        this.idleMonitor = new IdleMonitor(
                Duration.seconds(10),
                this::navigateToLandingPage,
                true
        );

        Scene currentScene = this.fullImageView.getScene();
        this.idleMonitor.registerEvent(currentScene, MouseEvent.ANY);
        this.idleMonitor.registerEvent(currentScene, KeyEvent.ANY);
        logger.info("Idle monitor registered for scene events.");
    }

    private void navigateToLandingPage() {
        try {
            this.navigationService.navigateWithoutEvent(fullImageView, this, "fxml/landing-page.fxml", "Landing Page", true);
        } catch (IOException e) {
            logger.debug("Error navigating to landing page: {}", e.getMessage());
        }
    }

    // Populate the gallery with thumbnails
    private void populateGallery() {
        File folder = new File(Constants.IMAGE_FOLDER);
        File[] files = folder.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") ||
                    lower.endsWith(".jpeg") || lower.endsWith(".gif");
        });

        if (files != null) {
            for (File file : files) {
                imageFiles.add(file);
                try {
                    ImageView imageView = getImageView(file);

                    galleryFlow.getChildren().add(imageView);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ImageView getImageView(File file) throws FileNotFoundException {
        FileInputStream inputStream = new FileInputStream(file);
        Image image = new Image(inputStream);
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(THUMBNAIL_SIZE);
        imageView.setFitHeight(THUMBNAIL_SIZE);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCache(true);

        // Open full view on click/touch
        imageView.setOnMouseClicked(event -> {
            int index = imageFiles.indexOf(file);
            if (index != -1) {
                showFullImage(index);
            }
        });
        return imageView;
    }

    // Show the full view overlay for the image at the given index
    private void showFullImage(int index) {
        currentIndex = index;
        updateFullImage();
        fullViewOverlay.setVisible(true);
    }

    // Load the image corresponding to currentIndex
    private void updateFullImage() {
        if (currentIndex >= 0 && currentIndex < imageFiles.size()) {
            File file = imageFiles.get(currentIndex);
            try {
                Image image = new Image(new FileInputStream(file));
                fullImageView.setImage(image);
                // Reset any scaling from previous zoom
                fullImageView.setScaleX(1);
                fullImageView.setScaleY(1);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    // Add gesture listeners for pinch-to-zoom, swipe left/right, and swipe down
    private void setupFullViewGestureHandlers() {
        // Pinch-to-zoom: adjust scale on zoom gesture
        fullImageView.setOnZoom((ZoomEvent event) -> {
            double scale = fullImageView.getScaleX() * event.getZoomFactor();
            fullImageView.setScaleX(scale);
            fullImageView.setScaleY(scale);
            event.consume();
        });

        // Swipe left: show next image
        fullViewOverlay.setOnSwipeLeft((SwipeEvent event) -> {
            if (currentIndex < imageFiles.size() - 1) {
                currentIndex++;
                updateFullImage();
            }
            event.consume();
        });

        // Swipe right: show previous image
        fullViewOverlay.setOnSwipeRight((SwipeEvent event) -> {
            if (currentIndex > 0) {
                currentIndex--;
                updateFullImage();
            }
            event.consume();
        });

        // Swipe down: return to gallery view
        fullViewOverlay.setOnSwipeDown((SwipeEvent event) -> {
            fullViewOverlay.setVisible(false);
            event.consume();
        });

        // Optional: tap anywhere to exit full view (if not zooming or swiping)
        fullViewOverlay.setOnMouseClicked(event -> fullViewOverlay.setVisible(false));
    }

    // Action for the Back button
    @FXML
    private void goBack(ActionEvent event) {
        // You'll replace this with your actual navigation logic
        // For example, loading the landing page
        try {
            this.navigationService.navigateToView(event, this, "fxml/landing-page.fxml", "Restaurant Camera App", false);
            System.out.println("Returning to previous screen...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        // Clean up resources, if any
        System.out.println("Shutting down GalleryController...");
    }
}
