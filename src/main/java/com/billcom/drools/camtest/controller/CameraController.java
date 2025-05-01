package com.billcom.drools.camtest.controller;

import com.billcom.drools.camtest.RestaurantApplication;
import com.billcom.drools.camtest.navigation.NavigationService;
import com.billcom.drools.camtest.util.Constants;
import com.billcom.drools.camtest.util.EmailSender;
import com.billcom.drools.camtest.dialog.QRCodeDialog;
import com.billcom.drools.camtest.drive.GoogleDriveService;
import com.billcom.drools.camtest.util.FileService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.util.Duration;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

public class CameraController implements Shutdown {
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

    // OpenCV attributes
    private VideoCapture capture;
    private CascadeClassifier faceDetector;
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

    // face detection intervals
    private static final int FACE_DETECTION_INTERVAL = 10;
    private int frameCounter = 0;

    private final NavigationService navigationService = new NavigationService();

    // Methods for initialising necessary functionality for the application
    @FXML
    public void initialize() {
        this.startCamera();
        this.loadFaceDetectionModel();
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
        } catch (Exception e) {
            System.err.println("Error loading border images: " + e.getMessage());
            e.printStackTrace();
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

    private void updateCountdownTimeline() {
        // Create countdown timeline
        if (countDownTimeline != null) {
            countDownTimeline.stop();
        }

        countDownTimeline = new Timeline();
        countDownTimeline.setCycleCount(1);

        // Create key frames for countdown
        countDownTimeline.getKeyFrames().clear();

        // Add frames for each second of the countdown
        for (int i = 0; i < timerDuration; i++) {
            final int secondsLeft = timerDuration - i;
            KeyFrame frame = new KeyFrame(
                Duration.seconds(i), 
                event -> countDownLabel.setText(String.valueOf(secondsLeft))
            );
            countDownTimeline.getKeyFrames().add(frame);
        }

        // Add "SMILE!" frame
        KeyFrame smileFrame = new KeyFrame(
            Duration.seconds(timerDuration), 
            event -> countDownLabel.setText("SMILE!")
        );

        // Add the final frame to actually take the picture (0.5 seconds after "SMILE!")
        KeyFrame captureFrame = new KeyFrame(
            Duration.seconds(timerDuration + 0.5), 
            event -> {
                countDownLabel.setVisible(false);
                countDownActive = false;
                cancelTimerBtn.setVisible(false); // Ensure cancel button is hidden
                this.capturePhoto();
                captureBtn.setDisable(false);
            }
        );

        countDownTimeline.getKeyFrames().addAll(smileFrame, captureFrame);
    }

    // Methods to set timer duration
    @FXML
    private void setTimer2Seconds() {
        timerDuration = 2;
        updateCountdownTimeline();
        updateTimerButtonStyles(timer2Button);
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
            // Try to open the camera
            capture = new VideoCapture();
            capture.open(cameraIndex);

            if (capture.isOpened()) {
                cameraActive = true;

                // Start the camera capture thread
                cameraTimer = Executors.newSingleThreadScheduledExecutor();
                cameraTimer.scheduleAtFixedRate(this::updateFrame, 0, 100, TimeUnit.MILLISECONDS);
                System.out.println("Camera started successfully");
            } else {
                System.err.println("Failed to open camera with index: " + cameraIndex);
                // Try default camera as fallback
                capture.open(0);
                if (capture.isOpened()) {
                    cameraActive = true;
                    cameraTimer = Executors.newSingleThreadScheduledExecutor();
                    cameraTimer.scheduleAtFixedRate(this::updateFrame, 0, 33, TimeUnit.MILLISECONDS);
                    System.out.println("Default camera started as fallback");
                } else {
                    System.err.println("Could not start any camera");
                }
            }
        }
    }

    private void loadFaceDetectionModel() {
        try {
            // Try to find the cascade file in several locations
            File cascadeFile = new File("haarcascade_frontalface_alt.xml");
            if (!cascadeFile.exists()) {
                cascadeFile = new File("src/main/resources/haarcascade_frontalface_alt.xml");
            }

            if (cascadeFile.exists()) {
                faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
                if (faceDetector.empty()) {
                    System.err.println("Failed to load face detection model");
                    faceDetector = null;
                } else {
                    System.out.println("Face detection model loaded successfully");
                }
            } else {
                System.err.println("Cascade file not found");
                faceDetector = null;
            }
        } catch (Exception e) {
            System.err.println("Error loading face detection model: " + e.getMessage());
            e.printStackTrace();
            faceDetector = null;
        }
    }

    private void updateFrame() {
        Mat frame = new Mat();

        // Try to read a new frame
        boolean frameRead = capture.read(frame);

        if (frameRead && !frame.empty()) {
            // Convert the frame from BGR to RGB
            Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB);

            // Detect faces if the face detector is available
            if (faceDetector != null && !faceDetector.empty() && (frameCounter % FACE_DETECTION_INTERVAL == 0)) {
                detectFaces(frame);
            }
            this.frameCounter++;

            // Convert the OpenCV Mat to a JavaFX WritableImage
            WritableImage writableImage = convertToFxImage(frame);

            // Store the raw camera frame without borders
            rawCameraFrame = writableImage;

            // Update the ImageView on the JavaFX Application Thread
            javafx.application.Platform.runLater(() -> {
                // Always start with the raw frame
                cameraView.setImage(writableImage);

                // Apply border for preview if needed
                if (currentBorder != null && borders.containsKey(currentBorder)) {
                    StackPane previewPane = new StackPane();
                    previewPane.getChildren().addAll(
                            new ImageView(writableImage),
                            new ImageView(borders.get(currentBorder))
                    );

                    WritableImage previewImage = new WritableImage(
                            (int) writableImage.getWidth(), (int) writableImage.getHeight());

                    previewPane.snapshot(null, previewImage);
                    cameraView.setImage(previewImage);
                }
            });
        }
    }

    private void detectFaces(Mat frame) {
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_RGB2GRAY);
        Imgproc.equalizeHist(grayFrame, grayFrame);

        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(
                grayFrame,
                faceDetections,
                1.1,
                2,
                0 | org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE,
                new Size(30, 30)
        );

        Rect[] facesArray = faceDetections.toArray();
        for (Rect face : facesArray) {
//            Imgproc.rectangle(
//                    frame,
//                    new Point(face.x, face.y),
//                    new Point(face.x + face.width, face.y + face.height),
//                    new Scalar(0, 255, 0),
//                    3
//            );
        }
    }


    // All the button event methods
    @FXML
    private void onCapture() {
        if (cameraView.getImage() != null && !countDownActive) {
            // Start the countdown
            countDownActive = true;
            captureBtn.setDisable(true);
            countDownLabel.setText(String.valueOf(timerDuration));
            countDownLabel.setVisible(true);

            // Show cancel button and ensure it's in front
            cancelTimerBtn.setVisible(true);
            cancelTimerBtn.toFront();

            countDownTimeline.playFromStart();
            System.out.println("Countdown started");
        }
    }

    @FXML
    private void onCancelTimer() {
        if (countDownActive) {
            // Stop the countdown
            countDownTimeline.stop();
            countDownActive = false;
            countDownLabel.setVisible(false);

            // Hide cancel button
            cancelTimerBtn.setVisible(false);

            captureBtn.setDisable(false);
            System.out.println("Countdown cancelled");
        }
    }

    // Split the original onCapture method into two parts:
    // 1. The onCapture method that starts the countdown
    // 2. The capturePhoto method that will be called when the countdown finishes
    private void capturePhoto() {
        if (rawCameraFrame != null) {
            // Hide cancel button
            cancelTimerBtn.setVisible(false);

            // Always use the raw camera frame without borders as our base
            Image snapshot = rawCameraFrame;
            Image originalLogo = new Image(RestaurantApplication.class.getResourceAsStream("restaurant_logo.png")); // Load the logo image

            // Create a canvas to draw both images
            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(
                    snapshot.getWidth(), snapshot.getHeight());
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

            // Draw the original image
            gc.drawImage(snapshot, 0, 0);

            // Apply border if one is selected
            if (currentBorder != null && borders.containsKey(currentBorder)) {
                Image border = borders.get(currentBorder);
                gc.drawImage(border, 0, 0, snapshot.getWidth(), snapshot.getHeight());
            }

            // Scale the logo to a smaller size (adjust these values as needed)
            double logoWidth = snapshot.getWidth() * 0.15; // 15% of the image width
            double logoHeight = originalLogo.getHeight() * (logoWidth / originalLogo.getWidth()); // Keep aspect ratio
            // Calculate position for the resized logo in bottom right
            double logoX = snapshot.getWidth() - logoWidth - 10; // 10px padding
            double logoY = snapshot.getHeight() - logoHeight - 10; // 10px padding

            // Draw the resized logo at the bottom right
            gc.drawImage(originalLogo, logoX, logoY, logoWidth, logoHeight);

            // Capture the final image
            WritableImage finalImage = new WritableImage(
                    (int) snapshot.getWidth(), (int) snapshot.getHeight());
            canvas.snapshot(null, finalImage);

            // Resize the finalImage to fit within the processedImageView area
            int originalWidth = (int) finalImage.getWidth();
            int originalHeight = (int) finalImage.getHeight();

            double maxWidth = 350; // Example maximum width for processedImageView (adjust as needed)
            double maxHeight = 350; // Example maximum height for processedImageView (adjust as needed)

            double widthScale = maxWidth / originalWidth;
            double heightScale = maxHeight / originalHeight;

            double scale = Math.min(widthScale, heightScale);

            int scaledWidth = (int) (originalWidth * scale);
            int scaledHeight = (int) (originalHeight * scale);

            WritableImage scaledImage = new WritableImage(scaledWidth, scaledHeight);
            PixelWriter writer = scaledImage.getPixelWriter();
            PixelReader reader = finalImage.getPixelReader();

            for (int y = 0; y < scaledHeight; y++) {
                for (int x = 0; x < scaledWidth; x++) {
                    writer.setColor(x, y, reader.getColor((int) (x / scale), (int) (y / scale)));
                }
            }

            processedImageView.setImage(scaledImage);
            saveBtn.setDisable(false);
            qrCodeBtn.setDisable(false);
            emailBtn.setDisable(false); // Enable email button after capturing photo
            System.out.println("Image captured and resized with logo");
        }
    }

    /**
     * Shows a user agreement dialog and returns true if the user accepts
     */
    private boolean showUserAgreementDialog() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("User Agreement");
        alert.setHeaderText("Please read and accept the user agreement");
        alert.setContentText("By saving this image, you agree that:\n\n" +
                "1. You have the right to take and save this photo\n" +
                "2. You grant Restaurant le Méditerranée permission to store this image\n" +
                "3. You understand that this image may be displayed in the restaurant's gallery\n" +
                "4. You consent to the restaurant using this image for promotional purposes\n\n" +
                "Do you agree to these terms?");

        Stage currentStage = (Stage) processedImageView.getScene().getWindow();
        alert.initOwner(currentStage);
        alert.initModality(Modality.APPLICATION_MODAL);

        // Use ButtonType.OK and ButtonType.CANCEL for the buttons
        return alert.showAndWait().filter(response -> response == javafx.scene.control.ButtonType.OK).isPresent();
    }

    @FXML
    private void onSave() {
        // Show user agreement dialog first
        if (!showUserAgreementDialog()) {
            // User declined the agreement
            System.out.println("User declined the agreement, image not saved");
            return;
        }

        Double imageDirectorySize = FileService.getImageDirectorySize();

        if (imageDirectorySize >= Constants.MAX_IMAGE_FOLDER_SIZE) {
            FileService.clearOldFiles(1.0); // Clear 1 GB of old files
        }

        if (processedImageView.getImage() != null) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "photo_" + timestamp + ".png";

            // Create the directory if it doesn't exist
            File imageFolder = new File(Constants.IMAGE_FOLDER);
            if (!imageFolder.exists()) {
                boolean dirCreated = imageFolder.mkdirs();
                if (!dirCreated) {
                    System.err.println("Failed to create directory: " + Constants.IMAGE_FOLDER);

                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Save Error");
                    alert.setHeaderText("Failed to create directory");
                    alert.setContentText("Could not create directory: " + Constants.IMAGE_FOLDER);
                    alert.showAndWait();
                    return;
                }
                System.out.println("Created directory: " + Constants.IMAGE_FOLDER);
            }

            lastSavedImageFile = new File(Constants.IMAGE_FOLDER, imageFileName);

            try {
                BufferedImage bufferedImage = convertFromFxImage((Image) processedImageView.getImage());
                ImageIO.write(bufferedImage, "png", lastSavedImageFile);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Saved Image");
                alert.setHeaderText("Image saved successfully");
                alert.setContentText("Image saved successfully");

                Stage currentStage = (Stage) processedImageView.getScene().getWindow();
                alert.initOwner(currentStage);
                alert.initModality(Modality.APPLICATION_MODAL);

                alert.showAndWait();

                System.out.println("Image saved: " + lastSavedImageFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error saving image: " + e.getMessage());
                e.printStackTrace();

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Save Error");
                alert.setHeaderText("Failed to save image");
                alert.setContentText("Error: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    @FXML
    private void onEmailPhoto() {
        Stage stage = (Stage) cameraView.getScene().getWindow();

        // If we have a saved file, use it
        if (lastSavedImageFile != null && lastSavedImageFile.exists()) {
            EmailSender.showEmailDialog(stage, lastSavedImageFile);
            return;
        }

        // If no saved file but we have a processed image, create a temporary file
        if (processedImageView.getImage() != null) {
            try {
                // Create a temporary file for the email attachment
                File tempDir = new File(System.getProperty("java.io.tmpdir"));
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                File tempFile = new File(tempDir, "photo_" + timestamp + ".png");

                // Convert the processed image to a BufferedImage and save it to the temp file
                BufferedImage bufferedImage = convertFromFxImage(processedImageView.getImage());
                ImageIO.write(bufferedImage, "png", tempFile);

                // Delete the temp file when the JVM exits
                tempFile.deleteOnExit();

                // Show the email dialog with the temp file
                EmailSender.showEmailDialog(stage, tempFile);

                System.out.println("Using temporary file for email: " + tempFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error creating temporary file for email: " + e.getMessage());
                e.printStackTrace();

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Email Error");
                alert.setHeaderText("Failed to prepare image for email");
                alert.setContentText("Error: " + e.getMessage());
                alert.showAndWait();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No Image Available");
            alert.setContentText("There is no image to email. Please take a photo first.");
            alert.showAndWait();
        }
    }

    @FXML
    private void onSwitchCamera() {
        if (cameraActive) {
            // Stop current camera
            if (cameraTimer != null && !cameraTimer.isShutdown()) {
                cameraTimer.shutdown();
                try {
                    cameraTimer.awaitTermination(33, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    System.err.println("Error shutting down camera timer: " + e.getMessage());
                }
            }

            if (capture != null) {
                capture.release();
            }

            cameraActive = false;
        }

        // Switch to next camera
        cameraIndex = (cameraIndex + 1) % 2;
        System.out.println("Switching to camera index: " + cameraIndex);

        // Start new camera
        startCamera();
    }

    @FXML
    private void onChristmas1BorderSelected() {
        if (this.currentBorder != null && this.currentBorder.equals("christmas1")) return;
        this.currentBorder = "christmas1";
    }

    @FXML
    private void onChristmas2BorderSelected() {
        if (this.currentBorder != null && this.currentBorder.equals("christmas2")) return;
        this.currentBorder = "christmas2";
    }

    @FXML
    private void onChristmas3BorderSelected() {
        if (this.currentBorder != null && this.currentBorder.equals("christmas3")) return;
        this.currentBorder = "christmas3";
    }

    @FXML
    private void onValentinesBorderSelected() {
        if (this.currentBorder != null && this.currentBorder.equals("valentines")) return;
        this.currentBorder = "valentines";
    }

    @FXML
    private void onClearBorderSelected() {
        this.currentBorder = null;
    }

    @FXML
    private void onGenerateQRCode() {
        qrCodeExecutor = Executors.newSingleThreadScheduledExecutor();
        Stage stage = (Stage) cameraView.getScene().getWindow();
        QRCodeDialog qrCodeDialog = new QRCodeDialog(stage);
        qrCodeDialog.showLoading();
        qrCodeDialog.show();

        // Get the image bytes
        Image image = processedImageView.getImage();
        BufferedImage bufferedImage = convertFromFxImage(image);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try {
            ImageIO.write(bufferedImage, "png", byteArrayOutputStream);
        } catch (IOException ex) {
            qrCodeDialog.showError("Failed to generate QR code: " + ex.getMessage());
            return;
        }

        byte[] imageBytes = byteArrayOutputStream.toByteArray();

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

    // Methods for converting between OpenCV Mat and JavaFX Images
    private WritableImage convertToFxImage(Mat mat) {
        // Get matrix dimensions
        int width = mat.cols();
        int height = mat.rows();

        // Create buffered image
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        // Get the data array from the matrix
        byte[] data = new byte[width * height * (int) mat.elemSize()];
        mat.get(0, 0, data);

        // Fill the buffered image
        if (mat.channels() == 3) {
            // For BGR/RGB images
            int[] intData = new int[width * height];
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int index = i * width + j;
                    int bufferIndex = index * 3;
                    int r = data[bufferIndex + 0] & 0xFF; // R
                    int g = data[bufferIndex + 1] & 0xFF; // G
                    int b = data[bufferIndex + 2] & 0xFF; // B
                    intData[index] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
            bufferedImage.setRGB(0, 0, width, height, intData, 0, width);
        }

        // Convert to JavaFX image
        WritableImage writableImage = new WritableImage(width, height);
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        // Copy pixel data
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
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
                System.err.println("Error shutting down camera timer: " + e.getMessage());
            }
        }

        if (capture != null && capture.isOpened()) {
            capture.release();
        }

        if (this.qrCodeExecutor != null && !this.qrCodeExecutor.isShutdown()) {
            qrCodeExecutor.shutdown();
            // Allow tasks to complete within a reasonable timeout
            try {
                qrCodeExecutor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.err.println("Error shutting down QR code executor: " + e.getMessage());
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
        System.out.println("Camera resources released");
        System.out.println("Email resources released");
    }
}
