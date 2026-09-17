package com.example.sso.sso.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Form-urlencoded вариант POST /token (удобно для curl).
 *
 * <p>Сеттеры {@code setGrant_type} и т.п. нужны, чтобы Spring 6.1
 * связал имена полей формы с Java-бином.
 */
public class TokenForm {

    @NotBlank
    private String grantType;

    @NotBlank
    private String code;

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    @NotBlank
    private String redirectUri;

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public void setGrant_type(String grantType) {
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

    public void setClient_id(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setClient_secret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public void setRedirect_uri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
