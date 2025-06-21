package com.drdo.Source.Code.Translator.dto;

// Translation Response DTO
public class TranslationResponse {
    private String originalCode;
    private String translatedCode;
    private String sourceLanguage;
    private String targetLanguage;
    private boolean success;
    private String message;
    private SyntaxValidationResult syntaxValidation;

    // Constructors
    public TranslationResponse() {}

    public TranslationResponse(String originalCode, String translatedCode,
                               String sourceLanguage, String targetLanguage,
                               boolean success, String message) {
        this.originalCode = originalCode;
        this.translatedCode = translatedCode;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
    public String getOriginalCode() { return originalCode; }
    public void setOriginalCode(String originalCode) { this.originalCode = originalCode; }

    public String getTranslatedCode() { return translatedCode; }
    public void setTranslatedCode(String translatedCode) { this.translatedCode = translatedCode; }

    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }

    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public SyntaxValidationResult getSyntaxValidation() { return syntaxValidation; }
    public void setSyntaxValidation(SyntaxValidationResult syntaxValidation) {
        this.syntaxValidation = syntaxValidation;
    }
}
