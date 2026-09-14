package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1420 / T2142：评估运行年龄台账——Registry begin/close 埋点接线
 * （Registration 幂等 close 不重复计）、最老活跃年龄哨兵、最长完成水位、
 * reset 归零；静态面测试前后归零防串扰。
 */
class EvalRunAgeLedgerTest {

    private EvalRunRegistry registry;

    @BeforeEach
    void reset() {
        EvalRunAgeLedger.resetForTest();
        registry = EvalRunRegistry.create();
    }

    @AfterEach
    void resetAfter() {
        EvalRunAgeLedger.resetForTest();
    }

    @Test
    void emptyLedgerSentinels() {
        EvalRunAgeLedger.Snapshot s = EvalRunAgeLedger.stats();
        assertThat(s.active()).isZero();
        assertThat(s.oldestActiveAgeMillis()).isEqualTo(-1);
        assertThat(s.maxCompletedDurationMillis()).isZero();
        assertThat(s.closed()).isZero();
    }

    @Test
    void beginCloseCycleFeedsLedger() {
        try (EvalRunRegistry.Registration reg = registry.begin("eval", "run-1")) {
            assertThat(EvalRunAgeLedger.stats().active()).isEqualTo(1);
            // 活跃年龄 ≥ 0（刚开即查）
            assertThat(EvalRunAgeLedger.stats().oldestActiveAgeMillis())
                    .isGreaterThanOrEqualTo(0);
        }
        EvalRunAgeLedger.Snapshot s = EvalRunAgeLedger.stats();
        assertThat(s.active()).isZero();
        assertThat(s.closed()).isEqualTo(1);
        assertThat(s.oldestActiveAgeMillis()).isEqualTo(-1); // 无活跃回哨兵
        assertThat(s.maxCompletedDurationMillis()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void idempotentCloseCountsOnce() {
        EvalRunRegistry.Registration reg = registry.begin("eval", "run-x");
        reg.close();
        reg.close(); // 幂等 close——台账不重复计
        assertThat(EvalRunAgeLedger.stats().closed()).isEqualTo(1);
    }

    @Test
    void multipleActiveRunsReportOldestAge() throws Exception {
        try (EvalRunRegistry.Registration first = registry.begin("eval", "run-old")) {
            Thread.sleep(30);
            try (EvalRunRegistry.Registration second = registry.begin("ab", "run-new")) {
                long age = EvalRunAgeLedger.stats().oldestActiveAgeMillis();
                // 最老活跃=先开启者（跨 kind 汇总）
                assertThat(age).isGreaterThanOrEqualTo(30);
                assertThat(EvalRunAgeLedger.stats().active()).isEqualTo(2);
            }
        }
    }

    @Test
    void maxCompletedWatermarkIsMonotonic() {
        // 直调埋点缝：两次完成，水位取最大（不回退）
        long id1 = 910_001L;
        long id2 = 910_002L;
        EvalRunAgeLedger.recordOpened(id1);
        EvalRunAgeLedger.recordOpened(id2);
        EvalRunAgeLedger.recordClosed(id1);
        EvalRunAgeLedger.recordClosed(id2);
        long afterTwo = EvalRunAgeLedger.stats().maxCompletedDurationMillis();
        EvalRunAgeLedger.recordOpened(910_003L);
        EvalRunAgeLedger.recordClosed(910_003L); // 更短的第三次——水位不回退
        assertThat(EvalRunAgeLedger.stats().maxCompletedDurationMillis())
                .isGreaterThanOrEqualTo(afterTwo);
    }
}
