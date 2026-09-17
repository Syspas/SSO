package com.example.sso;

import com.example.sso.config.SsoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Точка входа SSO — отдельное приложение, не часть webapp и не часть excel.
 *
 * <p><b>Джун:</b> портал и Excel остаются своими JAR. Сюда ходят за логином
 * и коротким JWT. Сессию SSO держит cookie {@code SSOSESSIONID}.
 */
@SpringBootApplication
@EnableConfigurationProperties(SsoProperties.class)
public class SsoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsoApplication.class, args);
    }
}
