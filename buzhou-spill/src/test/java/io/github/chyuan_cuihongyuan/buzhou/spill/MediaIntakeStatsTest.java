package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1445 / T2188 兄弟票：媒体摄入统计——intake/readBack/bytes 计数、
 * MIME 直方降序、intakeText 复用同一漏斗、reset 归零（真 store 小载荷）。
 */
class MediaIntakeStatsTest {

    @Test
    void intakeAndReadBackCounted() {
        MediaIntake intake = new MediaIntake(new DiskSpillStore(tempDir()), 200);
        var ref1 = intake.intake("图片字节".getBytes(StandardCharsets.UTF_8),
                "image/png", "ag", "s-mi");
        var ref2 = intake.intakeText("音频转写文本", "text/plain", "ag", "s-mi");
        intake.readBack(ref1);
        intake.readBack(ref2);
        var s = intake.stats();
        assertThat(s.intakes()).isEqualTo(2);
        assertThat(s.readBacks()).isEqualTo(2);
        assertThat(s.bytesTotal()).isEqualTo(
                "图片字节".getBytes(StandardCharsets.UTF_8).length
                        + "音频转写文本".getBytes(StandardCharsets.UTF_8).length);
        // 直方：text/plain 与 image/png 各 1（同数平名典序——text/plain 在前）
        assertThat(s.byMime()).containsKeys("text/plain", "image/png");
        intake.resetStatsForTest();
        assertThat(intake.stats().intakes()).isZero();
    }

    static java.nio.file.Path tempDir() {
        try {
            return java.nio.file.Files.createTempDirectory("mi-stats");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
