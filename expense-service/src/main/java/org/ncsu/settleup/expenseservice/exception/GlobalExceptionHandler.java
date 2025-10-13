package org.ncsu.settleup.expenseservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for the expense service.  This advice
 * intercepts known exceptions thrown from controllers and service
 * layers and converts them into appropriate HTTP responses with
 * meaningful error messages.  Without this handler, unchecked
 * exceptions (e.g. IllegalArgumentException) would propagate to
 * Spring's default error handler and result in HTTP 500 errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle invalid argument scenarios.  These include cases where
     * the group or members do not exist or where the splits do not
     * sum correctly.  The message from the exception is returned
     * directly to the client.
     *
     * @param ex the exception
     * @return a bad request response with the exception message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    /**
     * Catch-all handler for any other runtime exception that escapes
     * from the controller.  This ensures that callers receive a
     * predictable JSON body instead of a HTML error page.
     *
     * @param ex the exception
     * @return an internal server error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        // In a production system we would log this exception.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred");
    }
}