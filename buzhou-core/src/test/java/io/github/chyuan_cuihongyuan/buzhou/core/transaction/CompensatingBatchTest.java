package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.UnitOfWork;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 304 / impl-327：事务批补偿回归——全成不补偿 / 中途失败倒序补偿原异常
 * 上抛 / 补偿失败即停止回退 / 每步在事务内执行。
 */
class CompensatingBatchTest {

    /** 事务观察 uow：记录每次事务包裹的 supplier（断言步与补偿都在事务内）。 */
    static final class RecordingUow implements UnitOfWork {
        final List<String> txLog = new ArrayList<>();

        @Override
        public <T> T executeInTransaction(Supplier<T> work) {
            txLog.add("tx-begin");
            try {
                T result = work.get();
                txLog.add("tx-commit");
                return result;
            } catch (RuntimeException e) {
                txLog.add("tx-rollback");
                throw e;
            }
        }
    }

    @Test
    void allStepsSucceed_noCompensation() {
        RecordingUow uow = new RecordingUow();
        List<String> compensated = new ArrayList<>();
        String result = CompensatingBatch.run(uow, List.of(
                CompensatingBatch.Step.of("a", () -> "A", r -> compensated.add("a")),
                CompensatingBatch.Step.of("b", () -> "B", r -> compensated.add("b"))));

        assertThat(result).isEqualTo("B");
        assertThat(compensated).isEmpty();
        assertThat(uow.txLog).containsExactly("tx-begin", "tx-commit", "tx-begin", "tx-commit");
    }

    @Test
    void failureCompensatesCompletedStepsInReverse_andRethrows() {
        RecordingUow uow = new RecordingUow();
        List<String> compensated = new ArrayList<>();
        IllegalStateException boom = new IllegalStateException("step-c-boom");

        assertThatThrownBy(() -> CompensatingBatch.run(uow, List.of(
                CompensatingBatch.Step.of("a", () -> "A", (java.util.function.Consumer<String>) r -> compensated.add("a:" + r)),
                CompensatingBatch.Step.of("b", () -> "B", (java.util.function.Consumer<String>) r -> compensated.add("b:" + r)),
                CompensatingBatch.Step.of("c", () -> {
                    throw boom;
                }, r -> compensated.add("c")))))
                .isSameAs(boom);
        assertThat(compensated).containsExactly("b:B", "a:A"); // 倒序 + 补偿收本步结果
    }

    @Test
    void compensationFailureHaltsUnwindEarlierStepsUntouched() {
        List<String> compensated = new ArrayList<>();
        assertThatThrownBy(() -> CompensatingBatch.run(new RecordingUow(), List.of(
                CompensatingBatch.Step.of("a", () -> "A", r -> compensated.add("a")),
                CompensatingBatch.Step.of("b", () -> "B", r -> {
                    compensated.add("b");
                    throw new IllegalStateException("comp-b-fails");
                }),
                CompensatingBatch.Step.of("c", () -> {
                    throw new IllegalStateException("c-boom");
                }, r -> compensated.add("c")))))
                .hasMessage("c-boom");
        // b 补偿失败 → 停止回退：a 不再补偿（人工介入断点）
        assertThat(compensated).containsExactly("b");
    }

    @Test
    void nullCompensationStepIsSkippedInUnwind() {
        List<String> compensated = new ArrayList<>();
        assertThatThrownBy(() -> CompensatingBatch.run(new RecordingUow(), List.of(
                CompensatingBatch.Step.of("no-comp", () -> "x", null),
                CompensatingBatch.Step.of("tail", () -> {
                    throw new IllegalStateException("tail-boom");
                }, r -> compensated.add("tail")))))
                .hasMessage("tail-boom");
        // tail 未成功（其自身事务已回滚）→ 不补偿；no-comp 无补偿 → 跳过
        assertThat(compensated).isEmpty();
    }
}
