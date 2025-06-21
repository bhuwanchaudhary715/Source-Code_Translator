package com.drdo.Source.Code.Translator.controller;

import com.drdo.Source.Code.Translator.dto.TranslationRequest;
import com.drdo.Source.Code.Translator.dto.TranslationResponse;
import com.drdo.Source.Code.Translator.service.TranslationService;
import com.drdo.Source.Code.Translator.service.OCRService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/translate")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TranslationController {

    private final TranslationService translationService;
    private final OCRService ocrService;

    @Autowired
    public TranslationController(TranslationService translationService, OCRService ocrService) {
        this.translationService = translationService;
        this.ocrService = ocrService;
    }

    @PostMapping("/text")
    public ResponseEntity<TranslationResponse> translateText(@Valid @RequestBody TranslationRequest request) {
        try {
            // Validate languages
            if (!translationService.isValidLanguage(request.getSourceLanguage()) ||
                    !translationService.isValidLanguage(request.getTargetLanguage())) {

                TranslationResponse errorResponse = new TranslationResponse(
                        request.getSourceCode(),
                        "",
                        request.getSourceLanguage(),
                        request.getTargetLanguage(),
                        false,
                        "Invalid language. Supported languages: java, c"
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }

            TranslationResponse response = translationService.translateCode(request);

            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            TranslationResponse errorResponse = new TranslationResponse(
                    request.getSourceCode(),
                    "",
                    request.getSourceLanguage(),
                    request.getTargetLanguage(),
                    false,
                    "Internal server error: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TranslationResponse> translateImage(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam("sourceLanguage") String sourceLanguage,
            @RequestParam("targetLanguage") String targetLanguage,
            @RequestParam(value = "validateSyntax", defaultValue = "true") boolean validateSyntax) {

        try {
            // Check if OCR service is available first
            if (!ocrService.isTesseractAvailable()) {
                TranslationResponse errorResponse = new TranslationResponse(
                        "",
                        "",
                        sourceLanguage,
                        targetLanguage,
                        false,
                        "OCR service is not available. " + ocrService.getTesseractStatus()
                );
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
            }

            // Validate file
            if (imageFile.isEmpty()) {
                TranslationResponse errorResponse = new TranslationResponse(
                        "",
                        "",
                        sourceLanguage,
                        targetLanguage,
                        false,
                        "No image file provided"
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Validate languages
            if (!translationService.isValidLanguage(sourceLanguage) ||
                    !translationService.isValidLanguage(targetLanguage)) {

                TranslationResponse errorResponse = new TranslationResponse(
                        "",
                        "",
                        sourceLanguage,
                        targetLanguage,
                        false,
                        "Invalid language. Supported languages: java, c"
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // Check file type
            String contentType = imageFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                TranslationResponse errorResponse = new TranslationResponse(
                        "",
                        "",
                        sourceLanguage,
                        targetLanguage,
                        false,
                        "File must be an image"
                );
                return ResponseEntity.badRequest().body(errorResponse);
            }

            TranslationResponse response = translationService.translateFromImage(
                    imageFile, sourceLanguage, targetLanguage, validateSyntax);

            HttpStatus status = response.isSuccess() ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            TranslationResponse errorResponse = new TranslationResponse(
                    "",
                    "",
                    sourceLanguage,
                    targetLanguage,
                    false,
                    "Internal server error: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "Code Translation API");
        status.put("timestamp", java.time.Instant.now().toString());

        // Add service status
        Map<String, Object> services = new HashMap<>();

        // Anthropic service status
        services.put("anthropic", translationService.getAnthropicStatus());

        // OCR service status
        if (ocrService.isTesseractAvailable()) {
            services.put("ocr", "Available");
        } else {
            services.put("ocr", "Unavailable - " + ocrService.getTesseractStatus());
        }

        status.put("services", services);

        // Overall health - consider degraded if OCR is not available, but still functional
        boolean ocrAvailable = ocrService.isTesseractAvailable();
        status.put("status", ocrAvailable ? "UP" : "DEGRADED");
        status.put("message", ocrAvailable ?
                "All services operational" :
                "Text translation available, OCR unavailable");

        // Always return OK since text translation can work without OCR
        return ResponseEntity.ok(status);
    }

    @GetMapping("/ocr/status")
    public ResponseEntity<Map<String, Object>> ocrStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("available", ocrService.isTesseractAvailable());
        status.put("status", ocrService.getTesseractStatus());
        status.put("timestamp", java.time.Instant.now().toString());

        HttpStatus responseStatus = ocrService.isTesseractAvailable() ?
                HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(responseStatus).body(status);
    }

    @GetMapping("/languages")
    public ResponseEntity<Map<String, Object>> getSupportedLanguages() {
        Map<String, Object> languages = new HashMap<>();
        languages.put("supported", new String[]{"java", "c"});
        languages.put("translations", new String[]{"java-to-c", "c-to-java"});

        // Add service capabilities
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("text_translation", translationService.isAnthropicAvailable());
        capabilities.put("image_translation", ocrService.isTesseractAvailable());
        capabilities.put("syntax_validation", true);
        capabilities.put("mock_mode", !translationService.isAnthropicAvailable());

        languages.put("capabilities", capabilities);

        return ResponseEntity.ok(languages);
    }

    // Error handling
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal server error");
        error.put("message", e.getMessage());
        error.put("timestamp", java.time.Instant.now().toString());
        error.put("suggestion", "Check server logs for more details. If the issue persists, verify your Tesseract OCR installation.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}