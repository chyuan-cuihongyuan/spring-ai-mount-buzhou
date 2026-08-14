package io.github.chyuan_cuihongyuan.buzhou.memory.episodic;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-38 / spec 13 §growth-8：EpisodeLedger 序号从持久状态恢复——重启（新实例）
 * 不归零、不覆盖既有情景（每次记录从既有 episode.&lt;n&gt; 键推导下一序号）。
 */
class EpisodeLedgerSequenceRecoveryTest {

    private final SessionStateStore stateStore = new InMemorySessionStateStore();

    @Test
    void sequenceContinuesAcrossRestartWithoutOverwrite() {
        EpisodeLedger first = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});
        first.record("s1", "部署数据库迁移", "migrate --apply", "success");
        first.record("s1", "回滚坏版本", "rollback v1.2", "success");
        assertThat(stateStore.getAll("s1")).hasSize(2);

        // 重启：新实例（进程内 sequence 从零）——从持久状态恢复，既有情景不覆盖
        EpisodeLedger restarted = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});
        restarted.record("s1", "扩容分片", "reshard --from 2 --to 4", "success");

        Map<String, io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry> all =
                stateStore.getAll("s1");
        assertThat(all).hasSize(3); // episode.1 / episode.2 / episode.3
        assertThat(all.keySet()).containsExactlyInAnyOrder("episode.1", "episode.2", "episode.3");

        // 召回可见全部三条（序号连续且互不覆盖）
        assertThat(restarted.recallExamples("s1", "部署数据库迁移", 5)).hasSize(3);
    }

    @Test
    void sequenceIsPerSession() {
        EpisodeLedger ledger = new EpisodeLedger(stateStore, text -> new float[]{1f});
        ledger.record("s1", "任务 A", "", "success");
        ledger.record("s2", "任务 B", "", "success");

        assertThat(stateStore.getAll("s1")).containsKey("episode.1");
        assertThat(stateStore.getAll("s2")).containsKey("episode.1"); // 各会话独立序号空间
    }
}
