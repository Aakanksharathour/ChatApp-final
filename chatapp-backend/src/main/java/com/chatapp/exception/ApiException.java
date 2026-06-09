package com.chatapp.exception;

import org.springframework.http.HttpStatus;

/**
 * ════════════════════════════════════════════════════════════════
 *  ApiException  —  OUR CUSTOM ERROR
 * ════════════════════════════════════════════════════════════════
 *
 *  WHAT IS A CUSTOM EXCEPTION?
 *  Java has built-in exceptions like RuntimeException, NullPointerException etc.
 *  But those are too generic. We want OUR OWN exception that:
 *  1. Has a meaningful error message ("Email already registered")
 *  2. Has an HTTP status code (400, 401, 409, etc.)
 *
 *  So instead of throwing:
 *    throw new RuntimeException("Email already registered");
 *
 *  We throw:
 *    throw new ApiException("Email already registered", HttpStatus.CONFLICT);
 *
 *  Then our GlobalExceptionHandler catches it and formats a nice JSON response:
 *  { "error": "Email already registered" }  with status 409
 *
 *  WHY extend RuntimeException?
 *  RuntimeException = "unchecked" exception → we don't have to declare it
 *  in every method signature with "throws ApiException". Cleaner code.
 */
public class ApiException extends RuntimeException {

    /** The HTTP status code to send (400, 401, 409, etc.) */
    private final HttpStatus status;

    /**
     * Constructor.
     * @param message  the error message (sent to frontend)
     * @param status   the HTTP status code
     */
    public ApiException(String message, HttpStatus status) {
        super(message);          // passes message to RuntimeException
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
