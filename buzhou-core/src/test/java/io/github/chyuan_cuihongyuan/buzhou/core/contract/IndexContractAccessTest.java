package io.github.chyuan_cuihongyuan.buzhou.core.contract;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStoreContract;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-694 / spec 945：SessionIndexStore 契约套件接入示例——内存实现过五项
 * 契约检查（spec 732/922/936 接入先例同款）。
 */
class IndexContractAccessTest {

    @Test
    void inMemoryIndexStorePassesAllFiveChecks() {
        SessionIndexStoreContract.ContractReport report =
                SessionIndexStoreContract.verify(new InMemorySessionIndexStore());

        assertThat(report.allPassed())
                .as(() -> "未过项：" + report.checks().stream()
                        .filter(c -> !c.passed()).map(SessionIndexStoreContract.CheckResult::name)
                        .toList())
                .isTrue();
        assertThat(report.total()).isEqualTo(5);
        assertThat(report.passed()).isEqualTo(5);
    }
}
