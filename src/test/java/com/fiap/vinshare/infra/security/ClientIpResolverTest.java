package com.fiap.vinshare.infra.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private static MockHttpServletRequest request(String remoteAddr, String xff) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr(remoteAddr);
        if (xff != null) req.addHeader("X-Forwarded-For", xff);
        return req;
    }

    @Test
    void semProxyConfiavelIgnoraXForwardedFor() {
        ClientIpResolver resolver = new ClientIpResolver(0, Map.of());
        assertThat(resolver.resolve(request("198.51.100.9", "1.2.3.4"))).isEqualTo("198.51.100.9");
    }

    @Test
    void noAppServiceSemConfiguracaoUsaOIpQueOFrontEndDoAzureRepassa() {
        // Sem isso, o remoteAddr é o front end do Azure e o rate limit vale para todos os clientes juntos.
        ClientIpResolver resolver = new ClientIpResolver(0, Map.of("WEBSITE_SITE_NAME", "vinshare-api"));
        assertThat(resolver.resolve(request("169.254.129.3", "203.0.113.7:51234"))).isEqualTo("203.0.113.7");
    }

    @Test
    void configuracaoExplicitaPrevaleceSobreADeteccaoDoAppService() {
        ClientIpResolver resolver = new ClientIpResolver(2, Map.of("WEBSITE_SITE_NAME", "vinshare-api"));
        assertThat(resolver.resolve(request("169.254.129.3", "1.2.3.4, 203.0.113.7, 10.0.0.9"))).isEqualTo("203.0.113.7");
    }

    @Test
    void comUmProxyUsaAEntradaMaisADireitaERemovePorta() {
        ClientIpResolver resolver = new ClientIpResolver(1);
        assertThat(resolver.resolve(request("10.0.0.5", "1.2.3.4, 203.0.113.7:51234"))).isEqualTo("203.0.113.7");
    }

    @Test
    void entradaUnicaComPortaDoAzure() {
        ClientIpResolver resolver = new ClientIpResolver(1);
        assertThat(resolver.resolve(request("10.0.0.5", "203.0.113.7:51234"))).isEqualTo("203.0.113.7");
    }

    @Test
    void semHeaderUsaRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver(1);
        assertThat(resolver.resolve(request("10.0.0.5", null))).isEqualTo("10.0.0.5");
    }

    @Test
    void ipv6ComColchetesEPorta() {
        assertThat(ClientIpResolver.stripPort("[2001:db8::1]:443")).isEqualTo("2001:db8::1");
        assertThat(ClientIpResolver.stripPort("2001:db8::1")).isEqualTo("2001:db8::1");
    }
}
