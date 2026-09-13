package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionLeaseStoreContract;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-682 / spec 929：租约契约接入 H2/JDBC store——JdbcSessionLeaseStore 过
 * SessionLeaseStoreContract 九项检查（真实 SQL fence 语义自证；H2 无 Docker CI 口径，
 * spec 732/744 接入先例）。
 */
class H2LeaseContractTest {

    @Test
    void jdbcLeaseStorePassesAllNineChecks() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:lease-contract-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        var recoveryStores = JdbcBuzhouStores.createWithRecovery(dataSource, Dialect.H2);

        SessionLeaseStoreContract.ContractReport report =
                SessionLeaseStoreContract.verify(recoveryStores.sessionLeaseStore());

        assertThat(report.allPassed())
                .as(() -> "未过项：" + report.checks().stream()
                        .filter(c -> !c.passed()).map(SessionLeaseStoreContract.CheckResult::name)
                        .toList())
                .isTrue();
        assertThat(report.total()).isEqualTo(9);
        assertThat(report.passed()).isEqualTo(9);
    }
}
