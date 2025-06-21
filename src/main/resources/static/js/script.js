// Configuration
const CONFIG = {
    API_BASE_URL: 'http://localhost:8080/api',
    MAX_FILE_SIZE: 10 * 1024 * 1024, // 10MB
    SUPPORTED_IMAGE_TYPES: ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/bmp'],
    TOAST_DURATION: 5000
};

// DOM Elements
const elements = {
    // Type selector
    typeButtons: document.querySelectorAll('.type-btn'),
    textForm: document.getElementById('textForm'),
    imageForm: document.getElementById('imageForm'),

    // Text form elements
    sourceLanguage: document.getElementById('sourceLanguage'),
    targetLanguage: document.getElementById('targetLanguage'),
    swapLanguages: document.getElementById('swapLanguages'),
    sourceCode: document.getElementById('sourceCode'),
    translatedCode: document.getElementById('translatedCode'),
    validateSyntax: document.getElementById('validateSyntax'),
    translateBtn: document.getElementById('translateBtn'),
    clearCode: document.getElementById('clearCode'),
    copySource: document.getElementById('copySource'),
    copyTranslated: document.getElementById('copyTranslated'),
    downloadCode: document.getElementById('downloadCode'),

    // Image form elements
    imageSourceLanguage: document.getElementById('imageSourceLanguage'),
    imageTargetLanguage: document.getElementById('imageTargetLanguage'),
    swapImageLanguages: document.getElementById('swapImageLanguages'),
    imageFile: document.getElementById('imageFile'),
    uploadArea: document.getElementById('uploadArea'),
    imagePreview: document.getElementById('imagePreview'),
    previewImage: document.getElementById('previewImage'),
    removeImage: document.getElementById('removeImage'),
    imageTranslatedCode: document.getElementById('imageTranslatedCode'),
    imageValidateSyntax: document.getElementById('imageValidateSyntax'),
    translateImageBtn: document.getElementById('translateImageBtn'),
    copyImageTranslated: document.getElementById('copyImageTranslated'),
    downloadImageCode: document.getElementById('downloadImageCode'),

    // Status
    statusMessage: document.getElementById('statusMessage')
};

// State
let currentTranslationType = 'text';
let currentImageFile = null;

// Initialize application
document.addEventListener('DOMContentLoaded', function() {
    initializeEventListeners();
    loadSampleCode();
});

// Event Listeners
function initializeEventListeners() {
    // Type selector
    elements.typeButtons.forEach(btn => {
        btn.addEventListener('click', handleTypeChange);
    });

    // Text form events
    elements.swapLanguages.addEventListener('click', swapTextLanguages);
    elements.translateBtn.addEventListener('click', handleTextTranslation);
    elements.clearCode.addEventListener('click', clearSourceCode);
    elements.copySource.addEventListener('click', () => copyToClipboard(elements.sourceCode.value));
    elements.copyTranslated.addEventListener('click', () => copyToClipboard(elements.translatedCode.textContent));
    elements.downloadCode.addEventListener('click', downloadTranslatedCode);

    // Image form events
    elements.swapImageLanguages.addEventListener('click', swapImageLanguages);
    elements.uploadArea.addEventListener('click', () => elements.imageFile.click());
    elements.uploadArea.addEventListener('dragover', handleDragOver);
    elements.uploadArea.addEventListener('dragleave', handleDragLeave);
    elements.uploadArea.addEventListener('drop', handleDrop);
    elements.imageFile.addEventListener('change', handleImageFileSelect);
    elements.removeImage.addEventListener('click', removeSelectedImage);
    elements.translateImageBtn.addEventListener('click', handleImageTranslation);
    elements.copyImageTranslated.addEventListener('click', () => copyToClipboard(elements.imageTranslatedCode.textContent));
    elements.downloadImageCode.addEventListener('click', downloadImageTranslatedCode);

    // Status message close
    elements.statusMessage.addEventListener('click', (e) => {
        if (e.target.classList.contains('status-close')) {
            hideStatusMessage();
        }
    });

    // Auto-resize textareas
    elements.sourceCode.addEventListener('input', autoResizeTextarea);
}

// Type Change Handler
function handleTypeChange(e) {
    const type = e.currentTarget.dataset.type;

    // Update active button
    elements.typeButtons.forEach(btn => btn.classList.remove('active'));
    e.currentTarget.classList.add('active');

    // Show/hide forms
    if (type === 'text') {
        elements.textForm.classList.remove('hidden');
        elements.imageForm.classList.add('hidden');
    } else {
        elements.textForm.classList.add('hidden');
        elements.imageForm.classList.remove('hidden');
    }

    currentTranslationType = type;
    hideStatusMessage();
}

// Text Translation Functions
function swapTextLanguages() {
    const temp = elements.sourceLanguage.value;
    elements.sourceLanguage.value = elements.targetLanguage.value;
    elements.targetLanguage.value = temp;

    // Clear translated code when swapping
    elements.translatedCode.innerHTML = '<div class="placeholder">Translated code will appear here...</div>';
}

async function handleTextTranslation() {
    const sourceCode = elements.sourceCode.value.trim();

    if (!sourceCode) {
        showStatusMessage('Please enter some code to translate.', 'error');
        return;
    }

    const sourceLanguage = elements.sourceLanguage.value;
    const targetLanguage = elements.targetLanguage.value;

    if (sourceLanguage === targetLanguage) {
        showStatusMessage('Source and target languages cannot be the same.', 'error');
        return;
    }

    setTranslationLoading(true);

    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/translate/text`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                sourceCode: sourceCode,
                sourceLanguage: sourceLanguage,
                targetLanguage: targetLanguage,
                validateSyntax: elements.validateSyntax.checked
            })
        });

        const result = await response.json();

        if (result.success) {
            displayTranslatedCode(result.translatedCode);

            let message = 'Translation completed successfully!';
            if (result.syntaxValidation && !result.syntaxValidation.valid) {
                message += ` Note: ${result.syntaxValidation.errorMessage}`;
                showStatusMessage(message, 'warning');
            } else {
                showStatusMessage(message, 'success');
            }
        } else {
            showStatusMessage(`Translation failed: ${result.message}`, 'error');
            displayTranslatedCode('');
        }

    } catch (error) {
        console.error('Translation error:', error);
        showStatusMessage('Network error occurred. Please check your connection and try again.', 'error');
        displayTranslatedCode('');
    } finally {
        setTranslationLoading(false);
    }
}

function displayTranslatedCode(code) {
    if (code.trim()) {
        elements.translatedCode.textContent = code;
        elements.translatedCode.classList.remove('placeholder');
    } else {
        elements.translatedCode.innerHTML = '<div class="placeholder">Translation failed or no code generated...</div>';
    }
}

function clearSourceCode() {
    elements.sourceCode.value = '';
    elements.translatedCode.innerHTML = '<div class="placeholder">Translated code will appear here...</div>';
    hideStatusMessage();
}

// Image Translation Functions
function swapImageLanguages() {
    const temp = elements.imageSourceLanguage.value;
    elements.imageSourceLanguage.value = elements.imageTargetLanguage.value;
    elements.imageTargetLanguage.value = temp;

    // Clear translated code when swapping
    elements.imageTranslatedCode.innerHTML = '<div class="placeholder">Upload an image to extract and translate code...</div>';
}

function handleDragOver(e) {
    e.preventDefault();
    elements.uploadArea.classList.add('dragover');
}

function handleDragLeave(e) {
    e.preventDefault();
    elements.uploadArea.classList.remove('dragover');
}

function handleDrop(e) {
    e.preventDefault();
    elements.uploadArea.classList.remove('dragover');

    const files = e.dataTransfer.files;
    if (files.length > 0) {
        handleImageFile(files[0]);
    }
}

function handleImageFileSelect(e) {
    const file = e.target.files[0];
    if (file) {
        handleImageFile(file);
    }
}

function handleImageFile(file) {
    // Validate file type
    if (!CONFIG.SUPPORTED_IMAGE_TYPES.includes(file.type)) {
        showStatusMessage('Please select a valid image file (JPG, PNG, GIF, BMP).', 'error');
        return;
    }

    // Validate file size
    if (file.size > CONFIG.MAX_FILE_SIZE) {
        showStatusMessage('File size must be less than 10MB.', 'error');
        return;
    }

    currentImageFile = file;

    // Display image preview
    const reader = new FileReader();
    reader.onload = function(e) {
        elements.previewImage.src = e.target.result;
        elements.uploadArea.classList.add('hidden');
        elements.imagePreview.classList.remove('hidden');
        elements.translateImageBtn.disabled = false;
    };
    reader.readAsDataURL(file);
}

function removeSelectedImage() {
    currentImageFile = null;
    elements.imageFile.value = '';
    elements.uploadArea.classList.remove('hidden');
    elements.imagePreview.classList.add('hidden');
    elements.translateImageBtn.disabled = true;
    elements.imageTranslatedCode.innerHTML = '<div class="placeholder">Upload an image to extract and translate code...</div>';
    hideStatusMessage();
}

async function handleImageTranslation() {
    if (!currentImageFile) {
        showStatusMessage('Please select an image file first.', 'error');
        return;
    }

    const sourceLanguage = elements.imageSourceLanguage.value;
    const targetLanguage = elements.imageTargetLanguage.value;

    if (sourceLanguage === targetLanguage) {
        showStatusMessage('Source and target languages cannot be the same.', 'error');
        return;
    }

    setImageTranslationLoading(true);

    try {
        const formData = new FormData();
        formData.append('image', currentImageFile);
        formData.append('sourceLanguage', sourceLanguage);
        formData.append('targetLanguage', targetLanguage);
        formData.append('validateSyntax', elements.imageValidateSyntax.checked);

        const response = await fetch(`${CONFIG.API_BASE_URL}/translate/image`, {
            method: 'POST',
            body: formData
        });

        const result = await response.json();

        if (result.success) {
            displayImageTranslatedCode(result.translatedCode);

            let message = 'Image translation completed successfully!';
            if (result.message.includes('OCR confidence')) {
                message = result.message;
            }

            if (result.syntaxValidation && !result.syntaxValidation.valid) {
                message += ` Note: ${result.syntaxValidation.errorMessage}`;
                showStatusMessage(message, 'warning');
            } else {
                showStatusMessage(message, 'success');
            }
        } else {
            showStatusMessage(`Image translation failed: ${result.message}`, 'error');
            displayImageTranslatedCode('');
        }

    } catch (error) {
        console.error('Image translation error:', error);
        showStatusMessage('Network error occurred. Please check your connection and try again.', 'error');
        displayImageTranslatedCode('');
    } finally {
        setImageTranslationLoading(false);
    }
}

function displayImageTranslatedCode(code) {
    if (code.trim()) {
        elements.imageTranslatedCode.textContent = code;
        elements.imageTranslatedCode.classList.remove('placeholder');
    } else {
        elements.imageTranslatedCode.innerHTML = '<div class="placeholder">Translation failed or no code generated...</div>';
    }
}

// Utility Functions
function setTranslationLoading(loading) {
    elements.translateBtn.disabled = loading;
    elements.translateBtn.classList.toggle('loading', loading);

    if (loading) {
        elements.translateBtn.querySelector('.btn-text').style.opacity = '0';
        elements.translateBtn.querySelector('.btn-loader').classList.remove('hidden');
    } else {
        elements.translateBtn.querySelector('.btn-text').style.opacity = '1';
        elements.translateBtn.querySelector('.btn-loader').classList.add('hidden');
    }
}

function setImageTranslationLoading(loading) {
    elements.translateImageBtn.disabled = loading || !currentImageFile;
    elements.translateImageBtn.classList.toggle('loading', loading);

    if (loading) {
        elements.translateImageBtn.querySelector('.btn-text').style.opacity = '0';
        elements.translateImageBtn.querySelector('.btn-loader').classList.remove('hidden');
    } else {
        elements.translateImageBtn.querySelector('.btn-text').style.opacity = '1';
        elements.translateImageBtn.querySelector('.btn-loader').classList.add('hidden');
    }
}

async function copyToClipboard(text) {
    if (!text || text.trim() === '') {
        showStatusMessage('No content to copy.', 'error');
        return;
    }

    try {
        await navigator.clipboard.writeText(text);
        showStatusMessage('Code copied to clipboard!', 'success');
    } catch (error) {
        console.error('Copy failed:', error);
        showStatusMessage('Failed to copy to clipboard.', 'error');
    }
}

function downloadTranslatedCode() {
    const code = elements.translatedCode.textContent;
    if (!code || code.trim() === '') {
        showStatusMessage('No translated code to download.', 'error');
        return;
    }

    const targetLanguage = elements.targetLanguage.value;
    const extension = getFileExtension(targetLanguage);
    const filename = `translated_code${extension}`;

    downloadFile(code, filename);
}

function downloadImageTranslatedCode() {
    const code = elements.imageTranslatedCode.textContent;
    if (!code || code.trim() === '') {
        showStatusMessage('No translated code to download.', 'error');
        return;
    }

    const targetLanguage = elements.imageTargetLanguage.value;
    const extension = getFileExtension(targetLanguage);
    const filename = `translated_from_image${extension}`;

    downloadFile(code, filename);
}

function downloadFile(content, filename) {
    const blob = new Blob([content], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);

    showStatusMessage(`File downloaded: ${filename}`, 'success');
}

function getFileExtension(language) {
    switch (language.toLowerCase()) {
        case 'java':
            return '.java';
        case 'c':
            return '.c';
        default:
            return '.txt';
    }
}

function showStatusMessage(message, type = 'info') {
    elements.statusMessage.className = `status-message ${type}`;
    elements.statusMessage.querySelector('.status-text').textContent = message;
    elements.statusMessage.classList.remove('hidden');

    // Auto-hide after duration
    setTimeout(() => {
        hideStatusMessage();
    }, CONFIG.TOAST_DURATION);
}

function hideStatusMessage() {
    elements.statusMessage.classList.add('hidden');
}

function autoResizeTextarea(e) {
    const textarea = e.target;
    textarea.style.height = 'auto';
    textarea.style.height = textarea.scrollHeight + 'px';
}

function loadSampleCode() {
    const sampleJavaCode = `public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        // Variables
        int number = 42;
        String message = "Java to C Translation";

        // Loop example
        for (int i = 0; i < 5; i++) {
            System.out.println("Count: " + i);
        }

        // Method call
        printMessage(message);
    }

    public static void printMessage(String msg) {
        System.out.println("Message: " + msg);
    }
}`;

    elements.sourceCode.value = sampleJavaCode;
}

// API Health Check
async function checkAPIHealth() {
    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/translate/health`);
        if (response.ok) {
            console.log('API is healthy');
        } else {
            console.warn('API health check failed');
        }
    } catch (error) {
        console.error('API is not reachable:', error);
    }
}

// Check API health on load
checkAPIHealth();