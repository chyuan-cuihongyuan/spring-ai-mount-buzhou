package io.github.chyuan_cuihongyuan.buzhou.core.contract;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStoreContract;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-688 / spec 936：ObservabilityStore 契约套件接入示例——内存实现过八项
 * 契约检查（第三方 store 自证价值主张实证，spec 732/922 先例同款）。
 */
class ObsContractAccessTest {

    @Test
    void inMemoryObservabilityStorePassesAllEightChecks() {
        ObservabilityStoreContract.ContractReport report =
                ObservabilityStoreContract.verify(new InMemoryObservabilityStore());

        assertThat(report.allPassed())
                .as(() -> "未过项：" + report.checks().stream()
                        .filter(c -> !c.passed()).map(ObservabilityStoreContract.CheckResult::name)
                        .toList())
                .isTrue();
        assertThat(report.total()).isEqualTo(8);
        assertThat(report.passed()).isEqualTo(8);
    }
}
