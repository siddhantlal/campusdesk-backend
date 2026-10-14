package dev.campusdesk.common;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.ErrorResponse;

@RestControllerAdvice
public class ExceptionHandlerAdvice {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandlerAdvice.class);
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> api(ApiException ex, HttpServletRequest req) {
        return response(ex.status(), ex.code(), ex.getMessage(), req, Map.of());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String,String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> fields.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return response(400, "VALIDATION_ERROR", "Request validation failed", req, fields);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ProblemDetail> malformed(Exception ex, HttpServletRequest req) {
        return response(400, "INVALID_REQUEST", "Malformed JSON, unknown field, or invalid parameter/enum", req, Map.of());
    }
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, jakarta.persistence.OptimisticLockException.class})
    ResponseEntity<ProblemDetail> optimistic(Exception ex, HttpServletRequest req) { return api(ApiException.conflict(), req); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integrity(Exception ex, HttpServletRequest req) {
        return response(409, "DATA_CONFLICT", "A unique value or data constraint conflicts with this request", req, Map.of());
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> denied(Exception ex, HttpServletRequest req) {
        return response(403, "FORBIDDEN", "You do not have permission for this operation", req, Map.of());
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> other(Exception ex, HttpServletRequest req) {
        if (ex instanceof ErrorResponse error)
            return response(error.getStatusCode().value(), "HTTP_ERROR", "Request cannot be processed", req, Map.of());
        // Log only type and correlation ID, never request bodies or SQL exception text.
        LOG.error("Unhandled {} correlationId={}", ex.getClass().getSimpleName(), req.getAttribute("correlationId"));
        return response(500, "INTERNAL_ERROR", "An unexpected error occurred", req, Map.of());
    }
    private ResponseEntity<ProblemDetail> response(int status, String code, String detail, HttpServletRequest req, Map<String,String> fields) {
        return ResponseEntity.status(status).body(Problems.create(status, code, detail, req, fields));
    }
}
