package com.billcom.drools.camtest.dialog;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class QRCodeDialog extends Dialog<Void> {

    private final ImageView qrCodeImageView;
    private final Label statusLabel;

    public static final String DEFAULT_LABEL = "Scan this QR code to access the image";

    public QRCodeDialog(Stage parentStage) {
        // Set dialog title, modality and owner for the dialog to not minimise the app when shown
        setTitle("QR Code");
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        initOwner(parentStage);

        // Instantiate the ImageView, set width and height, and preserve the aspect ratio
        qrCodeImageView = new ImageView();
        qrCodeImageView.setFitHeight(300);
        qrCodeImageView.setFitWidth(300);
        qrCodeImageView.setPreserveRatio(true);

        // Instantiate the label, set the text and wrap the text
        statusLabel = new Label(DEFAULT_LABEL);
        statusLabel.setWrapText(true);

        // Create the VBox layout, set padding, alignment and add the ImageView and Label
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(qrCodeImageView, statusLabel);

        // Set the content and add a close button
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Set the minimum width and height of the dialog
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        stage.setMinWidth(350);
        stage.setMinHeight(450);
    }


    public void setQRCodeImage(Image qrCodeImage) {
        qrCodeImageView.setImage(qrCodeImage);
        statusLabel.setText(DEFAULT_LABEL);
    }

    public void showLoading() {
        qrCodeImageView.setImage(null);
        statusLabel.setText("Generating QR Code ...");
    }

    public void showError(String errorMessage) {
        qrCodeImageView.setImage(null);
        statusLabel.setText("Failed to generate QR Code: " + errorMessage);
    }
}
