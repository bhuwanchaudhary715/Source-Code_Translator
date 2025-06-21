package com.drdo.Source.Code.Translator.service;

import com.drdo.Source.Code.Translator.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TranslationService {

    private final AnthropicService anthropicService;
    private final OCRService ocrService;
    private final SyntaxValidationService syntaxValidationService;

    @Autowired
    public TranslationService(AnthropicService anthropicService,
                              OCRService ocrService,
                              SyntaxValidationService syntaxValidationService) {
        this.anthropicService = anthropicService;
        this.ocrService = ocrService;
        this.syntaxValidationService = syntaxValidationService;
    }

    public TranslationResponse translateCode(TranslationRequest request) {
        try {
            // Validate input
            if (request.getSourceLanguage().equals(request.getTargetLanguage())) {
                return new TranslationResponse(
                        request.getSourceCode(),
                        request.getSourceCode(),
                        request.getSourceLanguage(),
                        request.getTargetLanguage(),
                        false,
                        "Source and target languages cannot be the same"
                );
            }

            // Validate source code syntax if requested
            SyntaxValidationResult sourceValidation = null;
            if (request.isValidateSyntax()) {
                sourceValidation = validateSyntax(request.getSourceCode(), request.getSourceLanguage());
                if (!sourceValidation.isValid()) {
                    TranslationResponse response = new TranslationResponse(
                            request.getSourceCode(),
                            "",
                            request.getSourceLanguage(),
                            request.getTargetLanguage(),
                            false,
                            "Source code syntax validation failed: " + sourceValidation.getErrorMessage()
                    );
                    response.setSyntaxValidation(sourceValidation);
                    return response;
                }
            }

            // Perform translation using Anthropic Claude
            String translatedCode = anthropicService.translateCode(
                    request.getSourceCode(),
                    request.getSourceLanguage(),
                    request.getTargetLanguage()
            );

            // Validate translated code syntax if requested
            SyntaxValidationResult targetValidation = null;
            if (request.isValidateSyntax()) {
                targetValidation = validateSyntax(translatedCode, request.getTargetLanguage());
            }

            // Create response
            TranslationResponse response = new TranslationResponse(
                    request.getSourceCode(),
                    translatedCode,
                    request.getSourceLanguage(),
                    request.getTargetLanguage(),
                    true,
                    "Translation completed successfully"
            );

            // Set syntax validation results
            if (targetValidation != null) {
                response.setSyntaxValidation(targetValidation);
                if (!targetValidation.isValid()) {
                    response.setMessage("Translation completed but target code has syntax issues: " +
                            targetValidation.getErrorMessage());
                }
            }

            return response;

        } catch (Exception e) {
            return new TranslationResponse(
                    request.getSourceCode(),
                    "",
                    request.getSourceLanguage(),
                    request.getTargetLanguage(),
                    false,
                    "Translation failed: " + e.getMessage()
            );
        }
    }

    public TranslationResponse translateFromImage(MultipartFile imageFile,
                                                  String sourceLanguage,
                                                  String targetLanguage,
                                                  boolean validateSyntax) {
        try {
            // Extract text from image using OCR
            OCRResult ocrResult = ocrService.extractTextFromImage(imageFile);

            if (!ocrResult.isSuccess()) {
                return new TranslationResponse(
                        "",
                        "",
                        sourceLanguage,
                        targetLanguage,
                        false,
                        "OCR failed: " + ocrResult.getErrorMessage()
                );
            }

            String extractedCode = ocrResult.getExtractedText();
            if (extractedCode.trim().isEmpty()) {
                return new TranslationResponse(
                        "",
                        "",
                        sourceLanguage,
                        targetLanguage,
                        false,
                        "No text could be extracted from the image"
                );
            }

            // Create translation request with extracted code
            TranslationRequest request = new TranslationRequest(extractedCode, sourceLanguage, targetLanguage);
            request.setValidateSyntax(validateSyntax);

            // Perform translation
            TranslationResponse response = translateCode(request);

            // Add OCR confidence information to the response message
            if (response.isSuccess()) {
                response.setMessage(response.getMessage() +
                        " (OCR confidence: " + String.format("%.1f", ocrResult.getConfidence() * 100) + "%)");
            }

            return response;

        } catch (Exception e) {
            return new TranslationResponse(
                    "",
                    "",
                    sourceLanguage,
                    targetLanguage,
                    false,
                    "Image translation failed: " + e.getMessage()
            );
        }
    }

    private SyntaxValidationResult validateSyntax(String code, String language) {
        switch (language.toLowerCase()) {
            case "java":
                return syntaxValidationService.validateJavaCode(code);
            case "c":
                return syntaxValidationService.validateCCode(code);
            default:
                return new SyntaxValidationResult(false, "Unsupported language for validation: " + language);
        }
    }

    public boolean isValidLanguage(String language) {
        return "java".equalsIgnoreCase(language) || "c".equalsIgnoreCase(language);
    }

    public String getLanguageFileExtension(String language) {
        switch (language.toLowerCase()) {
            case "java":
                return ".java";
            case "c":
                return ".c";
            default:
                return ".txt";
        }
    }

    public boolean isAnthropicAvailable() {
        return anthropicService.isApiAvailable();
    }

    public String getAnthropicStatus() {
        return anthropicService.getApiStatus();
    }
}