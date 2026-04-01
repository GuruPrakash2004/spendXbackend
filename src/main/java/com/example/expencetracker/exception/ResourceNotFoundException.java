package com.example.expencetracker.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
 
/**
 * Thrown when a requested resource (expense, category) does not exist in the DB.
 *
 * @ResponseStatus(HttpStatus.NOT_FOUND) is a hint for Spring MVC, but since we
 * use @RestControllerAdvice we handle the mapping explicitly in GlobalExceptionHandler.
 * The annotation is kept for documentation clarity.
 *
 * Extending RuntimeException (unchecked) means callers don't need to
 * declare or catch it — it propagates naturally up to the global handler.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
 
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * @param message — descriptive message e.g. "Expense not found with id: 42"
     *                  This message is returned directly in the API error response.
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
 
