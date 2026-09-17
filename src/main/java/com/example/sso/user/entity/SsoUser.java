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
 * отдельным приложением. Синхронизация с порталом — отдельная задача.
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

    /** Имя для UI. */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * Роли через запятую, например {@code ROLE_ADMIN,ROLE_USER}.
     * Простой формат для v1 без join-таблицы.
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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
