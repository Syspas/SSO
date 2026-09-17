package com.example.sso.sso.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Тело POST /token: обмен authorization code на JWT.
 */
public class TokenRequest {

    @NotBlank
    @JsonProperty("grant_type")
    private String grantType = "authorization_code";

    @NotBlank
    private String code;

    @NotBlank
    @JsonProperty("client_id")
    private String clientId;

    @NotBlank
    @JsonProperty("client_secret")
    private String clientSecret;

    @NotBlank
    @JsonProperty("redirect_uri")
    private String redirectUri;

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
