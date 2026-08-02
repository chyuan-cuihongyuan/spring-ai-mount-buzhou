package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

public final class JdbcBuzhouStores {

    private JdbcBuzhouStores() {
    }

    public static BuzhouStores create(DataSource dataSource, Dialect dialect) {
        try (var connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource(dialect.schemaResource()));
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException("Failed to initialize buzhou schema", e);
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        return new BuzhouStores(
                new JdbcMessageStore(jdbc),
                new JdbcSummaryStore(jdbc),
                new JdbcSessionStateStore(jdbc),
                new JdbcSessionLeaseStore(jdbc),
                new JdbcObservabilityStore(jdbc),
                new JdbcUnitOfWork(tx));
    }
}
