package br.com.belloinfo.saap_mvp.infrastructure.security;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String getAuthenticatedUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous@saap.com";
    }
}
