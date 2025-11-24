package org.ncsu.settleup.settlementservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GlobalExceptionHandler}.  These tests invoke each
 * exception handler method directly and assert that the returned
 * {@link ResponseEntity} has the correct HTTP status code and body.
 */
public class GlobalExceptionHandlerTest {

    @Test
    void handleIllegalArgument_returnsBadRequestWithMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        String errorMessage = "Invalid parameter";
        ResponseEntity<String> response = handler.handleIllegalArgument(new IllegalArgumentException(errorMessage));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "Status should be 400 Bad Request");
        assertEquals(errorMessage, response.getBody(), "Response body should contain the exception message");
    }

    @Test
    void handleGeneralException_returnsInternalServerErrorWithGenericMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<String> response = handler.handleGeneralException(new RuntimeException("boom"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), "Status should be 500 Internal Server Error");
        assertEquals("An unexpected error occurred", response.getBody(), "Response body should contain a generic error message");
    }
}
