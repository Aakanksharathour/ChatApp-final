package com.chatapp.service;

import com.chatapp.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ════════════════════════════════════════════════════════════════
 *  UploadService  —  Saves files and returns their URL
 * ════════════════════════════════════════════════════════════════
 *
 *  HOW FILE UPLOAD WORKS (end to end):
 *  ─────────────────────────────────────────────────────────────
 *
 *  1. React frontend sends a multipart/form-data POST request:
 *     POST /api/upload
 *     Content-Type: multipart/form-data
 *     Body: file=<the actual image bytes>
 *
 *  2. UploadController receives it as a MultipartFile object.
 *
 *  3. This service:
 *     a) Validates: is size <= 10MB? Is extension allowed?
 *     b) Generates a unique filename: UUID + original extension
 *        Example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg"
 *     c) Creates the uploads/ folder if it doesn't exist
 *     d) Saves the bytes to disk at uploads/a1b2c3d4.jpg
 *     e) Returns the public URL: http://localhost:8080/uploads/a1b2c3d4.jpg
 *
 *  4. The user then sends a message with that URL as the text field.
 *     Other users click the URL to view the image.
 *
 *  WHAT IS MultipartFile?
 *  ─────────────────────────────────────────────────────────────
 *  When you upload a file via a form or Postman, Spring wraps it in
 *  a MultipartFile object. It contains:
 *    - file.getOriginalFilename()  → "photo.jpg"
 *    - file.getSize()              → 248000 (bytes)
 *    - file.getInputStream()       → the actual file data (bytes)
 *    - file.getContentType()       → "image/jpeg"
 *
 *  WHAT IS UUID?
 *  ─────────────────────────────────────────────────────────────
 *  UUID = Universally Unique Identifier
 *  Example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *
 *  We use it as the filename so:
 *  - No two files ever have the same name (no overwrites)
 *  - No one can guess another user's file URL (basic security)
 *
 *  @Value reads config values from application.properties
 *  app.upload.dir  → "uploads"
 *  app.base-url    → "http://localhost:8080"
 */
@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;  // 10 MB in bytes

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "pdf", "mp4", "mp3", "zip"
    );

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif"
    );

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Validates, saves, and returns the URL for an uploaded file.
     *
     * @param file  the uploaded file from the HTTP request
     * @return      Map with: success, url, type, sizeKB
     */
    public Map<String, Object> uploadFile(MultipartFile file) throws IOException {

        // ── STEP 1: Validate file size ────────────────────────────────
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ApiException(
                    "File size exceeds the 10 MB limit. Your file: " + (file.getSize() / 1024 / 1024) + " MB",
                    HttpStatus.PAYLOAD_TOO_LARGE   // 413
            );
        }

        // ── STEP 2: Get the file extension ────────────────────────────
        // getOriginalFilename() → "photo.jpg"
        // We take the part after the last dot → "jpg"
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            throw new ApiException("Invalid file — no extension found.", HttpStatus.BAD_REQUEST);
        }

        String extension = originalName
                .substring(originalName.lastIndexOf(".") + 1)
                .toLowerCase();

        // ── STEP 3: Validate extension ────────────────────────────────
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ApiException(
                    "File type '." + extension + "' is not allowed. Supported: jpg, png, gif, pdf, mp4, mp3, zip",
                    HttpStatus.BAD_REQUEST
            );
        }

        // ── STEP 4: Generate a unique filename ────────────────────────
        // UUID.randomUUID() generates something like: a1b2c3d4-e5f6-7890-abcd-ef1234567890
        // Final filename: a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;

        // ── STEP 5: Create uploads folder if it doesn't exist ─────────
        // Paths.get("uploads") → a Path object pointing to the uploads/ folder
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
        }

        // ── STEP 6: Save file to disk ─────────────────────────────────
        // resolve() = join paths: "uploads/" + "a1b2c3d4.jpg" = "uploads/a1b2c3d4.jpg"
        // Files.copy() = reads from InputStream and writes bytes to the path
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);

        log.info("File saved: {} ({} KB)", uniqueFilename, file.getSize() / 1024);

        // ── STEP 7: Build response ────────────────────────────────────
        String fileType = IMAGE_EXTENSIONS.contains(extension) ? "image" : extension;
        long sizeKB = file.getSize() / 1024;

        // The public URL where anyone can download/view this file
        String publicUrl = baseUrl + "/uploads/" + uniqueFilename;

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("url",     publicUrl);
        response.put("type",    fileType);
        response.put("sizeKB",  sizeKB);
        return response;
    }
}
