package com.drdo.Source.Code.Translator.service;
// Updated OCRService.java with better error handling and configuration

import com.drdo.Source.Code.Translator.dto.OCRResult;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class OCRService {

    @Value("${ocr.tesseract.data-path}")
    private String tesseractDataPath;

    private Tesseract tesseract;
    private boolean tesseractInitialized = false;
    private String initializationError = null;

    public OCRService() {
        // Initialization will be done lazily in initializeTesseract()
    }

    private synchronized void initializeTesseract() {
        if (tesseractInitialized) {
            return;
        }

        try {
            tesseract = new Tesseract();

            // Try to find Tesseract data path automatically
            String dataPath = findTesseractDataPath();
            if (dataPath != null) {
                tesseract.setDatapath(dataPath);
                System.out.println("Using Tesseract data path: " + dataPath);
            } else {
                throw new RuntimeException("Could not locate Tesseract data files");
            }

            // Configure Tesseract for better code recognition
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(6); // Uniform block of text
            tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only

            // Set variables for better code recognition
            tesseract.setVariable("tessedit_char_whitelist",
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" +
                            "(){}[]<>;,.:!@#$%^&*-+=|\\/?\"' \t\n");

            // Test Tesseract with a simple operation
            tesseract.setVariable("user_defined_dpi", "300");

            tesseractInitialized = true;
            initializationError = null;
            System.out.println("Tesseract OCR initialized successfully");

        } catch (Exception e) {
            tesseractInitialized = false;
            initializationError = "Failed to initialize Tesseract: " + e.getMessage();
            System.err.println(initializationError);
            e.printStackTrace();
        }
    }

    private String findTesseractDataPath() {
        // Common Tesseract data paths for different operating systems
        String[] possiblePaths = {
                // From configuration
                tesseractDataPath,

                // Environment variable
                System.getenv("TESSDATA_PREFIX"),

                // Windows paths
                "C:\\Program Files\\Tesseract-OCR\\tessdata",
                "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata",
                "C:\\tools\\tesseract\\tessdata",

                // Linux/Ubuntu paths
                "/usr/share/tesseract-ocr/4.00/tessdata",
                "/usr/share/tesseract-ocr/5.00/tessdata",
                "/usr/share/tessdata",
                "/usr/local/share/tessdata",

                // macOS paths (Homebrew)
                "/opt/homebrew/share/tessdata",
                "/usr/local/share/tessdata",
                "/usr/local/Cellar/tesseract/*/share/tessdata",

                // Docker/container paths
                "/app/tessdata",
                "./tessdata"
        };

        for (String path : possiblePaths) {
            if (path != null && !path.isEmpty() && isValidTesseractPath(path)) {
                return path;
            }
        }

        // Try to find using system command
        return findTesseractPathUsingCommand();
    }

    private boolean isValidTesseractPath(String path) {
        try {
            Path tessDataPath = Paths.get(path);
            if (Files.exists(tessDataPath) && Files.isDirectory(tessDataPath)) {
                // Check for eng.traineddata file
                Path engFile = tessDataPath.resolve("eng.traineddata");
                boolean hasEngFile = Files.exists(engFile);

                if (hasEngFile) {
                    System.out.println("Found valid Tesseract data path: " + path);
                    return true;
                } else {
                    System.out.println("Path exists but missing eng.traineddata: " + path);
                }
            }
        } catch (Exception e) {
            System.out.println("Error checking path: " + path + " - " + e.getMessage());
        }
        return false;
    }

    private String findTesseractPathUsingCommand() {
        try {
            // Try to get tessdata location from tesseract command
            Process process = Runtime.getRuntime().exec("tesseract --print-parameters");
            process.waitFor();

            // This is a fallback approach - may not work in all environments
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public OCRResult extractTextFromImage(MultipartFile imageFile) {
        // Initialize Tesseract if not already done
        if (!tesseractInitialized) {
            initializeTesseract();
        }

        // Check if Tesseract is properly initialized
        if (!tesseractInitialized || tesseract == null) {
            OCRResult result = new OCRResult("", false);
            result.setErrorMessage("Tesseract OCR is not properly configured: " +
                    (initializationError != null ? initializationError : "Unknown error"));
            return result;
        }

        try {
            // Validate file
            if (imageFile.isEmpty()) {
                return new OCRResult("", false);
            }

            // Check if file is an image
            String contentType = imageFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                OCRResult result = new OCRResult("", false);
                result.setErrorMessage("File must be an image");
                return result;
            }

            // Read image from multipart file
            BufferedImage image;
            try (InputStream inputStream = imageFile.getInputStream()) {
                image = ImageIO.read(inputStream);
            }

            if (image == null) {
                OCRResult result = new OCRResult("", false);
                result.setErrorMessage("Could not read image file");
                return result;
            }

            // Perform OCR with error handling
            String extractedText;
            try {
                extractedText = tesseract.doOCR(image);
            } catch (TesseractException e) {
                OCRResult result = new OCRResult("", false);
                result.setErrorMessage("OCR processing failed: " + e.getMessage() +
                        ". Please check Tesseract installation and configuration.");
                return result;
            }

            // Clean up the extracted text
            extractedText = cleanExtractedText(extractedText);

            OCRResult result = new OCRResult(extractedText, true);
            result.setConfidence(calculateConfidence(extractedText));

            return result;

        } catch (IOException e) {
            OCRResult result = new OCRResult("", false);
            result.setErrorMessage("Failed to read image: " + e.getMessage());
            return result;

        } catch (Exception e) {
            OCRResult result = new OCRResult("", false);
            result.setErrorMessage("Unexpected error during OCR: " + e.getMessage());
            return result;
        }
    }

    private String cleanExtractedText(String text) {
        if (text == null) {
            return "";
        }

        // Remove excessive whitespace and normalize line endings
        text = text.replaceAll("\\r\\n", "\n");
        text = text.replaceAll("\\r", "\n");

        // Remove multiple consecutive spaces but preserve indentation
        String[] lines = text.split("\n");
        StringBuilder cleanedText = new StringBuilder();

        for (String line : lines) {
            // Preserve leading whitespace for indentation
            String trimmedLine = line.replaceAll("[ \\t]+", " ").trim();
            if (!trimmedLine.isEmpty()) {
                // Count leading spaces in original line
                int leadingSpaces = 0;
                for (char c : line.toCharArray()) {
                    if (c == ' ' || c == '\t') {
                        leadingSpaces++;
                    } else {
                        break;
                    }
                }

                // Add back some indentation (simplified)
                StringBuilder indentation = new StringBuilder();
                for (int i = 0; i < Math.min(leadingSpaces / 2, 8); i++) {
                    indentation.append("  ");
                }

                cleanedText.append(indentation).append(trimmedLine).append("\n");
            }
        }

        return cleanedText.toString().trim();
    }

    private double calculateConfidence(String extractedText) {
        // Simple heuristic for confidence based on text characteristics
        if (extractedText == null || extractedText.trim().isEmpty()) {
            return 0.0;
        }

        double confidence = 0.5; // Base confidence

        // Check for code-like patterns
        if (extractedText.contains("{") && extractedText.contains("}")) {
            confidence += 0.2;
        }
        if (extractedText.contains("(") && extractedText.contains(")")) {
            confidence += 0.1;
        }
        if (extractedText.contains(";")) {
            confidence += 0.1;
        }
        if (extractedText.matches(".*\\b(public|private|protected|void|int|class|if|for|while)\\b.*")) {
            confidence += 0.1;
        }

        return Math.min(confidence, 1.0);
    }

    public boolean isImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    // Health check method
    public boolean isTesseractAvailable() {
        if (!tesseractInitialized) {
            initializeTesseract();
        }
        return tesseractInitialized && tesseract != null;
    }

    public String getTesseractStatus() {
        if (!tesseractInitialized) {
            initializeTesseract();
        }

        if (tesseractInitialized) {
            return "Tesseract OCR is properly configured and available";
        } else {
            return "Tesseract OCR is not available: " +
                    (initializationError != null ? initializationError : "Unknown error");
        }
    }
}