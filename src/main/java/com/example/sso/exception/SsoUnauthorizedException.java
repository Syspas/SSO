package com.example.sso.exception;

/** Нет или битый Bearer JWT / нет сессии. */
public class SsoUnauthorizedException extends RuntimeException {

    public SsoUnauthorizedException(String message) {
        super(message);
    }
}
