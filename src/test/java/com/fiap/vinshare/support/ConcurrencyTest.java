package com.fiap.vinshare.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/** Base dos testes de concorrência: disparam mais requisições simultâneas que conexões no pool. */
public abstract class ConcurrencyTest extends IntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Value("${spring.datasource.hikari.maximum-pool-size}")
    protected int poolSize;

    /**
     * Abre todas as conexões do pool e devolve, para que as threads do teste as
     * peguem de uma vez, como num pico de tráfego (senão o pool cresce aos poucos).
     */
    protected void warmUpPool() throws Exception {
        List<Connection> held = new ArrayList<>();
        try {
            for (int i = 0; i < poolSize; i++) held.add(dataSource.getConnection());
        } finally {
            for (Connection c : held) c.close();
        }
    }
}
