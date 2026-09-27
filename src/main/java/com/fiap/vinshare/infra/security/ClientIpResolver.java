package com.fiap.vinshare.infra.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolve o IP real do cliente. Só confia no X-Forwarded-For quando há
 * proxies conhecidos na frente da API (Azure App Service = 1 salto). Usa a
 * entrada adicionada pelo proxy mais próximo (lida da direita para a
 * esquerda); valores que o cliente injeta à esquerda são ignorados.
 */
@Slf4j
@Component
public class ClientIpResolver {

    /** Variável que o Azure App Service define em todo app. */
    static final String APP_SERVICE_MARKER = "WEBSITE_SITE_NAME";

    private final int trustedProxyHops;

    @Autowired
    public ClientIpResolver(@Value("${security.client-ip.trusted-proxy-hops:0}") int trustedProxyHops) {
        this(trustedProxyHops, System.getenv());
    }

    /**
     * Sem configuração (0) e rodando no App Service, confia em 1 salto: lá o
     * remoteAddr é o front end do Azure, e usá-lo faria o rate limit e o bloqueio
     * por IP valerem para todos os clientes juntos.
     */
    ClientIpResolver(int configuredHops, Map<String, String> env) {
        if (configuredHops == 0 && env.containsKey(APP_SERVICE_MARKER)) {
            log.info("Azure App Service detectado: IP do cliente lido do X-Forwarded-For (1 proxy)");
            this.trustedProxyHops = 1;
        } else {
            this.trustedProxyHops = configuredHops;
        }
    }

    public String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (trustedProxyHops <= 0) return remote;
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) return remote;
        String[] hops = xff.split(",");
        int index = Math.max(0, hops.length - trustedProxyHops);
        return stripPort(hops[index].trim());
    }

    /** Remove a porta: "203.0.113.7:51234" e "[2001:db8::1]:443". IPv6 sem colchetes fica intacto. */
    static String stripPort(String value) {
        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            return end > 0 ? value.substring(1, end) : value;
        }
        int colon = value.indexOf(':');
        if (colon > 0 && colon == value.lastIndexOf(':')) {
            return value.substring(0, colon);
        }
        return value;
    }
}
