package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1438 / T2184：Spill 冷热分层访问审计——never/single/multi 三桶、
 * 冷/热占比派生、空库 -1 哨兵、窗口截断口径（读事件有界窗）。
 */
class SpillTieringAuditTest {

    private static ReadAuditTrail.ReadRecord read(String uri) {
        return new ReadAuditTrail.ReadRecord(uri, 100, false,
                Instant.parse("2026-09-14T10:00:00Z"));
    }

    @Test
    void emptyStoreYieldsSentinel() {
        var r = SpillTieringAudit.analyze(0, List.of());
        assertThat(r.totalHandles()).isZero();
        assertThat(r.hotRatio()).isEqualTo(-1d);
        assertThat(r.coldRatio()).isEqualTo(-1d);
    }

    @Test
    void neverReadSingleAndMultiBucketed() {
        // 5 个存量：2 个从未读、1 个单读、2 个多次读
        List<ReadAuditTrail.ReadRecord> reads = List.of(
                read("spill://hot"), read("spill://hot"),
                read("spill://hot"), read("spill://hot"), // hot 4 次
                read("spill://once"));
        var r = SpillTieringAudit.analyze(5, reads);
        assertThat(r.totalHandles()).isEqualTo(5);
        assertThat(r.neverReadCount()).isEqualTo(3); // 5 − 覆盖 2
        assertThat(r.singleReadCount()).isEqualTo(1);
        assertThat(r.multiReadCount()).isEqualTo(1);
        assertThat(r.hotRatio()).isEqualTo(1.0d / 5);
        assertThat(r.coldRatio()).isEqualTo(3.0d / 5);
    }

    @Test
    void readsExceedingHandlesClampNeverToZero() {
        // trail 窗口可能含已删除 uri 的历史读——never 钳 0 不为负
        var r = SpillTieringAudit.analyze(1, List.of(
                read("spill://a"), read("spill://b"), read("spill://c")));
        assertThat(r.neverReadCount()).isZero();
    }

    @Test
    void thresholdBoundaryTwoReadsIsHot() {
        var r = SpillTieringAudit.analyze(1, List.of(read("x"), read("x")));
        assertThat(r.multiReadCount()).isEqualTo(1);
        assertThat(r.singleReadCount()).isZero();
    }
}
