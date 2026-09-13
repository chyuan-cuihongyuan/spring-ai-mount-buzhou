package io.github.chyuan_cuihongyuan.buzhou.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 816 / T1134：记忆分层容量回归——三层层序与份额/水位三级边界/
 * 不设限层恒 OK/脏快照归零不炸。
 */
class MemoryHierarchyCapacityTest {

    @Test
    void layersSharesAndTotals() {
        MemoryHierarchyCapacity.Report report = MemoryHierarchyCapacity.analyze(
                new MemoryHierarchyCapacity.Snapshot(3000, 40, 2000, 120, 5000), 0, 0, 0);

        assertThat(report.totalChars()).isEqualTo(10_000);
        assertThat(report.layers()).hasSize(3);
        assertThat(report.layers().get(0).layer()).isEqualTo("core-summary");
        assertThat(report.layers().get(1).layer()).isEqualTo("archival-facts");
        assertThat(report.layers().get(1).items()).isEqualTo(40);
        assertThat(report.layers().get(2).layer()).isEqualTo("recall-window");
        assertThat(report.layers().get(2).items()).isEqualTo(120);
        // 不设限层：cap null、ratio null、恒 OK
        assertThat(report.layers()).allSatisfy(l -> {
            assertThat(l.capChars()).isNull();
            assertThat(l.fillRatio()).isNull();
            assertThat(l.level()).isEqualTo("OK");
        });
    }

    @Test
    void watermarkBoundariesExact() {
        MemoryHierarchyCapacity.Snapshot snap = new MemoryHierarchyCapacity.Snapshot(0, 0, 0, 0, 0);

        // 恰 80% → WARN；恰 100% → FULL；79.9% → OK
        var warn = MemoryHierarchyCapacity.analyze(
                new MemoryHierarchyCapacity.Snapshot(800, 0, 0, 0, 0), 1000, 0, 0);
        assertThat(warn.layers().get(0).level()).isEqualTo("WARN");
        assertThat(warn.layers().get(0).fillRatio()).isCloseTo(0.8, within(1e-9));

        var full = MemoryHierarchyCapacity.analyze(
                new MemoryHierarchyCapacity.Snapshot(1000, 0, 0, 0, 0), 1000, 0, 0);
        assertThat(full.layers().get(0).level()).isEqualTo("FULL");

        var ok = MemoryHierarchyCapacity.analyze(
                new MemoryHierarchyCapacity.Snapshot(799, 0, 0, 0, 0), 1000, 0, 0);
        assertThat(ok.layers().get(0).level()).isEqualTo("OK");

        // 超限（120%）仍 FULL
        var over = MemoryHierarchyCapacity.analyze(
                new MemoryHierarchyCapacity.Snapshot(1200, 0, 0, 0, 0), 1000, 0, 0);
        assertThat(over.layers().get(0).level()).isEqualTo("FULL");
        assertThat(over.layers().get(0).fillRatio()).isCloseTo(1.2, within(1e-9));
    }

    @Test
    void mixedCappedAndUncappedLayers() {
        MemoryHierarchyCapacity.Report report = MemoryHierarchyCapacity.analyze(
                new MemoryHierarchyCapacity.Snapshot(1000, 10, 850, 5, 100), 2000, 1000, 0);
        assertThat(report.layers().get(0).fillRatio()).isCloseTo(0.5, within(1e-9));
        assertThat(report.layers().get(0).level()).isEqualTo("OK");     // 1000/2000 = 50%
        assertThat(report.layers().get(1).level()).isEqualTo("WARN");  // 850/1000 = 85% → WARN
        assertThat(report.layers().get(1).fillRatio()).isCloseTo(0.85, within(1e-9));
        assertThat(report.layers().get(2).capChars()).isNull(); // recall 不设限
    }

    @Test
    void dirtySnapshotNegativeClampedToZero() {
        MemoryHierarchyCapacity.Report report = MemoryHierarchyCapacity.analyze(
                new MemoryHierarchyCapacity.Snapshot(-100, -5, -50, -1, -10), 0, 0, 0);
        assertThat(report.totalChars()).isZero();
        assertThat(report.layers().get(1).items()).isZero();
        assertThat(report.layers().get(1).chars()).isZero();
        // core-summary 恒按 1 条目计（摘要层单活跃版本语义）
        assertThat(report.layers().get(0).items()).isEqualTo(1);
    }
}
