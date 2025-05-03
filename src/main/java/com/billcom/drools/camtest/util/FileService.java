package com.billcom.drools.camtest.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class FileService {

    private FileService() {
    }

    /**
     * Gets or creates the photo directory.
     *
     * @return the photo directory
     */
    public static File getOrCreatePhotoDirectory() {
        File photoDir = new File(Constants.IMAGE_FOLDER);
        if (!photoDir.exists()) {
            photoDir.mkdirs();
        }
        return photoDir;
    }

    /**
     * Get the size of the image directory in gigabytes.
     *
     * @return the size of the directory in gigabytes
     */
    public static Double getImageDirectorySize() {
        double sizeInGb = 0L;

        File imageFolder = new File(Constants.IMAGE_FOLDER);
        if (!imageFolder.exists()) {
            return sizeInGb; // Return 0 if directory doesn't exist
        }

        try (Stream<Path> paths = Files.walk(Paths.get(Constants.IMAGE_FOLDER))) {
            long sizeInBytes = paths
                    .filter(Files::isRegularFile)
                    .mapToLong(path -> path.toFile().length())
                    .sum();
            sizeInGb = sizeInBytes / Math.pow(1024, 3);
        } catch (Exception e) {
            System.err.println("Error calculating directory size: " + e.getMessage());
            e.printStackTrace();
        }

        return sizeInGb;
    }

    public static void clearOldFiles(double spaceToClearInGB) {
        File imageFolder = new File(Constants.IMAGE_FOLDER);
        if (!imageFolder.exists()) {
            System.out.println("Image folder does not exist, nothing to clear");
            return; // Nothing to clear if directory doesn't exist
        }

        try (Stream<Path> paths = Files.walk(Paths.get(Constants.IMAGE_FOLDER))) {
            // Collect files sorted by last modified time
            List<File> files = paths
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .sorted(Comparator.comparingLong(File::lastModified))
                    .toList();

            long spaceToClearInBytes = (long) (spaceToClearInGB * Math.pow(1024, 3));
            long clearedSpace = 0;

            for (File file : files) {
                long fileSize = file.length();
                if (file.delete()) {
                    clearedSpace += fileSize;
                    System.out.println("Deleted old file: " + file.getName() + " (" + (fileSize / 1024) + " KB)");
                    if (clearedSpace >= spaceToClearInBytes) {
                        break;
                    }
                } else {
                    System.err.println("Failed to delete file: " + file.getAbsolutePath());
                }
            }

            System.out.println("Cleared " + (clearedSpace / Math.pow(1024, 3)) + " GB of space");
        } catch (Exception e) {
            System.err.println("Error clearing old files: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
