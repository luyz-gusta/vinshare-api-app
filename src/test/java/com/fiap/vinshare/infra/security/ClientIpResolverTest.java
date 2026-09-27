package com.fiap.vinshare.infra.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

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
        ClientIpResolver resolver = new ClientIpResolver(0);
        assertThat(resolver.resolve(request("198.51.100.9", "1.2.3.4"))).isEqualTo("198.51.100.9");
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
