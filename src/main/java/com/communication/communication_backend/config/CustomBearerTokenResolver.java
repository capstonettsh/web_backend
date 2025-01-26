package com.communication.communication_backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

public class CustomBearerTokenResolver implements BearerTokenResolver {
    private static final String ACCESS_TOKEN_PARAM = "access_token";
    private final BearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        // Attempt to resolve token from Authorization header
        String token = defaultResolver.resolve(request);
        if (token == null) {
            // Fallback to resolving token from query parameter
            token = request.getParameter(ACCESS_TOKEN_PARAM);
        }
        return token;
    }
}
