package io.github.chyuan_cuihongyuan.buzhou.core.contract;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.InMemoryLeaderElector;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElectorContract;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-699 续 / spec 954：LeaderElector 契约套件接入示例——内存实现过五项
 * 契约检查（spec 732/922/936 接入先例同款）。
 */
class LeaderContractAccessTest {

    @Test
    void inMemoryLeaderElectorPassesAllFiveChecks() {
        LeaderElectorContract.ContractReport report =
                LeaderElectorContract.verify(new InMemoryLeaderElector("contract-holder"));

        assertThat(report.allPassed())
                .as(() -> "未过项：" + report.checks().stream()
                        .filter(c -> !c.passed()).map(LeaderElectorContract.CheckResult::name)
                        .toList())
                .isTrue();
        assertThat(report.total()).isEqualTo(5);
        assertThat(report.passed()).isEqualTo(5);
    }
}
