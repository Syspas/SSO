package com.example.sso.sso.web;

import com.example.sso.sso.dto.TokenForm;
import com.example.sso.sso.dto.TokenRequest;
import com.example.sso.sso.dto.TokenResponse;
import com.example.sso.sso.dto.UserInfoResponse;
import com.example.sso.sso.mapper.TokenFormMapper;
import com.example.sso.sso.service.AuthorizationService;
import com.example.sso.sso.service.TokenService;
import com.example.sso.sso.service.UserInfoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Упрощённый OAuth-подобный протокол SSO.
 *
 * <p>Context-path = {@code /sso}, снаружи URL: {@code /sso/authorize} и т.д.
 */
@RestController
public class SsoController {

    private final AuthorizationService authorizationService;
    private final TokenService tokenService;
    private final UserInfoService userInfoService;
    private final TokenFormMapper tokenFormMapper;

    public SsoController(
            AuthorizationService authorizationService,
            TokenService tokenService,
            UserInfoService userInfoService,
            TokenFormMapper tokenFormMapper) {
        this.authorizationService = authorizationService;
        this.tokenService = tokenService;
        this.userInfoService = userInfoService;
        this.tokenFormMapper = tokenFormMapper;
    }

    @GetMapping("/authorize")
    public RedirectView authorize(
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "state", required = false) String state,
            Authentication authentication) {
        String location = authorizationService.buildRedirect(clientId, redirectUri, state, authentication);
        return new RedirectView(location);
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        return tokenService.exchange(request);
    }

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public TokenResponse tokenForm(@Valid @ModelAttribute TokenForm form) {
        return tokenService.exchange(tokenFormMapper.toRequest(form));
    }

    @GetMapping("/userinfo")
    public UserInfoResponse userinfo(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return userInfoService.fromAuthorizationHeader(authorization);
    }
}
