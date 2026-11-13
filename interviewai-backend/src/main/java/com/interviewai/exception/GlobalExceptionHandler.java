package com.interviewai.exception;

import com.interviewai.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  ResponseEntity<ApiResponse<Void>> tooLarge(Exception e) {
    return error(HttpStatus.PAYLOAD_TOO_LARGE, "PDF must not exceed 5 MB");
  }

  @ExceptionHandler({
    org.springframework.web.multipart.support.MissingServletRequestPartException.class,
    org.springframework.web.bind.MissingServletRequestParameterException.class,
    org.springframework.web.method.annotation.HandlerMethodValidationException.class
  })
  ResponseEntity<ApiResponse<Void>> invalidInput(Exception e) {
    return error(HttpStatus.BAD_REQUEST, "Invalid or missing request parameters");
  }

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiResponse<Void>> api(
      ApiException e, jakarta.servlet.http.HttpServletResponse response) {
    if (response.isCommitted()) return null;
    return error(e.status(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .distinct()
            .sorted()
            .collect(java.util.stream.Collectors.joining("; "));
    return error(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ApiResponse<Void>> malformed(Exception e) {
    return error(HttpStatus.BAD_REQUEST, "Invalid request");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiResponse<Void>> conflict(Exception e) {
    return error(HttpStatus.CONFLICT, "The operation conflicts with existing data");
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ApiResponse<Void>> forbidden(Exception e) {
    return error(HttpStatus.FORBIDDEN, "Access denied");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiResponse<Void>> missing(Exception e) {
    return error(HttpStatus.NOT_FOUND, "Resource not found");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiResponse<Void>> method(Exception e) {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Void>> unexpected(
      Exception e, jakarta.servlet.http.HttpServletResponse response) {
    if (response.isCommitted()) return null;
    log.error("Unhandled API failure ({})", e.getClass().getSimpleName());
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed");
  }

  private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_JSON)
        .body(ApiResponse.error(message));
  }
}
