package com.example.sso.sso.service;

import com.example.sso.exception.SsoUnauthorizedException;
import com.example.sso.sso.dto.UserInfoResponse;
import com.example.sso.sso.mapper.UserInfoMapper;
import com.example.sso.user.entity.SsoUser;
import com.example.sso.user.repository.SsoUserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserInfoService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final SsoUserRepository users;
    private final UserInfoMapper mapper;

    public UserInfoService(JwtService jwtService, SsoUserRepository users, UserInfoMapper mapper) {
        this.jwtService = jwtService;
        this.users = users;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public UserInfoResponse fromAuthorizationHeader(String authorization) {
        String token = extractBearer(authorization);
        Claims claims = jwtService.parseAndValidate(token);
        String email = claims.getSubject();
        SsoUser user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new SsoUnauthorizedException("User not found"));
        return mapper.toResponse(user);
    }

    private static String extractBearer(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new SsoUnauthorizedException("Bearer token required");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
