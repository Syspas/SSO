package com.example.sso.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Единый обработчик API-ошибок SSO.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SsoBadRequestException.class)
    public ProblemDetail handleBadRequest(SsoBadRequestException ex) {
        log.warn("SSO bad request: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(SsoUnauthorizedException.class)
    public ProblemDetail handleUnauthorized(SsoUnauthorizedException ex) {
        log.warn("SSO unauthorized: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ProblemDetail handleValidation(Exception ex) {
        log.warn("SSO validation failed: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleOther(Exception ex) {
        log.error("SSO unexpected error", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal SSO error");
    }
}
