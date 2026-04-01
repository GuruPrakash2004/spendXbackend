package com.example.expencetracker.exception;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
* Centralised exception handling for the entire application.
*
* @RestControllerAdvice intercepts exceptions thrown from any @RestController
* and converts them into structured JSON error responses.
*
* Without this class, Spring would return raw HTML error pages or
* unformatted stack traces — neither useful for an API client.
*
* Every exception type maps to exactly one HTTP status code and
* a consistent JSON shape that the frontend can reliably parse.
*/


@RestControllerAdvice
public class GlobalExceptionHandler {
	
	 
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
   // ─────────────────────────────────────────────────────
   // 404 — Resource not found
   // ─────────────────────────────────────────────────────

   /**
    * Handles lookups for expenses / categories that don't exist in the DB.
    * Returns 404 with a human-readable message.
    */
   @ExceptionHandler(ResourceNotFoundException.class)
   public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
       log.warn("Resource not found: {}", ex.getMessage());
       return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
   }


   // ─────────────────────────────────────────────────────
   // 400 — Validation failures
   // ─────────────────────────────────────────────────────

   /**
    * Fires when @Valid on a request body finds constraint violations.
    *
    * Instead of one generic "validation failed" message, we collect every
    * field-level error into a map { fieldName: "message" } so the frontend
    * can highlight the exact fields that need fixing.
    *
    * Example response body:
    * {
    *   "status": 400,
    *   "message": "Validation failed",
    *   "errors": { "amount": "Amount must be greater than zero", "title": "Title is required" },
    *   "timestamp": "2024-04-01T12:00:00"
    * }
    */
   @ExceptionHandler(MethodArgumentNotValidException.class)
   public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
       Map<String, String> fieldErrors = new HashMap<>();

       // Collect each field error: field name → default message
       for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
           fieldErrors.put(fe.getField(), fe.getDefaultMessage());
       }

       log.warn("Validation failed: {}", fieldErrors);

       ErrorResponse error = new ErrorResponse(
               HttpStatus.BAD_REQUEST.value(),
               "Validation failed",
               fieldErrors,
               LocalDateTime.now()
       );

       return ResponseEntity.badRequest().body(error);
   }


   // ─────────────────────────────────────────────────────
   // 500 — Anything else
   // ─────────────────────────────────────────────────────

   /**
    * Safety net for any unhandled runtime exceptions.
    * We log the full stack trace here but return only a generic message
    * to the client — we never leak internal details (stack traces, SQL, etc.).
    */
   @ExceptionHandler(Exception.class)
   public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
       log.error("Unexpected error: {}", ex.getMessage(), ex);
       return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
   }


   // ─────────────────────────────────────────────────────
   // HELPER
   // ─────────────────────────────────────────────────────

   /** Builds a standard error response with no field-level detail. */
   private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String message) {
       ErrorResponse error = new ErrorResponse(
               status.value(),
               message,
               null,
               LocalDateTime.now()
       );
       return ResponseEntity.status(status).body(error);
   }
}