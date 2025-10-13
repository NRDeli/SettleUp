package org.ncsu.settleup.settlementservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the settlement service.  This advice
 * translates common exceptions thrown by controllers into appropriate
 * HTTP responses.  It prevents generic runtime exceptions from
 * bubbling up to the caller as HTTP 500 responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle illegal arguments such as invalid IDs or missing
     * membership associations.  The message from the exception is
     * returned directly in the response body with a 400 status.
     *
     * @param ex the exception
     * @return a bad request response containing the error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    /**
     * Catch-all handler for unexpected runtime exceptions.  Returns
     * an internal server error response without exposing sensitive
     * details to the client.
     *
     * @param ex the exception
     * @return a generic error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred");
    }
}