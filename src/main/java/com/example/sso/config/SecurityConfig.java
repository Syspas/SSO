package com.example.sso.config;

import com.example.sso.client.ClientRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

/**
 * Дверь SSO: форма логина, сессия cookie, публичные token/userinfo по Bearer.
 *
 * <p>{@code /token} не требует cookie — клиент шлёт code + secret.
 * {@code /authorize} требует сессию SSO. Context-path уже {@code /sso},
 * поэтому матчеры без префикса {@code /sso}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String CSP_POLICY =
            "default-src 'self'; style-src 'self' 'unsafe-inline'; "
                    + "script-src 'self'; form-action 'self'; frame-ancestors 'none'";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ClientRegistry clientRegistry) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/login", "/error").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/token").permitAll()
                        .requestMatchers(HttpMethod.GET, "/userinfo").permitAll()
                        .requestMatchers("/authorize").authenticated()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll())
                .logout(logout -> logout
                        .logoutRequestMatcher(new OrRequestMatcher(
                                new AntPathRequestMatcher("/logout", "GET"),
                                new AntPathRequestMatcher("/logout", "POST")))
                        .logoutSuccessHandler(new RpInitiatedLogoutSuccessHandler(clientRegistry))
                        .invalidateHttpSession(true)
                        .deleteCookies("SSOSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/token"))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_POLICY)))
                .httpBasic(basic -> basic.disable());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
