package com.slotbook.core.config;

import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Starts a throwaway Postgres with the app, so nobody needs to install one.
 * The database is empty on every start; Flyway migrations create the schema and demo data.
 */
@Configuration
@ConditionalOnProperty(name = "slotbook.db.mode", havingValue = "embedded", matchIfMissing = true)
public class EmbeddedPostgresConfig {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedPostgresConfig.class);

    @Bean(destroyMethod = "close")
    public EmbeddedPostgres embeddedPostgres() throws IOException {
        EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
        log.info("Embedded Postgres is running: {}", pg.getJdbcUrl("postgres", "postgres"));
        return pg;
    }

    @Bean
    public DataSource dataSource(EmbeddedPostgres pg) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(pg.getJdbcUrl("postgres", "postgres"));
        ds.setUsername("postgres");
        ds.setMaximumPoolSize(10);
        return ds;
    }
}
