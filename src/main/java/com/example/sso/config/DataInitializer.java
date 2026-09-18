package com.example.sso.config;

import com.example.sso.user.entity.SsoUser;
import com.example.sso.user.repository.SsoUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Сид демо-пользователей, выровненный с порталом (webapp DataInitializer).
 *
 * <p>Одинаковые email, ФИО и plaintext-пароль {@link #DEMO_PASSWORD}.
 * Upsert: при старте обновляет ФИО и пароль существующих строк, чтобы
 * старые сиды с паролем {@code admin} не остались в БД.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Тот же plaintext, что {@code DataInitializer.DEFAULT_PASSWORD} в webapp. */
    public static final String DEMO_PASSWORD = "PortalSeed9!Change";

    /**
     * Контракт с порталом. Меняешь — обнови webapp SSO_ALIGNED_SEEDS.
     */
    public static final List<AlignedSeed> ALIGNED_SEEDS = List.of(
            new AlignedSeed("admin@mail.ru", "Иванов", "Иван", "Иванович", "ROLE_ADMIN,ROLE_USER"),
            new AlignedSeed("mod@mail.ru", "Петров", "Пётр", null, "ROLE_USER"),
            new AlignedSeed("user@mail.ru", "Сидоров", "Сидор", "Сидорович", "ROLE_USER"),
            new AlignedSeed("admin@local", "Локальный", "Админ", null, "ROLE_ADMIN,ROLE_USER"),
            new AlignedSeed("user@local", "Локальный", "Пользователь", null, "ROLE_USER")
    );

    private final SsoUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SsoUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String passwordHash = passwordEncoder.encode(DEMO_PASSWORD);
        for (AlignedSeed seed : ALIGNED_SEEDS) {
            upsert(seed, passwordHash);
        }
        log.info("SSO aligned users ready (password same as portal seed): {}",
                ALIGNED_SEEDS.stream().map(AlignedSeed::email).toList());
    }

    private void upsert(AlignedSeed seed, String passwordHash) {
        SsoUser user = users.findByEmailIgnoreCase(seed.email()).orElseGet(SsoUser::new);
        user.setEmail(seed.email());
        user.setLastName(seed.lastName());
        user.setFirstName(seed.firstName());
        user.setMiddleName(seed.middleName());
        user.setPasswordHash(passwordHash);
        user.setRoles(seed.roles());
        user.setEnabled(true);
        users.save(user);
    }

    /**
     * @param email      логин
     * @param lastName   фамилия
     * @param firstName  имя
     * @param middleName отчество или null
     * @param roles      роли SSO (портал их из JWT не применяет)
     */
    public record AlignedSeed(
            String email, String lastName, String firstName, String middleName, String roles) {
    }
}
