package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 707 / T1014–T1015：spill 双文件配对巡检——健康对不报、孤 data 带字节、
 * 孤 meta、root 缺席诚实零、null fail-fast。
 */
class SpillPairAuditTest {

    @TempDir
    Path root;

    @Test
    void healthyPairsAreNotFlaggedAndOrphansAre() throws Exception {
        Path session = root.resolve("agent-a").resolve("sess-1");
        Files.createDirectories(session);
        // 健康对
        Files.write(session.resolve("turn-1.spill"), "内容数据".getBytes());
        Files.write(session.resolve("turn-1.meta"), "{}".getBytes());
        // 孤 data（meta 写失败崩溃窗口）
        byte[] bigPayload = new byte[2048];
        Files.write(session.resolve("turn-2.spill"), bigPayload);
        // 孤 meta（data 写失败崩溃窗口）
        Files.write(session.resolve("turn-3.meta"), "{}".getBytes());

        SpillPairAudit.Report report = SpillPairAudit.audit(root);
        assertThat(report.dataFiles()).isEqualTo(2);
        assertThat(report.metaFiles()).isEqualTo(2);
        assertThat(report.dataBytes()).isEqualTo("内容数据".getBytes().length + 2048);

        assertThat(report.findings()).hasSize(2);
        assertThat(report.findings().get(0).kind()).isEqualTo("DATA_WITHOUT_META");
        assertThat(report.findings().get(0).uri()).isEqualTo("agent-a/sess-1/turn-2.spill");
        assertThat(report.findings().get(0).bytes()).isEqualTo(2048);
        assertThat(report.findings().get(1).kind()).isEqualTo("META_WITHOUT_DATA");
        assertThat(report.findings().get(1).uri()).isEqualTo("agent-a/sess-1/turn-3.meta");
    }

    @Test
    void missingRootIsHonestZero() throws Exception {
        SpillPairAudit.Report missing = SpillPairAudit.audit(root.resolve("not-exist"));
        assertThat(missing.findings()).isEmpty();
        assertThat(missing.dataFiles()).isZero();
        assertThat(missing.metaFiles()).isZero();
        assertThat(missing.dataBytes()).isZero();

        // 空 root（目录存在无文件）同零
        SpillPairAudit.Report empty = SpillPairAudit.audit(Files.createDirectory(root.resolve("empty")));
        assertThat(empty.findings()).isEmpty();
    }

    @Test
    void nullPathFailsFast() {
        assertThatThrownBy(() -> SpillPairAudit.audit(null))
                .isInstanceOf(NullPointerException.class);
        List<SpillPairAudit.Finding> none = SpillPairAudit.audit(root).findings();
        assertThat(none).isEmpty();
    }
}
