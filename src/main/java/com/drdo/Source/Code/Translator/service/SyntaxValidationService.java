package com.drdo.Source.Code.Translator.service;

import com.drdo.Source.Code.Translator.dto.SyntaxValidationResult;
import org.springframework.stereotype.Service;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SyntaxValidationService {

    public SyntaxValidationResult validateJavaCode(String javaCode) {
        try {
            // Get Java compiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                return new SyntaxValidationResult(false,
                        "Java compiler not available. Make sure you're running with JDK, not JRE.");
            }

            // Extract class name from the code
            String className = extractPublicClassName(javaCode);
            if (className == null) {
                // If no public class found, use a default name and make code non-public
                className = "TempClass";
                javaCode = makeClassNonPublic(javaCode);
            }

            // Create diagnostic collector
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

            // Get standard file manager
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                    diagnostics, null, null);

            // Create in-memory Java file object with correct class name
            JavaFileObject javaFile = new InMemoryJavaFileObject(className, javaCode);
            List<JavaFileObject> compilationUnits = Collections.singletonList(javaFile);

            // Compile
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, fileManager, diagnostics, null, null, compilationUnits);

            boolean success = task.call();

            if (success) {
                return new SyntaxValidationResult(true, "Java syntax is valid");
            } else {
                // Get first error
                List<Diagnostic<? extends JavaFileObject>> diagnosticList = diagnostics.getDiagnostics();
                if (!diagnosticList.isEmpty()) {
                    Diagnostic<? extends JavaFileObject> firstError = diagnosticList.get(0);

                    SyntaxValidationResult result = new SyntaxValidationResult(false,
                            firstError.getMessage(null));
                    result.setErrorLine((int) firstError.getLineNumber());
                    result.setErrorColumn((int) firstError.getColumnNumber());
                    return result;
                }
                return new SyntaxValidationResult(false, "Java syntax validation failed");
            }

        } catch (Exception e) {
            return new SyntaxValidationResult(false,
                    "Java syntax validation error: " + e.getMessage());
        }
    }

    public SyntaxValidationResult validateCCode(String cCode) {
        try {
            // Create temporary file
            Path tempDir = Files.createTempDirectory("c_syntax_check");
            Path tempFile = tempDir.resolve("temp.c");

            try {
                // Write C code to temporary file
                Files.write(tempFile, cCode.getBytes());

                // Try to compile with gcc
                ProcessBuilder pb = new ProcessBuilder("gcc", "-fsyntax-only",
                        tempFile.toString());
                pb.redirectErrorStream(true);

                Process process = pb.start();

                // Read output
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                }

                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    return new SyntaxValidationResult(true, "C syntax is valid");
                } else {
                    String errorOutput = output.toString();
                    SyntaxValidationResult result = parseCCompilerError(errorOutput);
                    return result;
                }

            } finally {
                // Clean up temporary files
                try {
                    Files.deleteIfExists(tempFile);
                    Files.deleteIfExists(tempDir);
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            }

        } catch (IOException e) {
            // GCC not available, try basic syntax checks
            return performBasicCSyntaxCheck(cCode);
        } catch (Exception e) {
            return new SyntaxValidationResult(false,
                    "C syntax validation error: " + e.getMessage());
        }
    }

    /**
     * Extract the public class name from Java code
     */
    private String extractPublicClassName(String javaCode) {
        // Pattern to match: public class ClassName
        Pattern pattern = Pattern.compile("public\\s+class\\s+(\\w+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(javaCode);

        if (matcher.find()) {
            return matcher.group(1);
        }

        // Also check for public interface, enum, etc.
        Pattern interfacePattern = Pattern.compile("public\\s+interface\\s+(\\w+)", Pattern.MULTILINE);
        Matcher interfaceMatcher = interfacePattern.matcher(javaCode);
        if (interfaceMatcher.find()) {
            return interfaceMatcher.group(1);
        }

        Pattern enumPattern = Pattern.compile("public\\s+enum\\s+(\\w+)", Pattern.MULTILINE);
        Matcher enumMatcher = enumPattern.matcher(javaCode);
        if (enumMatcher.find()) {
            return enumMatcher.group(1);
        }

        return null; // No public class found
    }

    /**
     * Remove public modifier from class declarations to avoid filename issues
     */
    private String makeClassNonPublic(String javaCode) {
        // Remove public from class declarations
        javaCode = javaCode.replaceAll("public\\s+class\\s+", "class ");
        javaCode = javaCode.replaceAll("public\\s+interface\\s+", "interface ");
        javaCode = javaCode.replaceAll("public\\s+enum\\s+", "enum ");

        return javaCode;
    }

    private SyntaxValidationResult parseCCompilerError(String errorOutput) {
        // Parse GCC error output to extract line number and error message
        Pattern errorPattern = Pattern.compile("temp\\.c:(\\d+):(\\d+):\\s*error:\\s*(.+)");
        Matcher matcher = errorPattern.matcher(errorOutput);

        if (matcher.find()) {
            int lineNumber = Integer.parseInt(matcher.group(1));
            int columnNumber = Integer.parseInt(matcher.group(2));
            String errorMessage = matcher.group(3);

            SyntaxValidationResult result = new SyntaxValidationResult(false, errorMessage);
            result.setErrorLine(lineNumber);
            result.setErrorColumn(columnNumber);
            return result;
        }

        // If we can't parse the specific error, return the full output
        return new SyntaxValidationResult(false,
                "C syntax errors found:\n" + errorOutput);
    }

    private SyntaxValidationResult performBasicCSyntaxCheck(String cCode) {
        // Basic syntax checks when GCC is not available

        // Check for balanced braces
        int braceCount = 0;
        int lineNumber = 1;

        for (int i = 0; i < cCode.length(); i++) {
            char c = cCode.charAt(i);

            if (c == '\n') {
                lineNumber++;
            } else if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount < 0) {
                    SyntaxValidationResult result = new SyntaxValidationResult(false,
                            "Unmatched closing brace");
                    result.setErrorLine(lineNumber);
                    return result;
                }
            }
        }

        if (braceCount != 0) {
            return new SyntaxValidationResult(false,
                    "Unmatched braces: " + Math.abs(braceCount) +
                            (braceCount > 0 ? " opening" : " closing") + " brace(s)");
        }

        // Check for balanced parentheses
        int parenCount = 0;
        lineNumber = 1;

        for (int i = 0; i < cCode.length(); i++) {
            char c = cCode.charAt(i);

            if (c == '\n') {
                lineNumber++;
            } else if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
                if (parenCount < 0) {
                    SyntaxValidationResult result = new SyntaxValidationResult(false,
                            "Unmatched closing parenthesis");
                    result.setErrorLine(lineNumber);
                    return result;
                }
            }
        }

        if (parenCount != 0) {
            return new SyntaxValidationResult(false,
                    "Unmatched parentheses: " + Math.abs(parenCount) +
                            (parenCount > 0 ? " opening" : " closing") + " parenthesis(es)");
        }

        // If basic checks pass
        return new SyntaxValidationResult(true,
                "Basic C syntax checks passed (full validation requires GCC)");
    }

    // Inner class for in-memory Java file compilation
    private static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final String code;

        public InMemoryJavaFileObject(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}