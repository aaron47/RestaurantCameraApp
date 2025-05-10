package com.billcom.drools.camtest.controller;

import com.billcom.drools.camtest.RestaurantApplication;
import com.billcom.drools.camtest.navigation.NavigationService;
import com.billcom.drools.camtest.util.Constants;
import com.billcom.drools.camtest.util.EmailSender;
import com.billcom.drools.camtest.dialog.QRCodeDialog;
import com.billcom.drools.camtest.drive.GoogleDriveService;
import com.billcom.drools.camtest.util.FileService;
import java.awt.Dimension;
import java.util.Arrays;

import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.util.Duration;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

public class CameraController implements Shutdown {
    // Logger for this class
    private static final Logger logger = LoggerFactory.getLogger(CameraController.class);

    // fxml binded attributes
    @FXML
    private ImageView cameraView;
    @FXML
    private ImageView processedImageView;
    @FXML
    private Button saveBtn;
    @FXML
    private Button emailBtn;
    @FXML
    private Button switchCameraBtn;
    @FXML
    private Button captureBtn;
    @FXML
    private Button qrCodeBtn;
    @FXML
    private BorderPane cameraPanel;
    @FXML
    private Button timer2Button;
    @FXML
    private Button timer5Button;
    @FXML
    private Button timer10Button;
    @FXML
    private Button cancelTimerBtn;

    // Camera attributes
    private Webcam webcam;
    private boolean cameraActive = false;

    // Executors for camera and QR code generation
    private ScheduledExecutorService cameraTimer;
    private ScheduledExecutorService qrCodeExecutor;

    // Image file for saving
    private File lastSavedImageFile;
    private int cameraIndex = 0;
    // countdown
    private Label countDownLabel;
    private Timeline countDownTimeline;
    private boolean countDownActive = false;
    private int timerDuration = 5; // Default timer duration in seconds

    // border management
    private static final Map<String, Image> borders = new HashMap<>();
    private String currentBorder = null;

    // Store the raw camera frame without borders
    private WritableImage rawCameraFrame = null;

    private final NavigationService navigationService = new NavigationService();

    // Methods for initialising necessary functionality for the application
    @FXML
    public void initialize() {
        this.startCamera();
        cameraView.fitWidthProperty().bind(cameraPanel.widthProperty());
        cameraView.fitHeightProperty().bind(cameraPanel.heightProperty());
        this.setupCountdownTimer();
        this.loadBorders();

        // Initialize timer button styles (default is 5 seconds)
        javafx.application.Platform.runLater(() -> {
            updateTimerButtonStyles(timer5Button);

            // Ensure cancel button is properly initialized
            cancelTimerBtn.setVisible(false);
            cancelTimerBtn.toFront();
        });
    }

    private void loadBorders() {
        try {
            borders.put("christmas1", new Image(RestaurantApplication.class.getResourceAsStream("borders/christmas1.png")));
            borders.put("christmas2", new Image(RestaurantApplication.class.getResourceAsStream("borders/christmas2.png")));
            borders.put("christmas3", new Image(RestaurantApplication.class.getResourceAsStream("borders/christmas3.png")));
            borders.put("valentines", new Image(RestaurantApplication.class.getResourceAsStream("borders/valentines.png")));
            logger.info("Border images loaded successfully");
        } catch (Exception e) {
            logger.error("Error loading border images: {}", e.getMessage(), e);
        }
    }

    private void setupCountdownTimer() {
        countDownLabel = new Label(String.valueOf(timerDuration));
        countDownLabel.setFont(Font.font("Arial", 48));
        countDownLabel.setStyle("-fx-text-fill: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.7), 10, 0, 0, 0);");
        countDownLabel.setVisible(false);

        // Add the countdown label to the camera view's parent (StackPane)
        javafx.application.Platform.runLater(() -> {
            StackPane cameraContainer = (StackPane) cameraView.getParent();
            cameraContainer.getChildren().add(countDownLabel);
        });

        updateCountdownTimeline();
    }

    /**
     * Displays an error message in the camera view when the camera fails to initialize
     * This helps maintain the UI layout even when the camera doesn't work
     */
    private void displayCameraErrorMessage(String errorMessage) {
        javafx.application.Platform.runLater(() -> {
            try {
                // Create a VBox to hold the error message
                VBox errorBox = new VBox();
                errorBox.setAlignment(Pos.CENTER);
                errorBox.setStyle("-fx-background-color: #333333; -fx-padding: 20px;");
                errorBox.setPrefSize(640, 480); // Set preferred size to maintain layout

                // Create a label with the error message
                Label errorLabel = new Label(errorMessage);
                errorLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
                errorLabel.setWrapText(true);

                // Add a generic camera icon
                FontIcon cameraIcon = new FontIcon("fas-camera-retro");
                cameraIcon.setIconSize(48);
                cameraIcon.setIconColor(Color.RED);

                // Add components to the VBox
                errorBox.getChildren().addAll(cameraIcon, errorLabel);
                errorBox.setSpacing(15);

                // Create a scene to hold the error box (needed for snapshot)
                javafx.scene.Scene tempScene = new javafx.scene.Scene(errorBox);

                // Create a snapshot of the error box
                WritableImage errorImage = errorBox.snapshot(new javafx.scene.SnapshotParameters(), null);

                // Set the error image to the camera view
                cameraView.setImage(errorImage);

                // Disable capture button
                captureBtn.setDisable(true);
            } catch (Exception e) {
                logger.error("Error displaying camera error message: {}", e.getMessage(), e);

                // Fallback to a simple colored rectangle if the fancy error display fails
                WritableImage fallbackImage = new WritableImage(640, 480);
                PixelWriter pw = fallbackImage.getPixelWriter();
                for (int y = 0; y < 480; y++) {
                    for (int x = 0; x < 640; x++) {
                        pw.setColor(x, y, Color.DARKGRAY);
                    }
                }
                cameraView.setImage(fallbackImage);
                captureBtn.setDisable(true);
            }
        });
    }

    private void updateCountdownTimeline() {
        // Create countdown timeline
        if (countDownTimeline != null) {
            countDownTimeline.stop();
        }

        countDownTimeline = new Timeline();
        countDownTimeline.setCycleCount(timerDuration);

        KeyFrame keyFrame = new KeyFrame(Duration.seconds(1), event -> {
            int remainingSeconds = Integer.parseInt(countDownLabel.getText()) - 1;
            countDownLabel.setText(String.valueOf(remainingSeconds));

            if (remainingSeconds <= 0) {
                countDownTimeline.stop();
                countDownLabel.setVisible(false);
                countDownActive = false;
                capturePhoto();
                cancelTimerBtn.setVisible(false);
            }
        });

        countDownTimeline.getKeyFrames().add(keyFrame);
        countDownTimeline.setOnFinished(event -> {
            countDownLabel.setText(String.valueOf(timerDuration));
        });
    }

    @FXML
    private void setTimer2Seconds() {
        timerDuration = 2;
        updateCountdownTimeline();
        updateTimerButtonStyles(timer2Button);
        logger.info("Timer set to 2 seconds");
    }

    @FXML
    private void setTimer5Seconds() {
        timerDuration = 5;
        updateCountdownTimeline();
        updateTimerButtonStyles(timer5Button);
    }

    @FXML
    private void setTimer10Seconds() {
        timerDuration = 10;
        updateCountdownTimeline();
        updateTimerButtonStyles(timer10Button);
    }

    private void updateTimerButtonStyles(Button selectedButton) {
        // Remove selected style from all buttons
        timer2Button.getStyleClass().remove("timer-button-selected");
        timer5Button.getStyleClass().remove("timer-button-selected");
        timer10Button.getStyleClass().remove("timer-button-selected");

        // Add selected style to the clicked button
        if (!selectedButton.getStyleClass().contains("timer-button-selected")) {
            selectedButton.getStyleClass().add("timer-button-selected");
        }
    }

    private void startCamera() {
        if (!cameraActive) {
            try {
                // Try to open the camera with specified index
                List<Webcam> webcams = Webcam.getWebcams();
                if (webcams.isEmpty()) {
                    logger.error("No webcams found");
                    return;
                }

                // Try to get the camera with the specified index, or default to the first one
                if (cameraIndex < webcams.size()) {
                    webcam = webcams.get(cameraIndex);
                } else {
                    webcam = webcams.get(0);
                    cameraIndex = 0;
                }

                // Try to set resolution to VGA (640x480) which is more widely supported
                try {
                    webcam.setViewSize(WebcamResolution.VGA.getSize());
                } catch (IllegalArgumentException e) {
                    // If VGA is not supported, try to use a supported resolution
                    Dimension[] supportedSizes = webcam.getViewSizes();
                    if (supportedSizes.length > 0) {
                        // Use the first available resolution (usually the smallest)
                        webcam.setViewSize(supportedSizes[0]);
                        logger.info("Using camera resolution: {}x{}", supportedSizes[0].width, supportedSizes[0].height);
                    } else {
                        logger.error("No supported resolutions found for camera");
                        return;
                    }
                }

                // Open the webcam
                webcam.open();

                if (webcam.isOpen()) {
                    cameraActive = true;

                    // Start the camera capture thread
                    cameraTimer = Executors.newSingleThreadScheduledExecutor();
                    cameraTimer.scheduleAtFixedRate(this::updateFrame, 0, 33, TimeUnit.MILLISECONDS);
                    logger.info("Camera started successfully");
                } else {
                    logger.error("Failed to open camera with index: {}", cameraIndex);
                    displayCameraErrorMessage("Failed to open camera");
                }
            } catch (Exception e) {
                logger.error("Error starting camera: {}", e.getMessage(), e);
                displayCameraErrorMessage("Camera error: " + e.getMessage());
            }
        }
    }

    private void updateFrame() {
        if (webcam != null && webcam.isOpen()) {
            try {
                // Get image from webcam
                BufferedImage bufferedImage = webcam.getImage();

                if (bufferedImage != null) {
                    // Convert BufferedImage to JavaFX WritableImage
                    WritableImage writableImage = convertToFxImage(bufferedImage);

                    // Store the raw camera frame without borders
                    rawCameraFrame = writableImage;

                    // Update the ImageView on the JavaFX Application Thread
                    javafx.application.Platform.runLater(() -> {
                        // Always start with the raw frame
                        cameraView.setImage(writableImage);

                        // Apply border for preview if needed
                        if (currentBorder != null && borders.containsKey(currentBorder)) {
                            StackPane previewPane = new StackPane();
                            // Set alignment to center for proper border positioning
                            previewPane.setAlignment(Pos.CENTER);

                            ImageView imageView = new ImageView(writableImage);
                            ImageView borderView = new ImageView(borders.get(currentBorder));

                            // Ensure border is sized correctly to match the image
                            borderView.setFitWidth(writableImage.getWidth());
                            borderView.setFitHeight(writableImage.getHeight());
                            borderView.setPreserveRatio(false);

                            previewPane.getChildren().addAll(imageView, borderView);

                            WritableImage previewImage = new WritableImage(
                                    (int) writableImage.getWidth(), (int) writableImage.getHeight());

                            previewPane.snapshot(null, previewImage);
                            cameraView.setImage(previewImage);
                        }
                    });
                }
            } catch (Exception e) {
                logger.error("Error updating frame: {}", e.getMessage(), e);
            }
        }
    }

    @FXML
    private void onCapture() {
        if (cameraActive) {
            if (countDownActive) {
                // If countdown is already active, do nothing
                return;
            }

            // Start countdown
            countDownLabel.setText(String.valueOf(timerDuration));
            countDownLabel.setVisible(true);
            countDownActive = true;
            countDownTimeline.playFromStart();

            // Show cancel button
            cancelTimerBtn.setVisible(true);
        } else {
            logger.error("Camera is not active");
        }
    }

    @FXML
    private void onCancelTimer() {
        if (countDownActive) {
            // Stop the countdown
            countDownTimeline.stop();
            countDownLabel.setVisible(false);
            countDownActive = false;

            // Hide cancel button
            cancelTimerBtn.setVisible(false);

            // Reset the countdown label
            countDownLabel.setText(String.valueOf(timerDuration));
        }
    }

    private void capturePhoto() {
        if (cameraActive && rawCameraFrame != null) {
            try {
                // Create a copy of the current frame
                WritableImage capturedImage = new WritableImage(
                        (int) rawCameraFrame.getWidth(),
                        (int) rawCameraFrame.getHeight());

                // Copy pixels from the raw frame
                PixelReader pixelReader = rawCameraFrame.getPixelReader();
                PixelWriter pixelWriter = capturedImage.getPixelWriter();

                for (int y = 0; y < rawCameraFrame.getHeight(); y++) {
                    for (int x = 0; x < rawCameraFrame.getWidth(); x++) {
                        pixelWriter.setArgb(x, y, pixelReader.getArgb(x, y));
                    }
                }

                // Apply border if selected
                if (currentBorder != null && borders.containsKey(currentBorder)) {
                    StackPane capturePane = new StackPane();
                    // Set alignment to center for proper border positioning
                    capturePane.setAlignment(Pos.CENTER);

                    ImageView imageView = new ImageView(capturedImage);
                    ImageView borderView = new ImageView(borders.get(currentBorder));

                    // Ensure border is sized correctly to match the image
                    borderView.setFitWidth(capturedImage.getWidth());
                    borderView.setFitHeight(capturedImage.getHeight());
                    borderView.setPreserveRatio(false);

                    capturePane.getChildren().addAll(imageView, borderView);

                    WritableImage borderedImage = new WritableImage(
                            (int) capturedImage.getWidth(), (int) capturedImage.getHeight());

                    capturePane.snapshot(null, borderedImage);
                    capturedImage = borderedImage;
                }

                // Resize the image to fit within the processed image container (max 330x350)
                // while preserving aspect ratio
                double maxWidth = 330.0;
                double maxHeight = 350.0;
                double imgWidth = capturedImage.getWidth();
                double imgHeight = capturedImage.getHeight();
                double scale = Math.min(maxWidth / imgWidth, maxHeight / imgHeight);

                // Only scale down, not up
                if (scale < 1.0) {
                    int newWidth = (int) (imgWidth * scale);
                    int newHeight = (int) (imgHeight * scale);

                    // Create a new scaled image
                    WritableImage scaledImage = new WritableImage(newWidth, newHeight);
                    PixelWriter scaledWriter = scaledImage.getPixelWriter();

                    // Simple scaling algorithm - could be improved for better quality
                    // Get pixel reader from the bordered image
                    PixelReader borderedPixelReader = capturedImage.getPixelReader();
                    for (int y = 0; y < newHeight; y++) {
                        for (int x = 0; x < newWidth; x++) {
                            int srcX = (int) (x / scale);
                            int srcY = (int) (y / scale);
                            scaledWriter.setArgb(x, y, borderedPixelReader.getArgb(srcX, srcY));
                        }
                    }

                    capturedImage = scaledImage;
                    logger.info("Resized image to {}x{}", newWidth, newHeight);
                }

                // Display the captured image
                processedImageView.setImage(capturedImage);

                // Enable save and email buttons
                saveBtn.setDisable(false);
                emailBtn.setDisable(false);
                qrCodeBtn.setDisable(false);

                logger.info("Photo captured successfully");
            } catch (Exception e) {
                logger.error("Error capturing photo: {}", e.getMessage(), e);
            }
        } else {
            logger.error("Cannot capture photo: Camera not active or no frame available");
        }
    }

    private boolean showUserAgreementDialog(Stage ownerStage) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("User Agreement");
        alert.setHeaderText("Terms of Use");
        alert.setContentText("By saving this photo, you agree that:\n\n" +
                "1. The photo may be displayed in the restaurant and on social media.\n" +
                "2. You have permission from all individuals in the photo to share it.\n\n" +
                "Do you agree to these terms?");
        alert.initOwner(ownerStage);

        // Set modality to APPLICATION_MODAL to block input to other windows
        alert.initModality(Modality.APPLICATION_MODAL);

        // Get the stage and set its owner to the main application window
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);

        // Show the dialog and wait for a response
        return alert.showAndWait()
                .filter(response -> response == javafx.scene.control.ButtonType.OK)
                .isPresent();
    }

    @FXML
    private void onSave() {
        if (processedImageView.getImage() == null) {
            logger.error("No image to save");
            return;
        }

        Stage currentStage = (Stage) processedImageView.getScene().getWindow();

        // Show user agreement dialog
        if (!showUserAgreementDialog(currentStage)) {
            logger.info("User declined the agreement");
            return;
        }

        try {
            // Create a unique filename based on current date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            String fileName = "photo_" + timestamp + ".png";

            // Get the directory to save the image
            File saveDir = FileService.getOrCreatePhotoDirectory();
            lastSavedImageFile = new File(saveDir, fileName);

            // Convert the JavaFX image to a BufferedImage
            BufferedImage bufferedImage = convertFromFxImage(processedImageView.getImage());

            // Save the image
            ImageIO.write(bufferedImage, "png", lastSavedImageFile);

            logger.info("Image saved to: {}", lastSavedImageFile.getAbsolutePath());

            // Show success alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initStyle(StageStyle.UTILITY);
            alert.initOwner(currentStage);
            alert.setTitle("Success");
            alert.setHeaderText("Photo Saved");
            alert.setContentText("Your photo has been saved successfully!");
            alert.showAndWait();

        } catch (IOException e) {
            logger.error("Error saving image: {}", e.getMessage(), e);

            // Show error alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initStyle(StageStyle.UTILITY);
            alert.initOwner(currentStage);
            alert.setTitle("Error");
            alert.setHeaderText("Save Failed");
            alert.setContentText("Failed to save the photo: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onEmailPhoto() {
        if (processedImageView.getImage() == null) {
            logger.error("No image to email");
            return;
        }
        Stage currentStage = (Stage) processedImageView.getScene().getWindow();

        // Show user agreement dialog
        if (!showUserAgreementDialog(currentStage)) {
            logger.info("User declined the agreement");
            return;
        }

        try {
            // Convert the JavaFX image to a BufferedImage
            BufferedImage bufferedImage = convertFromFxImage(processedImageView.getImage());

            // Convert BufferedImage to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            // Create a unique filename based on current date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = dateFormat.format(new Date());
            String fileName = "photo_" + timestamp + ".png";

            // Show email dialog for user to input email address
            EmailSender.showEmailDialogForByteArray(
                    currentStage,
                    "Photo from Restaurant le Méditerranée",
                    "Please find attached a photo taken at Restaurant le Méditerranée.",
                    fileName,
                    imageBytes
            );

        } catch (Exception e) {
            logger.error("Error preparing image for email: {}", e.getMessage(), e);

            // Show error alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.initModality(Modality.APPLICATION_MODAL);
            alert.initStyle(StageStyle.UTILITY);
            alert.initOwner(currentStage);
            alert.setTitle("Error");
            alert.setHeaderText("Email Preparation Failed");
            alert.setContentText("Failed to prepare the photo for email: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void onSwitchCamera() {
        if (cameraActive) {
            // Stop the current camera
            if (cameraTimer != null) {
                cameraTimer.shutdown();
                try {
                    cameraTimer.awaitTermination(33, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }

            cameraActive = false;

            // Switch to the next camera
            List<Webcam> webcams = Webcam.getWebcams();
            if (webcams.size() > 1) {
                cameraIndex = (cameraIndex + 1) % webcams.size();
                logger.info("Switching to camera index: {}", cameraIndex);
            } else {
                logger.info("Only one camera available, staying with current camera");
            }

            // Restart the camera
            startCamera();
        }
    }

    @FXML
    private void onChristmas1BorderSelected() {
        currentBorder = "christmas1";
        logger.info("Christmas border 1 selected");
    }

    @FXML
    private void onChristmas2BorderSelected() {
        currentBorder = "christmas2";
        logger.info("Christmas border 2 selected");
    }

    @FXML
    private void onChristmas3BorderSelected() {
        currentBorder = "christmas3";
        logger.info("Christmas border 3 selected");
    }

    @FXML
    private void onValentinesBorderSelected() {
        currentBorder = "valentines";
        logger.info("Valentines border selected");
    }

    @FXML
    private void onClearBorderSelected() {
        currentBorder = null;
        logger.info("Border cleared");
    }

    @FXML
    private void onGenerateQRCode() {
        if (processedImageView.getImage() == null) {
            logger.error("No image to generate QR code for");
            return;
        }
        Stage currentStage = (Stage) processedImageView.getScene().getWindow();

        Stage stage = (Stage) processedImageView.getScene().getWindow();
        // Show user agreement dialog
        if (!showUserAgreementDialog(stage)) {
            logger.info("User declined the agreement");
            return;
        }

        // Convert the JavaFX image to a BufferedImage
        BufferedImage bufferedImage = convertFromFxImage(processedImageView.getImage());

        // Convert BufferedImage to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, "png", baos);
        } catch (IOException e) {
            logger.error("Error converting image: {}", e.getMessage(), e);
            return;
        }

        byte[] imageBytes = baos.toByteArray();

        // Create and show the QR code dialog
        QRCodeDialog qrCodeDialog = new QRCodeDialog(stage);
        qrCodeDialog.show();

        // Initialize QR code executor if needed
        if (qrCodeExecutor == null || qrCodeExecutor.isShutdown()) {
            qrCodeExecutor = Executors.newSingleThreadScheduledExecutor();
        }

        // Create a task to generate the QR code
        Task<Image> qrCodeTask = new Task<>() {
            @Override
            protected Image call() throws Exception {
                String uploadedImageLink = GoogleDriveService.uploadImageToDrive(imageBytes, "photo.png");

                return GoogleDriveService.generateQRCode(uploadedImageLink, 300, 300);
            }
        };

        qrCodeTask.setOnSucceeded(event -> qrCodeDialog.setQRCodeImage(qrCodeTask.getValue()));

        qrCodeTask.setOnFailed(event -> qrCodeDialog.showError("Failed to generate QR code: " + qrCodeTask.getException().getMessage()));

        qrCodeExecutor.submit(qrCodeTask);
    }

    @FXML
    private void onHome(ActionEvent e) throws IOException {
        this.navigationService.navigateToView(e, this, "fxml/landing-page.fxml", "Landing Page", true);
    }

    // Method to convert BufferedImage to JavaFX WritableImage
    private WritableImage convertToFxImage(BufferedImage bufferedImage) {
        WritableImage writableImage = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                pixelWriter.setArgb(x, y, bufferedImage.getRGB(x, y));
            }
        }

        return writableImage;
    }

    private BufferedImage convertFromFxImage(Image fxImage) {
        int width = (int) fxImage.getWidth();
        int height = (int) fxImage.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        PixelReader pixelReader = fxImage.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, pixelReader.getArgb(x, y));
            }
        }

        return bufferedImage;
    }

    // Method responsible for releasing all the application resources when the app is closed
    @Override
    public void shutdown() {
        if (cameraTimer != null && !cameraTimer.isShutdown()) {
            cameraTimer.shutdown();
            try {
                cameraTimer.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                logger.error("Error shutting down camera timer: {}", e.getMessage());
            }
        }

        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }

        if (this.qrCodeExecutor != null && !this.qrCodeExecutor.isShutdown()) {
            qrCodeExecutor.shutdown();
            // Allow tasks to complete within a reasonable timeout
            try {
                qrCodeExecutor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Error shutting down QR code executor: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        if (countDownTimeline != null) {
            countDownTimeline.stop();
        }

        cameraView.setImage(null);
        processedImageView.setImage(null);
        rawCameraFrame = null;
        borders.clear();

        EmailSender.shutdown();
        logger.info("Camera resources released");
        logger.info("Email resources released");
    }
}
