package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * fork 证据引用生命周期测试（spec 26 / T105 / impl-80）：引用计数共享——
 * 源会话删除被 fork 引用的证据保留（最后引用者关闭）；无引用时级联/TTL/孤儿扫描
 * 语义回归；账本跨实例持久。
 */
class EvidenceLifecycleTest {

    @TempDir
    Path root;

    private static SpillUri uri(String sessionId, String toolCallId) {
        return new SpillUri("agent-a", sessionId, toolCallId);
    }

    /** fork 引用下源会话删除：证据物理保留，fork 可读；fork 关闭（最后引用者）→ 物理删。 */
    @Test
    void forkReferenceRetainsEvidenceUntilLastReferrerCloses() {
        DiskSpillStore store = new DiskSpillStore(root);
        SpillUri uri = uri("s1", "t1");
        store.store(SpillEntry.of(uri, "hello"), 100);

        int acquired = store.acquireSessionReferences("s1", "s2"); // fork s2 引用 s1 全部证据
        assertThat(acquired).isEqualTo(1);
        assertThat(store.evidenceReferrers(uri)).containsExactly("s2");

        int deleted = store.deleteBySession("agent-a", "s1"); // 源会话删除
        assertThat(deleted).isZero(); // 引用保留，不物理删
        assertThat(store.exists(uri)).isTrue();
        assertThat(store.readRange(uri, RangeReadRequest.bytes(0, 5)).content()).contains("hello");

        int drained = store.deleteBySession("agent-a", "s2"); // 最后引用者关闭
        assertThat(drained).isEqualTo(1); // 延迟物理删
        assertThat(store.exists(uri)).isFalse();
    }

    /** 无引用时级联删除回归：源会话删即物理删（既有行为零变化）。 */
    @Test
    void sessionCascadeStillDeletesWithoutReferences() {
        DiskSpillStore store = new DiskSpillStore(root);
        SpillUri uri = uri("s1", "t1");
        store.store(SpillEntry.of(uri, "data"), 100);

        assertThat(store.deleteBySession("agent-a", "s1")).isEqualTo(1);
        assertThat(store.exists(uri)).isFalse();
    }

    /** TTL 过期清理：被 fork 引用的未 link 证据不过期；引用释放后按原语义可清。 */
    @Test
    void expirySkipsReferencedEvidence() {
        DiskSpillStore store = new DiskSpillStore(root);
        SpillUri uri = uri("s1", "t1");
        store.store(SpillEntry.of(uri, "old"), 100);
        store.acquireSessionReferences("s1", "s2");

        int swept = store.deleteExpired(Instant.now().plus(Duration.ofHours(1)),
                Duration.ofMinutes(30));
        assertThat(swept).isZero(); // 引用保留
        assertThat(store.exists(uri)).isTrue();

        // 释放引用（最后引用者关闭）——延迟物理删即刻完成，无需再等 TTL
        assertThat(store.deleteBySession("agent-a", "s2")).isEqualTo(1);
        assertThat(store.exists(uri)).isFalse();
    }

    /** 孤儿扫描：死亡会话目录中被存活 fork 引用的整目录保留，无引用目录照扫。 */
    @Test
    void orphanSweepRetainsReferencedEvidence() {
        DiskSpillStore store = new DiskSpillStore(root);
        SpillUri kept = uri("dead-referenced", "t1");
        SpillUri swept = uri("dead-unreferenced", "t2");
        store.store(SpillEntry.of(kept, "keep"), 100);
        store.store(SpillEntry.of(swept, "gone"), 100);
        store.acquireSessionReferences("dead-referenced", "live-fork");

        int deleted = store.sweepOrphans(java.util.Set.of("live-fork", "other"));
        assertThat(deleted).isEqualTo(1); // 无引用目录扫掉，被引用目录保留
        assertThat(store.exists(kept)).isTrue();
        assertThat(store.exists(swept)).isFalse();
    }

    /** 账本跨实例持久（进程重启）：新 store 实例仍见 fork 引用，删除语义一致。 */
    @Test
    void ledgerPersistsAcrossStoreInstances() {
        DiskSpillStore first = new DiskSpillStore(root);
        SpillUri uri = uri("s1", "t1");
        first.store(SpillEntry.of(uri, "x"), 100);
        first.acquireSessionReferences("s1", "s2");

        DiskSpillStore restarted = new DiskSpillStore(root); // 模拟重启
        assertThat(restarted.evidenceReferrers(uri)).containsExactly("s2");
        assertThat(restarted.deleteBySession("agent-a", "s1")).isZero(); // 引用仍在，不删
    }
}
