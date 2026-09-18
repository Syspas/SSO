package com.example.sso.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.io.IOException;

/**
 * После формы логина: сохранённый {@code /authorize} важнее;
 * иначе редирект на портал ({@code sso.post-login-redirect}), а не на {@code /sso/}.
 */
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private static final String AUTHORIZE_PATH = "/authorize";

    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
    private final String postLoginRedirect;

    public LoginSuccessHandler(String postLoginRedirect) {
        this.postLoginRedirect = postLoginRedirect;
        setDefaultTargetUrl(postLoginRedirect);
        setAlwaysUseDefaultTargetUrl(false);
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws ServletException, IOException {
        SavedRequest saved = requestCache.getRequest(request, response);
        String savedUrl = saved == null ? null : saved.getRedirectUrl();
        boolean authorizeSaved = savedUrl != null && savedUrl.contains(AUTHORIZE_PATH);
        if (!authorizeSaved && saved != null) {
            requestCache.removeRequest(request, response);
        }
        if (authorizeSaved) {
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        getRedirectStrategy().sendRedirect(request, response, postLoginRedirect);
    }
}
