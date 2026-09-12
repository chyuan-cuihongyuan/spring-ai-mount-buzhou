package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JSONL 大小轮转测试（spec 642 / T934–T935 / impl 495，Logback
 * RollingFileAppender 思想）：触发轮转（代际 shift、行完整）、代际封顶、
 * 显式关、静态检查（逐次开写族）。
 */
class RollingJsonlWriterTest {

    @TempDir
    Path dir;

    private Path file(String name) {
        return dir.resolve(name);
    }

    /** 触发轮转：超限行入新代——file.1 持旧行、原文件重开只含新行、行完整。 */
    @Test
    void exceedingLimitRotatesAndShiftsGeneration() throws IOException {
        Path f = file("timeline.jsonl");
        // 行 "{\"n\":1}" = 8 字符 + \n = 9B 记账：两行 16B，第三行判定 16+8=24 > 20 触发
        try (RollingJsonlWriter w = new RollingJsonlWriter(f, 20, 3)) {
            w.appendLine("{\"n\":1}");
            w.appendLine("{\"n\":2}");
            w.appendLine("{\"n\":3}");
            assertThat(w.rotations()).isEqualTo(1);
        }
        Path gen1 = file("timeline.jsonl.1");
        assertThat(gen1).exists();
        List<String> oldLines = Files.readAllLines(gen1);
        assertThat(oldLines).containsExactly("{\"n\":1}", "{\"n\":2}");
        // 原文件重开只含轮转后的新行（每行完整一个 JSON——不半截）
        assertThat(Files.readAllLines(f)).containsExactly("{\"n\":3}");
    }

    /** 代际封顶：maxHistory=2 时最老代被删——.1/.2 存在、.3 永不存在。 */
    @Test
    void generationHistoryIsCapped() throws IOException {
        Path f = file("cap.jsonl");
        try (RollingJsonlWriter w = new RollingJsonlWriter(f, 20, 2)) {
            for (int i = 1; i <= 6; i++) {
                w.appendLine("{\"n\":" + i + "}");
            }
            assertThat(w.rotations()).isGreaterThanOrEqualTo(2);
        }
        assertThat(file("cap.jsonl.1")).exists();
        assertThat(file("cap.jsonl.2")).exists();
        assertThat(file("cap.jsonl.3")).doesNotExist();
    }

    /** 显式关（≤0）：无轮转文件生成——旧无界追加语义。 */
    @Test
    void zeroParamsDisableRolling() throws IOException {
        Path f = file("off.jsonl");
        try (RollingJsonlWriter w = new RollingJsonlWriter(f, 0, 0)) {
            assertThat(w.rollingEnabled()).isFalse();
            for (int i = 0; i < 10; i++) {
                w.appendLine("{\"n\":" + i + "}");
            }
        }
        assertThat(Files.readAllLines(f)).hasSize(10);
        assertThat(file("off.jsonl.1")).doesNotExist();
    }

    /** 打开时以现存文件大小初始化记账——重启后轮转判定不重置（不超限重复轮转）。 */
    @Test
    void reopenResumesByteAccounting() throws IOException {
        Path f = file("resume.jsonl");
        try (RollingJsonlWriter w = new RollingJsonlWriter(f, 100, 3)) {
            w.appendLine("{\"n\":1}"); // ~9B
        }
        try (RollingJsonlWriter w = new RollingJsonlWriter(f, 100, 3)) {
            assertThat(w.bytesWritten()).isEqualTo(Files.size(f));
            w.appendLine("{\"n\":2}"); // 记账续上——两行仍在同代
        }
        assertThat(file("resume.jsonl.1")).doesNotExist();
        assertThat(Files.readAllLines(f)).containsExactly("{\"n\":1}", "{\"n\":2}");
    }

    /** 静态轮转检查（逐次开写族——PromptUsageJsonl 同语义）：超限 shift、未超限 no-op。 */
    @Test
    void staticRotateIfNeededShiftsOnlyWhenExceeded() throws IOException {
        Path f = file("snap.jsonl");
        Files.writeString(f, "{\"n\":1}\n");
        // 未超限：no-op
        assertThat(RollingJsonlWriter.rotateIfNeeded(f, 10, 100, 3)).isZero();
        // 超限：shift（file→file.1；此后原文件不在——重开 CREATE 由调用方负责）
        assertThat(RollingJsonlWriter.rotateIfNeeded(f, 500, 100, 3)).isEqualTo(1);
        assertThat(Files.readString(file("snap.jsonl.1"))).isEqualTo("{\"n\":1}\n");
        assertThat(f).doesNotExist();
    }

    /**
     * spec 648 / T946–T947：轮转事件指标化——rotated 计数与轮转次数一致
     * （tag file 命中）；长驻与静态路径同发。
     */
    @Test
    void rotationEmitsMetrics() throws IOException {
        java.util.List<String> counters = new java.util.concurrent.CopyOnWriteArrayList<>();
        io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.install(
                new io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics() {
                    @Override
                    public void counter(String name, String... tagKeyValue) {
                        counters.add(name + "|" + String.join("=", tagKeyValue));
                    }

                    @Override
                    public void counter(String name, long delta, String... tagKeyValue) {
                        counters.add(name + "|" + String.join("=", tagKeyValue));
                    }

                    @Override
                    public void timer(String name, java.time.Duration duration, String... tagKeyValue) {
                        // 本用例只断言 counter 面
                    }
                });
        try {
            Path f = file("metrics.jsonl");
            try (RollingJsonlWriter w = new RollingJsonlWriter(f, 20, 3)) {
                for (int i = 1; i <= 6; i++) {
                    w.appendLine("{\"n\":" + i + "}");
                }
            }
            Path snap = file("snap-m.jsonl");
            Files.writeString(snap, "{\"n\":1}\n");
            RollingJsonlWriter.rotateIfNeeded(snap, 500, 100, 3);

            assertThat(counters.stream()
                    .filter(c -> c.startsWith(RollingJsonlWriter.METRIC_ROTATED + "|file=metrics.jsonl"))
                    .count()).isEqualTo(2); // 6 行 20B 阈值 → 两次长驻轮转
            assertThat(counters).contains(RollingJsonlWriter.METRIC_ROTATED + "|file=snap-m.jsonl");
        } finally {
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.reset();
        }
    }
}
