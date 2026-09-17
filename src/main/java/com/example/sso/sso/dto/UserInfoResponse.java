package com.example.sso.sso.dto;

import java.util.List;

/** Ответ GET /userinfo по Bearer JWT. */
public class UserInfoResponse {

    private final String email;
    private final String name;
    private final List<String> roles;

    public UserInfoResponse(String email, String name, List<String> roles) {
        this.email = email;
        this.name = name;
        this.roles = roles;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public List<String> getRoles() {
        return roles;
    }
}
