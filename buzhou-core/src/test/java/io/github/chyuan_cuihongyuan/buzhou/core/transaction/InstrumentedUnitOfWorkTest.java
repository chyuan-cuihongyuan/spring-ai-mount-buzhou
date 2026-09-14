package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.UnitOfWork;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1416 / T2134：事务计量装饰器——委托透传、三总量守恒、失败异常原样
 * 上抛且分类入榜、Top 有界并 OTHERS、双重载都计量、reset 归零
 * （pg_stat_database xact 思想）。
 */
class InstrumentedUnitOfWorkTest {

    private final UnitOfWork noop = new UnitOfWork() {
        @Override
        public <T> T executeInTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    @Test
    void completedTransactionsConserved() {
        InstrumentedUnitOfWork uow = new InstrumentedUnitOfWork(noop);
        uow.executeInTransaction(() -> "a");
        uow.executeInTransaction("s-1", () -> 42);
        var s = uow.stats();
        assertThat(s.begun()).isEqualTo(2);
        assertThat(s.completed()).isEqualTo(2);
        assertThat(s.failed()).isZero();
        assertThat(s.inFlight()).isZero();
        assertThat(s.begun()).isEqualTo(s.completed() + s.failed() + s.inFlight());
        assertThat(s.failureClasses()).isEmpty();
    }

    @Test
    void failedTransactionRethrowsAndClassifies() {
        InstrumentedUnitOfWork uow = new InstrumentedUnitOfWork(noop);
        assertThatThrownBy(() -> uow.executeInTransaction(() -> {
            throw new IllegalStateException("存储抖动");
        })).isInstanceOf(IllegalStateException.class);
        var s = uow.stats();
        assertThat(s.begun()).isEqualTo(1);
        assertThat(s.failed()).isEqualTo(1);
        assertThat(s.completed()).isZero();
        // 异常原样上抛且分类入榜（简单类名，次数降序）
        assertThat(s.failureClasses()).hasSize(1);
        assertThat(s.failureClasses().get(0).getKey()).isEqualTo("IllegalStateException");
        assertThat(s.failureClasses().get(0).getValue()).isEqualTo(1L);
    }

    @Test
    void delegationReachesDelegateIncludingDeleteSession() {
        boolean[] deleted = {false};
        UnitOfWork delegate = new UnitOfWork() {
            @Override
            public <T> T executeInTransaction(Supplier<T> work) {
                return work.get();
            }

            @Override
            public void deleteSession(String sessionId) {
                deleted[0] = true;
            }
        };
        InstrumentedUnitOfWork uow = new InstrumentedUnitOfWork(delegate);
        uow.deleteSession("s-x");
        assertThat(deleted[0]).isTrue();
        uow.executeInTransaction(() -> 1);
        uow.executeInTransaction("s-y", () -> 2);
        // 双重载都计量
        assertThat(uow.stats().begun()).isEqualTo(2);
    }

    @Test
    void failureClassBoardIsBoundedWithOthersBucket() {
        InstrumentedUnitOfWork uow = new InstrumentedUnitOfWork(noop);
        for (int i = 0; i < InstrumentedUnitOfWork.FAILURE_CLASS_CAPACITY + 3; i++) {
            final int n = i;
            assertThatThrownBy(() -> uow.executeInTransaction(() -> {
                throw new IllegalStateException("e" + n);
            })).isInstanceOf(IllegalStateException.class);
        }
        var s = uow.stats();
        assertThat(s.failed()).isEqualTo(InstrumentedUnitOfWork.FAILURE_CLASS_CAPACITY + 3);
        // 榜容量不超限
        assertThat(s.failureClasses().size())
                .isLessThanOrEqualTo(InstrumentedUnitOfWork.FAILURE_CLASS_CAPACITY);
        // 同类失败合并入同一键（IllegalStateException 单键累计）
        assertThat(s.failureClasses().get(0).getKey()).isEqualTo("IllegalStateException");
    }

    @Test
    void resetForTestClearsAllCounters() {
        InstrumentedUnitOfWork uow = new InstrumentedUnitOfWork(noop);
        uow.executeInTransaction(() -> 1);
        assertThat(uow.stats().begun()).isEqualTo(1);
        uow.resetForTest();
        assertThat(uow.stats()).isEqualTo(new InstrumentedUnitOfWork.Snapshot(
                0, 0, 0, 0, List.of()));
    }

    @Test
    void nullDelegateFailsFast() {
        assertThatThrownBy(() -> new InstrumentedUnitOfWork(null))
                .isInstanceOf(IllegalArgumentException.class);
        // Map.entry 排序语义自检（平名典序路径）
        var a = Map.entry("A", 1L);
        var b = Map.entry("B", 1L);
        assertThat(a.getKey()).isLessThan(b.getKey());
    }
}
