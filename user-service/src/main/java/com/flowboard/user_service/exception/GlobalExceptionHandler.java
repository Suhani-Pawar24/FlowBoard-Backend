package com.flowboard.user_service.exception;

import com.flowboard.user_service.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ Runtime Exception
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(ex.getMessage(), null));
    }

    // ✅ Database Unique Constraint Violation
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.contains("username")) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("Username already taken. Please choose another.", null));
        } else if (msg != null && msg.contains("email")) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("An account with this email already exists.", null));
        }
        return ResponseEntity.badRequest().body(new ApiResponse<>("A duplicate entry was detected. Please check your details.", null));
    }

    // ✅ Validation Exception
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(MethodArgumentNotValidException ex) {

        String error = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");

        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(error, null));
    }

    // ✅ Access Denied (ROLE BASED)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDenied() {
        return ResponseEntity.status(403)
                .body(new ApiResponse<>("Access Denied", null));
    }
}