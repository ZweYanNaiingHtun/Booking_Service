package com.codingproject.digitalbase.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 💡 Helper Method: Request Path ထုတ်ယူရန်
    private String getRequestPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    // 🌟 1. DTO Validation Error (@Valid annotation ကြောင့် တက်သော Error)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        String primaryErrorMessage = "Validation failed";

        // 💡 Validation Error တက်ပါက ပထမဆုံး Error Message ကို တိုက်ရိုက်ယူပြီး message key ထဲ ထည့်ပေးခြင်း
        if (!ex.getBindingResult().getFieldErrors().isEmpty()) {
            primaryErrorMessage = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("message", primaryErrorMessage);
        response.put("path", getRequestPath(request));

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 🌟 2. Custom Business Error (e.g. Current password does not match!)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequestException(
            BadRequestException ex, WebRequest request) {

        String message = ex.getMessage() != null ? ex.getMessage() : "Bad request";

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Bad Request");
        response.put("message", message);
        response.put("path", getRequestPath(request));

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 🌟 3. Resource Not Found Exception
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("error", "Resource Not Found");
        response.put("message", ex.getMessage());
        response.put("path", getRequestPath(request));

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // 🌟 4. Account Not Activated Exception
    @ExceptionHandler(AccountNotActivatedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountNotActivatedException(
            AccountNotActivatedException ex, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.FORBIDDEN.value());
        response.put("error", "Account Not Activated");
        response.put("message", ex.getMessage());
        response.put("path", getRequestPath(request));

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    // 🌟 5. Unauthorized Exception
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("error", "Unauthorized");
        response.put("message", ex.getMessage());
        response.put("path", getRequestPath(request));

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // 🌟 6. JSON Format Error
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleJsonError(
            HttpMessageNotReadableException ex, WebRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Malformed JSON Request");
        response.put("message", "Invalid JSON format in request body");
        response.put("path", getRequestPath(request));

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}