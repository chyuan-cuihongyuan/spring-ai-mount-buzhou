package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 742 / T1086–T1087：sweepOrphans 保留计数读数——fork 引用保留计入
 * totalRetainedOrphans/lastSweepRetained、彻底清除不计保留、初始 -1 哨兵。
 */
class SweepRetainedReadoutTest {

    @TempDir
    Path root;

    private SpillHandle spill(DiskSpillStore store, String session, String name, String content) {
        return store.store(SpillEntry.of(
                new SpillUri("agent-a", session, name), content), 32);
    }

    @Test
    void sweepRetainedAndDeletedAreSeparatelyVisible() {
        DiskSpillStore store = new DiskSpillStore(root);
        assertThat(store.lastSweepRetained()).isEqualTo(-1); // 从未执行
        assertThat(store.totalRetainedOrphans()).isZero();

        // fork-child 的证据被 live-fork 引用（登记引用）；plain 的无引用
        spill(store, "fork-child", "evidence-1", "被 fork 引用的证据");
        spill(store, "plain", "evidence-2", "无引用证据");
        store.acquireSessionReferences("fork-child", "live-fork");

        int deleted = store.sweepOrphans(java.util.Set.of());
        assertThat(deleted).isEqualTo(1); // 无引用的 plain 被清
        assertThat(store.lastSweepRetained()).isEqualTo(1); // fork-child 被保留
        assertThat(store.totalRetainedOrphans()).isEqualTo(1);
        assertThat(store.exists(new SpillUri("agent-a", "fork-child", "evidence-1"))).isTrue();
        assertThat(store.exists(new SpillUri("agent-a", "plain", "evidence-2"))).isFalse();
    }
}
