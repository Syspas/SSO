package com.example.sso.exception;

/** Ошибка запроса SSO (неверный client_id, code, redirect_uri). */
public class SsoBadRequestException extends RuntimeException {

    public SsoBadRequestException(String message) {
        super(message);
    }
}
