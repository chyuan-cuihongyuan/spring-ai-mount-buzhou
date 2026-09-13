package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionLeaseStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-675 / spec 922：SessionLeaseStore 契约套件接入示例——内存实现过九项
 * 契约检查（第三方 store 自证价值主张实证，spec 732/744 接入先例同款）。
 */
class LeaseContractAccessTest {

    @Test
    void inMemoryLeaseStorePassesAllNineChecks() {
        SessionLeaseStoreContract.ContractReport report =
                SessionLeaseStoreContract.verify(new InMemorySessionLeaseStore());

        assertThat(report.allPassed())
                .as(() -> "未过项：" + report.checks().stream()
                        .filter(c -> !c.passed()).map(SessionLeaseStoreContract.CheckResult::name)
                        .toList())
                .isTrue();
        assertThat(report.total()).isEqualTo(9);
        assertThat(report.passed()).isEqualTo(9);
    }
}
