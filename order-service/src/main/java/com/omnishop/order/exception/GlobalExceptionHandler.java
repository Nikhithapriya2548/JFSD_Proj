package com.omnishop.order.exception;

import com.omnishop.order.service.OrderServiceImpl.OrderNotFoundException;
import com.omnishop.order.service.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.client.ResourceAccessException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> body(HttpStatus status, String message, HttpServletRequest req) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("timestamp", Instant.now().toString());
        b.put("status", status.value());
        b.put("error", status.getReasonPhrase());
        b.put("message", message);
        b.put("path", req.getRequestURI());
        return b;
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Object> handleOrderNotFound(OrderNotFoundException ex, HttpServletRequest req) {
        return new ResponseEntity<>(body(HttpStatus.NOT_FOUND, ex.getMessage(), req), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Object> handleProductNotFound(ProductNotFoundException ex, HttpServletRequest req) {
        return new ResponseEntity<>(body(HttpStatus.NOT_FOUND, ex.getMessage(), req), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Object> handleInsufficientStock(InsufficientStockException ex, HttpServletRequest req) {
        return new ResponseEntity<>(body(HttpStatus.BAD_REQUEST, ex.getMessage(), req), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class,
            CouponService.InvalidCouponException.class})
    public ResponseEntity<Object> handleBadRequest(RuntimeException ex, HttpServletRequest req) {
        return new ResponseEntity<>(body(HttpStatus.BAD_REQUEST, ex.getMessage(), req), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, Object> b = body(HttpStatus.BAD_REQUEST, "Validation failed", req);
        b.put("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(e -> Map.of("field", e.getField(), "message",
                        e.getDefaultMessage() == null ? "invalid" : e.getDefaultMessage()))
                .collect(Collectors.toList()));
        return new ResponseEntity<>(b, HttpStatus.BAD_REQUEST);
    }

    // product-service down / unreachable → 503, order-service itself stays up
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Object> handleServiceUnavailable(ResourceAccessException ex, HttpServletRequest req) {
        return new ResponseEntity<>(
                body(HttpStatus.SERVICE_UNAVAILABLE, "Product service unavailable: " + ex.getMessage(), req),
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return new ResponseEntity<>(body(HttpStatus.NOT_FOUND, ex.getMessage(), req), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneral(Exception ex, HttpServletRequest req) {
        return new ResponseEntity<>(
                body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error: " + ex.getMessage(), req),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
