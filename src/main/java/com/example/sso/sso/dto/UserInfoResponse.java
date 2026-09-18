package com.example.sso.sso.dto;

import java.util.List;

/** Ответ GET /userinfo по Bearer JWT. ФИО стыкуется с порталом. */
public class UserInfoResponse {

    private final String email;
    private final String name;
    private final String lastName;
    private final String firstName;
    private final String middleName;
    private final List<String> roles;

    public UserInfoResponse(
            String email,
            String name,
            String lastName,
            String firstName,
            String middleName,
            List<String> roles) {
        this.email = email;
        this.name = name;
        this.lastName = lastName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.roles = roles;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public List<String> getRoles() {
        return roles;
    }
}
