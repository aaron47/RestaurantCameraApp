package com.billcom.drools.camtest.drive;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class GoogleDriveService {

    private GoogleDriveService() {
    }

    private static final String APPLICATION_NAME = "JavaFX Google Drive Upload Demo";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String GOOGLE_DRIVE_FOLDER_ID = "1IkfgX7eQkrDWEbNFn6NSwPcM1wWIzDCY";
    private static final String CREDENTIALS_FILE_PATH = "service-account-key.json";

    /**
     * Upload an image to Google Drive and return the shareable link
     *
     * @param imageBytes The image data as byte array
     * @param imageName  The name to use for the uploaded file
     * @return The shareable link to the uploaded file
     */
    public static String uploadImageToDrive(byte[] imageBytes, String imageName) throws IOException, GeneralSecurityException {
        // Create a temporary file from the bytes
        Path tempFile = Files.createTempFile("upload-", "png");
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            fos.write(imageBytes);
        }

        // Set up the drive service
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // Load service account credentials from the JSON key file
        GoogleCredential credential = GoogleCredential.fromStream(
                Files.newInputStream(Paths.get(CREDENTIALS_FILE_PATH))
        ).createScoped(Collections.singleton(DriveScopes.DRIVE_FILE));

        // Build the drive service
        Drive service = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();

        // Create file metadata
        File file = new File();
        file.setName(imageName);
        file.setParents(Collections.singletonList(GOOGLE_DRIVE_FOLDER_ID));

        // Create file content and upload it to google drive
        FileContent mediaContent = new FileContent("image/png", tempFile.toFile());
        File uploadedFile = service.files().create(file, mediaContent).setFields("id").execute();

        // Set the permissions for the viewer
        Permission permission = new Permission().setType("anyone").setRole("reader");
        service.permissions().create(uploadedFile.getId(), permission).execute();

        // Generate the shared link and return it
        File fileWithLink = service.files().get(uploadedFile.getId()).setFields("webViewLink").execute();
        String shareableLink = fileWithLink.getWebViewLink();
        System.out.println("File shareable link: " + shareableLink);

        // Clean up the temp file
        Files.deleteIfExists(tempFile);

        return shareableLink;
    }

    /**
     * Generate a QR code image for the given URL
     *
     * @param url    The URL to encode in the QR code
     * @param width  The width of the QR code image
     * @param height The height of the QR code image
     * @return The QR code image
     */
    public static Image generateQRCode(String url, int width, int height) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height);

        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", byteArrayOutputStream);

        // convert the image to a JavaFX Image object
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);

        return new Image(byteArrayInputStream);
    }
}
