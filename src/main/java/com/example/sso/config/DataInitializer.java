package com.example.sso.config;

import com.example.sso.user.entity.SsoUser;
import com.example.sso.user.repository.SsoUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Сид демо-пользователей для локального запуска.
 *
 * <p><b>TODO:</b> в проде либо синхронизация с users портала, либо общий IdP.
 * Сейчас своя таблица {@code sso_users}, чтобы не читать БД webapp напрямую.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String DEMO_PASSWORD = "admin";

    private final SsoUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SsoUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("admin@local", "Admin", "ROLE_ADMIN,ROLE_USER");
        seed("user@local", "User", "ROLE_USER");
        log.info("SSO demo users ready (password '{}'): admin@local, user@local", DEMO_PASSWORD);
    }

    private void seed(String email, String displayName, String roles) {
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }
        SsoUser user = new SsoUser();
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRoles(roles);
        user.setEnabled(true);
        users.save(user);
    }
}
