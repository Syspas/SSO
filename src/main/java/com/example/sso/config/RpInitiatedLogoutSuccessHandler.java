package com.example.sso.config;

import com.example.sso.client.ClientRegistry;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import java.io.IOException;

/**
 * После уничтожения cookie SSO редирект только на зарегистрированный
 * {@code post_logout_redirect_uri} клиента.
 */
public class RpInitiatedLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    static final String CLIENT_ID_PARAM = "client_id";
    static final String POST_LOGOUT_REDIRECT_PARAM = "post_logout_redirect_uri";
    static final String DEFAULT_LOGOUT_URL = "/login?logout";

    private final ClientRegistry clientRegistry;

    public RpInitiatedLogoutSuccessHandler(ClientRegistry clientRegistry) {
        this.clientRegistry = clientRegistry;
        setDefaultTargetUrl(DEFAULT_LOGOUT_URL);
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        String clientId = request.getParameter(CLIENT_ID_PARAM);
        String postLogout = request.getParameter(POST_LOGOUT_REDIRECT_PARAM);
        if (clientRegistry.isAllowedPostLogoutRedirect(clientId, postLogout)) {
            getRedirectStrategy().sendRedirect(request, response, postLogout);
            return;
        }
        super.onLogoutSuccess(request, response, authentication);
    }
}
