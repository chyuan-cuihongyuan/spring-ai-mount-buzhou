package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.error.QuotaExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-38 / spec 13 §growth-8：spill 磁盘配额（超限拒绝落盘——noeviction，原文回喂）、
 * 启动孤儿扫描（报告 + 清理，幂等）、TTL 过期清理经 sweeper 步骤形状可调。
 */
class SpillQuotaAndOrphanTest {

    @TempDir
    Path rootDir;

    private DiskSpillStore store(SpillQuota quota) {
        return new DiskSpillStore(rootDir, quota);
    }

    @Test
    void perSessionFileQuotaRejectsBeyondLimit() {
        DiskSpillStore store = store(new SpillQuota(null, 2));
        store.store(SpillEntry.of(new SpillUri("agent", "s1", "tc-1"), "x".repeat(100)), 32);
        store.store(SpillEntry.of(new SpillUri("agent", "s1", "tc-2"), "y".repeat(100)), 32);

        assertThatThrownBy(() -> store.store(
                SpillEntry.of(new SpillUri("agent", "s1", "tc-3"), "z".repeat(100)), 32))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("maxFilesPerSession=2");
        assertThat(store.exists(new SpillUri("agent", "s1", "tc-3"))).isFalse(); // 拒绝不留半份
        // 其他会话不受影响
        store.store(SpillEntry.of(new SpillUri("agent", "s2", "tc-1"), "w".repeat(100)), 32);
        assertThat(store.exists(new SpillUri("agent", "s2", "tc-1"))).isTrue();
    }

    @Test
    void totalBytesQuotaRejectsWhenWouldExceed() {
        DiskSpillStore store = store(new SpillQuota(250L, null));
        store.store(SpillEntry.of(new SpillUri("agent", "s1", "tc-1"), "a".repeat(100)), 32);

        assertThatThrownBy(() -> store.store(
                SpillEntry.of(new SpillUri("agent", "s2", "tc-1"), "b".repeat(200)), 32))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("maxTotalBytes=250");
        // 释放后可再写（noeviction：删旧换新）
        store.delete(new SpillUri("agent", "s1", "tc-1"));
        store.store(SpillEntry.of(new SpillUri("agent", "s2", "tc-1"), "b".repeat(200)), 32);
        assertThat(store.exists(new SpillUri("agent", "s2", "tc-1"))).isTrue();
    }

    @Test
    void quotaRejectionDegradesToPassthroughViaService() {
        DiskSpillStore store = store(new SpillQuota(null, 1));
        SpillService service = new SpillService(store, 32, 5);
        String longResult = "r".repeat(200);
        service.tryOffload("agent", "s1", "tc-1", "big_tool", longResult, 50);

        // 第二次超限：拒绝落盘 → 原文回喂（degraded，提示走显式分页）
        SpillService.OffloadOutcome outcome =
                service.tryOffload("agent", "s1", "tc-2", "big_tool", longResult, 50);

        assertThat(outcome.offloaded()).isFalse();
        assertThat(outcome.degraded()).isTrue();
        assertThat(outcome.text()).isEqualTo(longResult); // 原文照常回喂
    }

    @Test
    void orphanSweepCleansOnlyDeadSessionsAndIsIdempotent() {
        DiskSpillStore store = store(SpillQuota.unbounded());
        store.store(SpillEntry.of(new SpillUri("agent", "live-1", "tc-1"), "x".repeat(50)), 16);
        store.store(SpillEntry.of(new SpillUri("agent", "dead-1", "tc-1"), "y".repeat(50)), 16);
        store.store(SpillEntry.of(new SpillUri("agent", "dead-2", "tc-1"), "z".repeat(50)), 16);

        int swept = store.sweepOrphans(Set.of("live-1"));

        assertThat(swept).isEqualTo(2);
        assertThat(store.exists(new SpillUri("agent", "live-1", "tc-1"))).isTrue();
        assertThat(store.exists(new SpillUri("agent", "dead-1", "tc-1"))).isFalse();
        assertThat(store.exists(new SpillUri("agent", "dead-2", "tc-1"))).isFalse();
        // 幂等：再扫无孤儿
        assertThat(store.sweepOrphans(Set.of("live-1"))).isZero();
    }

    @Test
    void deleteExpiredRemovesOnlyUnlinkedBeyondTtl() {
        DiskSpillStore store = store(SpillQuota.unbounded());
        // 旧条目（createdAt 两天前、未 link）→ 过 TTL；新条目 → 保留
        store.store(new SpillEntry(new SpillUri("agent", "s1", "tc-1"), "old",
                "text/plain", 3, Instant.now().minus(Duration.ofDays(2))), 16);
        store.store(SpillEntry.of(new SpillUri("agent", "s1", "tc-2"), "fresh"), 16);

        int deleted = store.deleteExpired(Instant.now(), Duration.ofDays(1));

        assertThat(deleted).isEqualTo(1);
        assertThat(store.exists(new SpillUri("agent", "s1", "tc-1"))).isFalse();
        assertThat(store.exists(new SpillUri("agent", "s1", "tc-2"))).isTrue();
    }
}
