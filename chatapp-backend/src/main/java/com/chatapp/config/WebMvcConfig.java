package com.chatapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ════════════════════════════════════════════════════════════════
 *  WebMvcConfig  —  Configures static file serving
 * ════════════════════════════════════════════════════════════════
 *
 *  PROBLEM:
 *  ─────────────────────────────────────────────────────────────
 *  When a user uploads a photo, we save it to the "uploads/" folder on disk.
 *  But Spring Boot by default only serves static files from:
 *    - src/main/resources/static/
 *    - src/main/resources/public/
 *
 *  Files uploaded at runtime go to the "uploads/" folder OUTSIDE those paths.
 *  So Spring wouldn't know how to serve them back to the browser.
 *
 *  SOLUTION:
 *  ─────────────────────────────────────────────────────────────
 *  We tell Spring: "When someone requests /uploads/**, look in the uploads/ folder."
 *
 *  Example:
 *  File saved to:  uploads/abc-123.jpg
 *  Accessible at:  http://localhost:8080/uploads/abc-123.jpg
 *
 *  FILE UPLOAD FLOW:
 *  ─────────────────────────────────────────────────────────────
 *  1. User POSTs /api/upload with image file
 *  2. UploadService saves file as uploads/abc-123.jpg
 *  3. Returns URL: http://localhost:8080/uploads/abc-123.jpg
 *  4. User puts that URL in a message
 *  5. Other user's browser loads the image from that URL
 *  6. Spring serves it via this resource handler ← this class enables step 6
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map URL pattern /uploads/** → physical folder uploads/
        // "file:" prefix means look on the filesystem (not in classpath)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
