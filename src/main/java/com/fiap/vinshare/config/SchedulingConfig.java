package com.fiap.vinshare.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita @Scheduled no contexto. Cada service decide se executa seus jobs
 * via @ConditionalOnProperty próprio (notifications, retention, etc.), então
 * deixar o EnableScheduling sempre ativo não tem efeito colateral.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
