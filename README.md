<<<<<<< HEAD
# Source-Code_Translator
=======
# Java ⇄ C Code Translator

A Spring Boot application that translates Java code to C and vice versa using AI (OpenAI GPT-3.5) with OCR support for image-based code input.

## 🚀 Features

- **Bidirectional Translation**: Convert Java to C and C to Java
- **AI-Powered**: Uses OpenAI GPT-3.5 for intelligent code translation
- **OCR Support**: Extract code from images using Tesseract OCR
- **Syntax Validation**: Validates both source and translated code syntax
- **Web Interface**: Clean, responsive frontend with drag-and-drop image upload
- **REST API**: Full REST API for programmatic access
- **Real-time Translation**: Fast, real-time code translation

## 🛠️ Tech Stack

### Backend
- **Spring Boot 3.2.0** - Main framework
- **Java 17** - Programming language
- **OpenAI GPT-3.5** - AI translation engine
- **Tesseract OCR** - Image text extraction
- **Maven** - Build tool
- **WebFlux** - Reactive HTTP client

### Frontend
- **HTML5/CSS3/JavaScript** - Pure web technologies
- **Responsive Design** - Works on all devices
- **Modern UI** - Clean, professional interface

### Tools & Libraries
- **Tess4J** - Java wrapper for Tesseract OCR
- **Jackson** - JSON processing
- **JavaCompiler API** - Java syntax validation
- **GCC** - C syntax validation

## 📋 Prerequisites

Before running this application, make sure you have:

1. **Java 17 or higher** installed
2. **Maven 3.6+** installed
3. **OpenAI API Key** (GPT-3.5 access)
4. **Tesseract OCR** installed on your system
5. **GCC compiler** (for C syntax validation)

### Installing Tesseract OCR

#### Windows:
```bash
# Download from: https://github.com/UB-Mannheim/tesseract/wiki
# Or use chocolatey:
choco install tesseract
```

#### macOS:
```bash
brew install tesseract
```

#### Ubuntu/Debian:
```bash
sudo apt-get update
sudo apt-get install tesseract-ocr tesseract-ocr-eng libtesseract-dev
```

### Installing GCC

#### Windows:
```bash
# Install MinGW-w64 or use WSL
# Or download from: https://gcc.gnu.org/install/
```

#### macOS:
```bash
# Install Xcode Command Line Tools
xcode-select --install
```

#### Ubuntu/Debian:
```bash
sudo apt-get install gcc build-essential
```

## 🔧 Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/code-translator.git
cd code-translator
```

### 2. Configure Environment Variables

Create a `.env` file in the root directory:

```bash
cp .env.example .env
```

Edit the `.env` file and add your OpenAI API key:

```bash
OPENAI_API_KEY=your_actual_openai_api_key_here
TESSERACT_DATA_PATH=/usr/share/tesseract-ocr/4.00/tessdata
```

### 3. Update Application Configuration

Edit `src/main/resources/application.yml` if needed:

```yaml
openai:
  api:
    key: ${OPENAI_API_KEY:your-openai-api-key-here}
```

### 4. Build the Project

```bash
mvn clean install
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

Or run the JAR file:

```bash
java -jar target/code-translator-1.0.0.jar
```

### 6. Access the Application

- **Web Interface**: http://localhost:8080
- **API Base URL**: http://localhost:8080/api
- **Health Check**: http://localhost:8080/api/translate/health

## 🔗 API Documentation

### Translate Text Code

**Endpoint**: `POST /api/translate/text`

**Request Body**:
```json
{
  "sourceCode": "public class Hello { public static void main(String[] args) { System.out.println(\"Hello World\"); } }",
  "sourceLanguage": "java",
  "targetLanguage": "c",
  "validateSyntax": true
}
```

**Response**:
```json
{
  "originalCode": "public class Hello...",
  "translatedCode": "#include <stdio.h>\nint main() {...}",
  "sourceLanguage": "java",
  "targetLanguage": "c",
  "success": true,
  "message": "Translation completed successfully",
  "syntaxValidation": {
    "valid": true,
    "errorMessage": null
  }
}
```

### Translate from Image

**Endpoint**: `POST /api/translate/image`

**Request**: Multipart form data
- `image`: Image file (JPG, PNG, GIF, BMP)
- `sourceLanguage`: "java" or "c"
- `targetLanguage`: "java" or "c"
- `validateSyntax`: true/false

**Response**: Same as text translation

### Health Check

**Endpoint**: `GET /api/translate/health`

**Response**:
```json
{
  "status": "UP",
  "service": "Code Translation API",
  "timestamp": "2025-06-12T10:30:00Z"
}
```

### Supported Languages

**Endpoint**: `GET /api/translate/languages`

**Response**:
```json
{
  "supported": ["java", "c"],
  "translations": ["java-to-c", "c-to-java"]
}
```

## 🐳 Docker Support

### Build Docker Image

```bash
docker build -t code-translator .
```

### Run with Docker

```bash
docker run -d \
  --name code-translator \
  -p 8080:8080 \
  -e OPENAI_API_KEY=your_api_key_here \
  code-translator
```

### Docker Compose

Create `docker-compose.yml`:

```yaml
version: '3.8'
services:
  code-translator:
    build: .
    ports:
      - "8080:8080"
    environment:
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - SPRING_PROFILES_ACTIVE=production
    volumes:
      - ./uploads:/app/uploads
```

Run with:
```bash
docker-compose up -d
```

## 🧪 Testing

### Run Unit Tests

```bash
mvn test
```

### Test with Sample Files

The project includes sample code files in the `samples/` directory:

- `samples/java/` - Java code examples
- `samples/c/` - C code examples
- `samples/images/` - Sample code images for OCR testing

### Manual Testing

1. **Text Translation**:
    - Go to http://localhost:8080
    - Paste Java or C code
    - Select source and target languages
    - Click "Translate Code"

2. **Image Translation**:
    - Switch to "Image Upload" tab
    - Upload an image containing code
    - Select languages
    - Click "Translate from Image"

## 🔍 Troubleshooting

### Common Issues

1. **OpenAI API Key Issues**:
    - Ensure your API key is valid and has GPT-3.5 access
    - Check your OpenAI account balance

2. **Tesseract OCR Not Found**:
   ```bash
   # Linux/Mac
   export TESSERACT_DATA_PATH=/usr/share/tesseract-ocr/4.00/tessdata
   
   # Windows
   set TESSERACT_DATA_PATH=C:\Program Files\Tesseract-OCR\tessdata
   ```

3. **GCC Not Found**:
    - Install GCC compiler for C syntax validation
    - Ensure GCC is in your system PATH

4. **Port Already in Use**:
   ```bash
   # Change port in application.yml
   server:
     port: 8081
   ```

5. **CORS Issues**:
    - Frontend and backend must run on allowed origins
    - Check WebConfig.java for CORS settings

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.translator: DEBUG
    org.springframework.web: DEBUG
```

## 📁 Project Structure

```
code-translator/
├── src/main/java/com/translator/
│   ├── CodeTranslatorApplication.java
│   ├── controller/TranslationController.java
│   ├── service/
│   │   ├── OpenAIService.java
│   │   ├── OCRService.java
│   │   ├── SyntaxValidationService.java
│   │   └── TranslationService.java
│   ├── dto/ (Data Transfer Objects)
│   ├── config/WebConfig.java
│   └── exception/GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   └── static/ (Frontend files)
├── samples/ (Sample code files)
├── docs/ (Documentation)
└── Dockerfile
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **OpenAI** for providing the GPT-3.5 API
- **Tesseract OCR** for optical character recognition
- **Spring Boot** community for the excellent framework
- **Contributors** and **testers**

## 📞 Support

If you encounter any issues or have questions:

1. Check the [Troubleshooting](#-troubleshooting) section
2. Search existing [GitHub Issues](https://github.com/yourusername/code-translator/issues)
3. Create a new issue with detailed information
4. Contact the maintainers

---

**Made with ❤️ using Spring Boot, OpenAI GPT-3.5, and Tesseract OCR**
>>>>>>> 5101c77 (Initial commit)
