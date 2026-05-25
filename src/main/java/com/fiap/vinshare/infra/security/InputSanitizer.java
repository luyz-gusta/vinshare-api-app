package com.fiap.vinshare.infra.security;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Sanitização de XSS para campos texto livre.
 * Atende Cyber Frente 1 (sanitização contra XSS).
 */
@Component
public class InputSanitizer {

    public String sanitize(String input) {
        if (input == null) return null;
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return trimmed;
        return HtmlUtils.htmlEscape(trimmed);
    }
}
