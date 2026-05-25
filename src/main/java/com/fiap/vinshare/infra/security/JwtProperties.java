package com.fiap.vinshare.infra.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;

@Getter
@Configuration
public class JwtProperties {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.access-token-minutes}")
    private int accessTokenMinutes;

    @Value("${security.jwt.refresh-token-days}")
    private int refreshTokenDays;

    @Value("${security.jwt.issuer}")
    private String issuer;
}
