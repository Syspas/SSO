package com.example.sso;

import com.example.sso.config.DataInitializer;
import com.example.sso.user.entity.SsoUser;
import com.example.sso.user.repository.SsoUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Контракт сидов SSO с порталом: тот же plaintext-пароль и ФИО.
 */
@SpringBootTest
@DisplayName("Контракт сидов SSO")
class SsoSeedContractTest {

    @Autowired
    private SsoUserRepository users;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Aligned seeds: email, ФИО и пароль PortalSeed9!Change")
    void alignedSeedsMatchPortalContract() {
        assertThat(DataInitializer.ALIGNED_SEEDS).isNotEmpty();
        assertThat(DataInitializer.DEMO_PASSWORD).isEqualTo("PortalSeed9!Change");

        for (DataInitializer.AlignedSeed expected : DataInitializer.ALIGNED_SEEDS) {
            SsoUser user = users.findByEmailIgnoreCase(expected.email()).orElseThrow();
            assertThat(user.getLastName()).as(expected.email() + " lastName")
                    .isEqualTo(expected.lastName());
            assertThat(user.getFirstName()).as(expected.email() + " firstName")
                    .isEqualTo(expected.firstName());
            assertThat(normalize(user.getMiddleName())).as(expected.email() + " middleName")
                    .isEqualTo(normalize(expected.middleName()));
            assertThat(passwordEncoder.matches(DataInitializer.DEMO_PASSWORD, user.getPasswordHash()))
                    .as(expected.email() + " password")
                    .isTrue();
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
