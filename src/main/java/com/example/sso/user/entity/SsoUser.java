package com.example.sso.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Пользователь SSO. Авторизация по email.
 *
 * <p>Это не сущность portal {@code User}: своя таблица, чтобы SSO был
 * отдельным приложением. Сиды email/ФИО/пароля должны совпадать с порталом;
 * runtime-синхронизация паролей не делается.
 */
@Entity
@Table(name = "sso_users")
public class SsoUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Уникальный email = логин. */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /** BCrypt-хэш пароля. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** Фамилия (стык с порталом). */
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    /** Имя (стык с порталом). */
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    /** Отчество (может отсутствовать). */
    @Column(name = "middle_name", length = 50)
    private String middleName;

    /**
     * Роли через запятую, например {@code ROLE_ADMIN,ROLE_USER}.
     * Простой формат для v1 без join-таблицы. Портал их из JWT не берёт.
     */
    @Column(nullable = false, length = 255)
    private String roles = "ROLE_USER";

    @Column(nullable = false)
    private boolean enabled = true;

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    /** Отображаемое ФИО для UI и JWT claim {@code name}. */
    public String displayName() {
        StringBuilder sb = new StringBuilder();
        if (lastName != null && !lastName.isBlank()) {
            sb.append(lastName.trim());
        }
        if (firstName != null && !firstName.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(firstName.trim());
        }
        if (middleName != null && !middleName.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(middleName.trim());
        }
        return sb.toString();
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
