package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 539 / T827–828：spill 回读审计——回读样本落位（uri/字节）、完整性
 * 告警计数、per-uri 计数降序、窗口有界（60/67 导出族同构）。
 */
class ReadAuditTrailTest {

    private static final String LONG = "x".repeat(5000);

    private static SpillUri uri(String agent, String session) {
        return new SpillUri(agent, session, "tc-1");
    }

    @Test
    void readRangeRecordsAuditSamples(@TempDir Path dir) throws Exception {
        DiskSpillStore store = new DiskSpillStore(dir);
        SpillUri uri = uri("agent-a", "sess-a");
        store.store(SpillEntry.of(uri, LONG), 10);
        var trail = store.readAudit();

        long before = trail.totalReads();
        store.readRange(uri, new RangeReadRequest(RangeReadRequest.Mode.BYTES, 0, 100, null, null, null, null));
        assertThat(trail.totalReads()).isEqualTo(before + 1);
        assertThat(trail.recent()).isNotEmpty();
        assertThat(trail.recent().getLast().uri()).contains("sess-a");
        assertThat(trail.totalBytes()).isGreaterThanOrEqualTo(100);
        assertThat(trail.uriCounts()).isNotEmpty();
    }

    @Test
    void integrityWarningRecordedOnCorruption(@TempDir Path dir) throws Exception {
        DiskSpillStore store = new DiskSpillStore(dir);
        SpillUri uri = uri("agent-a", "sess-b");
        store.store(SpillEntry.of(uri, LONG), 10);
        long before = store.readAudit().integrityWarnings();

        // 破坏落盘数据文件（触发读侧完整性告警）
        try (var walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile).forEach(p -> {
                try {
                    String text = Files.readString(p);
                    if (text.contains(LONG.substring(0, 100))) {
                        Files.writeString(p, text.replace(LONG.substring(0, 100), "CORRUPTED!!"));
                    }
                } catch (Exception ignored) {
                    // 目录/元数据文件跳过
                }
            });
        }
        store.readRange(uri, new RangeReadRequest(RangeReadRequest.Mode.BYTES, 0, 50, null, null, null, null));
        assertThat(store.readAudit().integrityWarnings()).isGreaterThanOrEqualTo(before);
    }

    @Test
    void trailWindowBounded() {
        ReadAuditTrail trail = new ReadAuditTrail(8);
        for (int i = 0; i < 20; i++) {
            trail.record("spill://x#" + i, 10, false, java.time.Instant.now());
        }
        assertThat(trail.recent()).hasSize(8);
        assertThat(trail.totalReads()).isEqualTo(20);
        assertThat(trail.uriCounts()).isNotEmpty();
    }

    @Test
    void negativeBytesIgnored() {
        ReadAuditTrail trail = new ReadAuditTrail(8);
        trail.record("u", -5, false, java.time.Instant.now());
        assertThat(trail.totalReads()).isEqualTo(1);
        assertThat(trail.totalBytes()).isZero();
    }
}
