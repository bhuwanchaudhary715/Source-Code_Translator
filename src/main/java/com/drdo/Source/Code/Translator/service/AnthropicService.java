package com.drdo.Source.Code.Translator.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnthropicService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.api.url}")
    private String apiUrl;

    @Value("${anthropic.api.model}")
    private String model;

    @Value("${anthropic.api.max-tokens}")
    private int maxTokens;

    @Value("${anthropic.api.version}")
    private String apiVersion;

    @Value("${app.mock-mode:false}")
    private boolean mockMode;

    public AnthropicService(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    public String translateCode(String sourceCode, String sourceLanguage, String targetLanguage) {
        if (mockMode || isApiKeyInvalid()) {
            return getMockTranslation(sourceCode, sourceLanguage, targetLanguage);
        }

        try {
            String prompt = buildTranslationPrompt(sourceCode, sourceLanguage, targetLanguage);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));

            String response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", apiVersion)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                            .filter(throwable -> throwable instanceof WebClientResponseException &&
                                    ((WebClientResponseException) throwable).getStatusCode() == HttpStatus.TOO_MANY_REQUESTS))
                    .timeout(Duration.ofSeconds(30))
                    .block();

            return extractTranslatedCode(response);

        } catch (WebClientResponseException e) {
            throw handleAnthropicError(e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to translate code using Anthropic Claude: " + e.getMessage(), e);
        }
    }

    private boolean isApiKeyInvalid() {
        return apiKey == null || apiKey.trim().isEmpty() ||
                "your_anthropic_api_key_here".equals(apiKey) ||
                "your-anthropic-api-key-here".equals(apiKey);
    }

    private RuntimeException handleAnthropicError(WebClientResponseException e) {
        String errorMessage = "Anthropic API Error";
        String solutions = "";

        try {
            String responseBody = e.getResponseBodyAsString();
            JsonNode errorNode = objectMapper.readTree(responseBody);
            JsonNode error = errorNode.get("error");

            if (error != null) {
                String type = error.has("type") ? error.get("type").asText() : "unknown";
                String message = error.has("message") ? error.get("message").asText() : "Unknown error";

                switch (e.getStatusCode().value()) {
                    case 401:
                        errorMessage = "Anthropic API authentication failed. Invalid API key.";
                        solutions = "Solutions: 1. Check your API key in .env file 2. Verify the key at https://console.anthropic.com/ 3. Make sure the key has proper permissions";
                        break;
                    case 429:
                        errorMessage = "Anthropic API rate limit exceeded (429 Too Many Requests).";
                        solutions = "Solutions: 1. Wait a few minutes and try again 2. Check your Anthropic usage limits 3. Upgrade your Anthropic plan if needed 4. Contact Anthropic support if the issue persists";
                        break;
                    case 500:
                    case 502:
                    case 503:
                        errorMessage = "Anthropic API server error. The service is temporarily unavailable.";
                        solutions = "Solutions: 1. Try again in a few minutes 2. Check Anthropic status 3. Use mock mode for testing";
                        break;
                    default:
                        errorMessage = "Anthropic API error: " + message;
                        solutions = "Solutions: 1. Check your API key and account 2. Review Anthropic documentation 3. Contact support if needed";
                }

                errorMessage += " Error Type: " + type + " Details: " + message + ". " + solutions;
            }
        } catch (Exception parseError) {
            errorMessage = "Anthropic API Error (Status: " + e.getStatusCode() + "): " + e.getMessage() + ". " +
                    "Solutions: 1. Check your Anthropic API key 2. Try again later 3. Contact Anthropic support";
        }

        return new RuntimeException(errorMessage);
    }

    private String getMockTranslation(String sourceCode, String sourceLanguage, String targetLanguage) {
        System.out.println("🤖 Using mock translation (Anthropic API not configured or in mock mode)");

        if ("java".equalsIgnoreCase(sourceLanguage) && "c".equalsIgnoreCase(targetLanguage)) {
            return generateMockJavaToC(sourceCode);
        } else if ("c".equalsIgnoreCase(sourceLanguage) && "java".equalsIgnoreCase(targetLanguage)) {
            return generateMockCToJava(sourceCode);
        } else {
            return "// Mock translation: " + sourceLanguage + " to " + targetLanguage + "\n" +
                    "// Original code:\n/*\n" + sourceCode + "\n*/\n" +
                    "// This is a mock translation for testing purposes.\n" +
                    "// Please configure your Anthropic API key for real translations.";
        }
    }

    private String generateMockJavaToC(String javaCode) {
        StringBuilder cCode = new StringBuilder();
        cCode.append("#include <stdio.h>\n");
        cCode.append("#include <stdlib.h>\n");
        cCode.append("#include <string.h>\n\n");

        if (javaCode.contains("System.out.println")) {
            cCode.append("int main() {\n");

            String[] lines = javaCode.split("\n");
            for (String line : lines) {
                if (line.contains("System.out.println")) {
                    String message = extractPrintMessage(line);
                    cCode.append("    printf(").append(message).append("\\n\");\n");
                }
            }

            cCode.append("    return 0;\n");
            cCode.append("}\n");
        } else {
            cCode.append("// Mock C translation\n");
            cCode.append("int main() {\n");
            cCode.append("    // Translated from Java:\n");
            cCode.append("    /*\n").append(javaCode).append("\n    */\n");
            cCode.append("    printf(\"Mock translation - configure Anthropic API for real translation\\n\");\n");
            cCode.append("    return 0;\n");
            cCode.append("}\n");
        }

        return cCode.toString();
    }

    private String generateMockCToJava(String cCode) {
        StringBuilder javaCode = new StringBuilder();
        javaCode.append("public class TranslatedCode {\n");
        javaCode.append("    public static void main(String[] args) {\n");

        if (cCode.contains("printf")) {
            String[] lines = cCode.split("\n");
            for (String line : lines) {
                if (line.contains("printf")) {
                    String message = extractPrintfMessage(line);
                    javaCode.append("        System.out.println(").append(message).append(");\n");
                }
            }
        } else {
            javaCode.append("        // Mock Java translation\n");
            javaCode.append("        /*\n");
            javaCode.append("         * Translated from C:\n");
            javaCode.append("         * ").append(cCode.replace("\n", "\n         * ")).append("\n");
            javaCode.append("         */\n");
            javaCode.append("        System.out.println(\"Mock translation - configure Anthropic API for real translation\");\n");
        }

        javaCode.append("    }\n");
        javaCode.append("}\n");

        return javaCode.toString();
    }

    private String extractPrintMessage(String line) {
        int start = line.indexOf("\"");
        int end = line.lastIndexOf("\"");
        if (start != -1 && end != -1 && start < end) {
            return line.substring(start, end + 1);
        }
        return "\"Hello World\"";
    }

    private String extractPrintfMessage(String line) {
        int start = line.indexOf("\"");
        int end = line.indexOf("\"", start + 1);
        if (start != -1 && end != -1) {
            String message = line.substring(start + 1, end);
            message = message.replace("\\n", "");
            return "\"" + message + "\"";
        }
        return "\"Hello World\"";
    }

    private String buildTranslationPrompt(String sourceCode, String sourceLanguage, String targetLanguage) {
        return String.format(
                "You are an expert programmer specializing in code translation between Java and C. " +
                        "Translate the following %s code to %s while maintaining the same functionality and logic. " +
                        "Focus on:\n" +
                        "1. Maintaining equivalent functionality\n" +
                        "2. Using appropriate language-specific syntax and conventions\n" +
                        "3. Handling data types and memory management correctly\n" +
                        "4. Preserving the original algorithm and logic flow\n" +
                        "5. Adding necessary includes/imports for the target language\n\n" +
                        "Return only the translated code without any explanations or markdown formatting.\n\n" +
                        "Source %s code:\n%s",
                sourceLanguage.toUpperCase(),
                targetLanguage.toUpperCase(),
                sourceLanguage.toUpperCase(),
                sourceCode
        );
    }

    private String extractTranslatedCode(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode content = jsonNode.get("content");

            if (content != null && content.isArray() && content.size() > 0) {
                JsonNode firstContent = content.get(0);
                JsonNode text = firstContent.get("text");
                if (text != null) {
                    return cleanTranslatedCode(text.asText());
                }
            }

            throw new RuntimeException("Invalid response format from Anthropic API");

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Anthropic response: " + e.getMessage(), e);
        }
    }

    private String cleanTranslatedCode(String code) {
        // Remove markdown code blocks if present
        code = code.replaceAll("```[a-zA-Z]*\\n?", "");
        code = code.replaceAll("```", "");

        // Remove leading/trailing whitespace
        code = code.trim();

        return code;
    }

    public boolean isApiAvailable() {
        return !mockMode && !isApiKeyInvalid();
    }

    public String getApiStatus() {
        if (mockMode) {
            return "Mock mode enabled";
        } else if (isApiKeyInvalid()) {
            return "Anthropic API key not configured";
        } else {
            return "Anthropic API configured";
        }
    }
}