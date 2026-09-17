package com.example.sso.user.service;

import com.example.sso.user.entity.SsoUser;
import com.example.sso.user.repository.SsoUserRepository;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Подключает {@link SsoUser} к Spring Security form-login.
 *
 * <p>Username в Security = email (как в портале).
 */
@Service
public class SsoUserDetailsService implements UserDetailsService {

    private final SsoUserRepository users;

    public SsoUserDetailsService(SsoUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SsoUser user = users.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("User disabled: " + username);
        }
        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(AuthorityUtils.commaSeparatedStringToAuthorityList(user.getRoles()))
                .build();
    }
}
