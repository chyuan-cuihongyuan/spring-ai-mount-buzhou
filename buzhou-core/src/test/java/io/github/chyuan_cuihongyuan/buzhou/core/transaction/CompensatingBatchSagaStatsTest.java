package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1443 / T2184 兄弟票：saga 运行静态读数——漏斗守恒（runs=successes+
 * compensationRuns）、步数计量、断点步名、reset 归零；静态面前后归零防串扰。
 */
class CompensatingBatchSagaStatsTest {

    private final io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryUnitOfWork uow =
            new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryUnitOfWork();

    @BeforeEach
    void reset() {
        CompensatingBatch.resetSagaStatsForTest();
    }

    @AfterEach
    void resetAfter() {
        CompensatingBatch.resetSagaStatsForTest();
    }

    @Test
    void successfulRunCountsStepsAndSuccess() {
        CompensatingBatch.run(uow, List.of(
                CompensatingBatch.Step.of("step-a", () -> 1, r -> {
                }),
                CompensatingBatch.Step.of("step-b", () -> 2, r -> {
                })));
        var s = CompensatingBatch.sagaStats();
        assertThat(s.runs()).isEqualTo(1);
        assertThat(s.successes()).isEqualTo(1);
        assertThat(s.stepsExecuted()).isEqualTo(2);
        assertThat(s.compensationRuns()).isZero();
        assertThat(s.conserved()).isTrue();
        assertThat(s.lastFailedStep()).isNull();
    }

    @Test
    void failedRunRecordsCompensationAndBreakpointStep() {
        // 三步：第三步抛异常——前两步补偿回退、断点步名记录
        assertThatThrownBy(() -> CompensatingBatch.run(uow, List.of(
                CompensatingBatch.Step.of("step-a", () -> 1, r -> {
                }),
                CompensatingBatch.Step.of("step-b", () -> 2, r -> {
                }),
                CompensatingBatch.Step.of("step-c", () -> {
                    throw new IllegalStateException("第三步爆炸");
                }, r -> {
                }))))
                .isInstanceOf(IllegalStateException.class);
        var s = CompensatingBatch.sagaStats();
        assertThat(s.runs()).isEqualTo(1);
        assertThat(s.successes()).isZero();
        assertThat(s.compensationRuns()).isEqualTo(1);
        assertThat(s.conserved()).isTrue();
        assertThat(s.lastFailedStep()).isEqualTo("step-c");
    }

    @Test
    void compensationFailureBucketsSepearately() {
        // 补偿自身失败：compensationFailures 计数（停止回退断点语义既有）
        assertThatThrownBy(() -> CompensatingBatch.run(uow, List.of(
                CompensatingBatch.Step.of("bad-comp", () -> 1, r -> {
                    throw new IllegalStateException("补偿失败");
                }),
                CompensatingBatch.Step.of("boom", () -> {
                    throw new IllegalStateException("正向爆炸");
                }, r -> {
                }))))
                .isInstanceOf(IllegalStateException.class);
        var s = CompensatingBatch.sagaStats();
        assertThat(s.runs()).isEqualTo(1);
        assertThat(s.compensationRuns()).isEqualTo(1);
        assertThat(s.compensationFailures()).isEqualTo(1);
        assertThat(s.lastFailedStep()).isEqualTo("boom");
    }

    @Test
    void resetForTestClearsAll() {
        CompensatingBatch.run(uow, List.of(CompensatingBatch.Step.of("s", () -> 1, r -> {
        })));
        assertThat(CompensatingBatch.sagaStats().runs()).isEqualTo(1);
        CompensatingBatch.resetSagaStatsForTest();
        var s = CompensatingBatch.sagaStats();
        assertThat(s.runs()).isZero();
        assertThat(s.lastFailedStep()).isNull();
    }
}
