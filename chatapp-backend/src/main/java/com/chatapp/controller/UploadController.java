package com.chatapp.controller;

import com.chatapp.exception.ApiException;
import com.chatapp.service.UploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * ════════════════════════════════════════════════════════════════
 *  UploadController  —  POST /api/upload
 * ════════════════════════════════════════════════════════════════
 *
 *  ENDPOINT:
 *  ─────────────────────────────────────────────────────────────
 *  POST http://localhost:8080/api/upload
 *  Auth: Required (JWT Bearer token)
 *
 *  REQUEST (Postman → Body → form-data):
 *  Key: "file"  Type: File  Value: [select a file]
 *
 *  SUCCESS RESPONSE (200 OK):
 *  {
 *    "success": true,
 *    "url":     "http://localhost:8080/uploads/a1b2c3d4-....jpg",
 *    "type":    "image",
 *    "sizeKB":  248
 *  }
 *
 *  ERROR RESPONSES:
 *  400 → No file selected
 *  400 → Unsupported file type
 *  413 → File exceeds 10 MB
 *
 *  WHAT IS @RequestParam("file") MultipartFile?
 *  ─────────────────────────────────────────────────────────────
 *  Multipart = a way to send binary data (files) in an HTTP request.
 *  When you pick a file in Postman (form-data, type=File), it gets
 *  sent as multipart. Spring receives it as a MultipartFile object.
 *
 *  @RequestParam("file") = "the form-data field named 'file'"
 *
 *  HOW TO TEST IN POSTMAN:
 *  ─────────────────────────────────────────────────────────────
 *  1. Method: POST
 *  2. URL: http://localhost:8080/api/upload
 *  3. Authorization: Bearer <your JWT token>
 *  4. Body → form-data
 *     Key: file   Type: (change to File)   Value: (pick any image)
 *  5. Send
 *  6. Copy the returned "url" — paste it in a browser to view the image!
 */
@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "http://localhost:5173")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new ApiException("No file selected.", HttpStatus.BAD_REQUEST);
        }

        Map<String, Object> response = uploadService.uploadFile(file);
        return ResponseEntity.ok(response);
    }
}
