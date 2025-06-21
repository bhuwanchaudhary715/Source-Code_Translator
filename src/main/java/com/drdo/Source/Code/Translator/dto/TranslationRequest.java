package com.drdo.Source.Code.Translator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Translation Request DTO
public class TranslationRequest {
    @NotBlank(message = "Source code cannot be empty")
    private String sourceCode;

    @Pattern(regexp = "java|c", message = "Source language must be 'java' or 'c'")
    private String sourceLanguage;

    @Pattern(regexp = "java|c", message = "Target language must be 'java' or 'c'")
    private String targetLanguage;

    private boolean validateSyntax = true;

    // Constructors
    public TranslationRequest() {}

    public TranslationRequest(String sourceCode, String sourceLanguage, String targetLanguage) {
        this.sourceCode = sourceCode;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
    }

    // Getters and Setters
    public String getSourceCode() { return sourceCode; }
    public void setSourceCode(String sourceCode) { this.sourceCode = sourceCode; }

    public String getSourceLanguage() { return sourceLanguage; }
    public void setSourceLanguage(String sourceLanguage) { this.sourceLanguage = sourceLanguage; }

    public String getTargetLanguage() { return targetLanguage; }
    public void setTargetLanguage(String targetLanguage) { this.targetLanguage = targetLanguage; }

    public boolean isValidateSyntax() { return validateSyntax; }
    public void setValidateSyntax(boolean validateSyntax) { this.validateSyntax = validateSyntax; }
}