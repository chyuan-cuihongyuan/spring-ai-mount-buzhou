package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1135 / impl 873：压缩检查点操作读面——保存（saves）、回滚（rollbacks）、
 * resetForTest 归零。
 */
class CheckpointStatsTest {

    private SessionStateStore stateStore;

    @BeforeEach
    void reset() {
        CompactionCheckpoints.resetForTest();
        stateStore = new InMemorySessionStateStore();
    }

    @Test
    void saveAndRollbackCountTheirBuckets() {
        CompactionCheckpoints checkpoints = new CompactionCheckpoints(stateStore);
        checkpoints.save("s1", 1, List.of());
        assertThat(checkpoints.rollback("s1", CompactionCheckpoints.RollbackLevel.MESSAGES_ONLY)).isTrue();

        CompactionCheckpoints.CheckpointStats stats = CompactionCheckpoints.stats();
        assertThat(stats.saves()).isEqualTo(1);
        assertThat(stats.rollbacks()).isEqualTo(1);
    }

    @Test
    void rollbackWithoutCheckpointNotCounted() {
        CompactionCheckpoints checkpoints = new CompactionCheckpoints(stateStore);
        // 无检查点 → rollback 返回 false 且不计数（保存是回滚的前提）
        assertThat(checkpoints.rollback("ghost", CompactionCheckpoints.RollbackLevel.MESSAGES_ONLY)).isFalse();

        CompactionCheckpoints.CheckpointStats stats = CompactionCheckpoints.stats();
        assertThat(stats.rollbacks()).isZero();
    }

    @Test
    void resetForTestZeroesCounters() {
        CompactionCheckpoints checkpoints = new CompactionCheckpoints(stateStore);
        checkpoints.save("s1", 1, List.of());
        checkpoints.rollback("s1", CompactionCheckpoints.RollbackLevel.MESSAGES_ONLY);
        assertThat(CompactionCheckpoints.stats().saves()).isEqualTo(1);

        CompactionCheckpoints.resetForTest();

        CompactionCheckpoints.CheckpointStats stats = CompactionCheckpoints.stats();
        assertThat(stats.saves()).isZero();
        assertThat(stats.rollbacks()).isZero();
    }
}
