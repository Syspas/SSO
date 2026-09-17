package com.example.sso.sso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ответ POST /token.
 */
public class TokenResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;

    public TokenResponse(String accessToken, String tokenType, long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    @JsonProperty("token_type")
    public String getTokenType() {
        return tokenType;
    }

    @JsonProperty("expires_in")
    public long getExpiresIn() {
        return expiresIn;
    }
}
