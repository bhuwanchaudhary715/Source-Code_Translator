package com.drdo.Source.Code.Translator.dto;

// Syntax Validation Result DTO
public class SyntaxValidationResult {
    private boolean valid;
    private String errorMessage;
    private int errorLine;
    private int errorColumn;

    public SyntaxValidationResult() {}

    public SyntaxValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public int getErrorLine() { return errorLine; }
    public void setErrorLine(int errorLine) { this.errorLine = errorLine; }

    public int getErrorColumn() { return errorColumn; }
    public void setErrorColumn(int errorColumn) { this.errorColumn = errorColumn; }
}
