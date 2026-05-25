package com.fiap.vinshare.infra.security;

import com.fiap.vinshare.domain.entities.User;
import com.fiap.vinshare.infra.errors.exceptions.InvalidCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<User> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return Optional.empty();
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) return Optional.of(user);
        return Optional.empty();
    }

    public static User requireCurrentUser() {
        return currentUser().orElseThrow(() -> new InvalidCredentialsException("Não autenticado"));
    }
}
