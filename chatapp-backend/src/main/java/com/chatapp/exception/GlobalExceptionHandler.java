package com.chatapp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * ════════════════════════════════════════════════════════════════
 *  GlobalExceptionHandler  —  THE CENTRAL ERROR CATCHER
 * ════════════════════════════════════════════════════════════════
 *
 *  WHAT DOES IT DO?
 *  Without this class, whenever something goes wrong (e.g., email already exists),
 *  Spring would return a messy, ugly error page with a huge stack trace.
 *  That's terrible for a frontend that expects clean JSON.
 *
 *  This class INTERCEPTS all exceptions thrown anywhere in our app and
 *  converts them into clean, consistent JSON responses like:
 *  { "error": "Email already registered" }   with status 409
 *
 *  ANALOGY: Think of this as the "customer service desk" of the restaurant.
 *  When anything goes wrong in the kitchen (exception thrown), the manager
 *  (GlobalExceptionHandler) catches it and sends a polite response to the customer.
 *
 *  @RestControllerAdvice → tells Spring: "This class handles exceptions
 *  from ALL controllers in the app. Whenever any @RestController throws
 *  an exception, check this class first for a matching @ExceptionHandler."
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * HANDLES: ApiException (our custom exceptions)
     * ─────────────────────────────────────────────
     * Catches any ApiException thrown in Service classes and returns
     * the message + HTTP status we specified when throwing it.
     *
     * Example:
     * Service throws: new ApiException("Email already exists", HttpStatus.CONFLICT)
     * This handler returns:
     * HTTP 409 → { "error": "Email already exists" }
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    /**
     * HANDLES: Validation errors (from @Valid annotations)
     * ─────────────────────────────────────────────────────
     * When Spring Validation finds that the incoming request data is invalid
     * (e.g., email is blank, password too short), it throws
     * MethodArgumentNotValidException.
     *
     * This handler catches it and returns ALL validation errors at once:
     * HTTP 400 → { "email": "Please enter a valid email", "name": "Name is required" }
     *
     * This is much more helpful to the user than a generic error.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        // Collect all field errors into a map: { "fieldName": "error message" }
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);  // 400 Bad Request
    }

    /**
     * HANDLES: Any other unexpected exception
     * ─────────────────────────────────────────
     * A safety net. If something unexpected happens that we didn't
     * specifically handle above, this returns a generic 500 error
     * instead of exposing internal details to the client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "An unexpected error occurred. Please try again.");
        // Log the actual error for debugging (in production you'd use a logger here)
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
