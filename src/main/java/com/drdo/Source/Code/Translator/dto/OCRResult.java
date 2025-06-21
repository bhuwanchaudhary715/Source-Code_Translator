package com.drdo.Source.Code.Translator.dto;

// OCR Result DTO
public class OCRResult {
    private String extractedText;
    private boolean success;
    private String errorMessage;
    private double confidence;

    public OCRResult() {}

    public OCRResult(String extractedText, boolean success) {
        this.extractedText = extractedText;
        this.success = success;
    }

    // Getters and Setters
    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}