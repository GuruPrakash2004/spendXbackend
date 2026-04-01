package com.example.expencetracker.exception;


import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
 
/**
 * Consistent JSON shape returned for every API error.
 *
 * Having a fixed structure means the Vue frontend always knows what to expect
 * and can display errors without fragile string parsing.
 *
 * Example (validation error):
 * {
 *   "status":    400,
 *   "message":   "Validation failed",
 *   "errors":    { "title": "Title is required", "amount": "Amount must be greater than zero" },
 *   "timestamp": "2024-04-01T12:34:56"
 * }
 *
 * Example (not found):
 * {
 *   "status":    404,
 *   "message":   "Expense not found with id: 99",
 *   "timestamp": "2024-04-01T12:34:56"
 * }
 *
 * @JsonInclude(NON_NULL) — the `errors` field is omitted from the JSON
 * when it is null (i.e. for non-validation errors like 404 / 500).
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse implements Serializable {
 
	private static final long serialVersionUID = 1L;

	/** HTTP status code (e.g. 400, 404, 500). */
    private int status;
 
    /** Human-readable summary of what went wrong. */
    private String message;
 
    /** Field-level validation errors, populated only when status = 400. */
    private Map<String, String> errors;
 
    /** When the error occurred — useful for log correlation. */
    private LocalDateTime timestamp;

	// Constructors
	public ErrorResponse() {
	}

	public ErrorResponse(int status, String message, Map<String, String> errors, LocalDateTime timestamp) {
		this.status = status;
		this.message = message;
		this.errors = errors;
		this.timestamp = timestamp;
	}

	// Getters and Setters
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Map<String, String> getErrors() {
		return errors;
	}

	public void setErrors(Map<String, String> errors) {
		this.errors = errors;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
}
