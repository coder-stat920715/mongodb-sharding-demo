package com.souptik.shardingdemo.controller;

import com.souptik.shardingdemo.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex,
                                                         HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Missing required parameter: " + ex.getParameterName(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, message, req);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiError> handleDuplicateKey(DuplicateKeyException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Duplicate orderId", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req) {
        ApiError error = ApiError.of(status.value(), status.getReasonPhrase(), message, req.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}
