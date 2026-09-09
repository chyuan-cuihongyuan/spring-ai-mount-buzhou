package io.github.chyuan_cuihongyuan.buzhou.core.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 405 §Testing / T701–T702：健康时间线——diff-only 入环/首次 from=null/
 * 容量封顶；recorder 轮询翻转；JSONL 落盘行；端点载荷 + yml 装配（默认关）。
 */
class HealthTimelineTest {

    /** 可翻转的 stub 健康面。 */
    private static final class StubHealth implements BuzhouHealth {
        volatile BuzhouHealth.Status status = BuzhouHealth.Status.UP;

        @Override
        public String mechanism() {
            return "stub";
        }

        @Override
        public BuzhouHealth.Status status() {
            return status;
        }

        @Override
        public Map<String, Object> details() {
            return Map.of();
        }
    }

    @Test
    void shouldRecordOnlyTransitions_withNullFromOnFirstSighting() {
        HealthTimeline timeline = new HealthTimeline(8);
        Instant t0 = Instant.parse("2026-09-08T00:00:00Z");
        // 首轮：初见 from=null（LinkedHashMap 定序——断言确定）
        java.util.Map<String, BuzhouHealth.Status> first = new LinkedHashMap<>();
        first.put("memory", BuzhouHealth.Status.UP);
        first.put("spill", BuzhouHealth.Status.DOWN);
        var fresh = timeline.record(first, t0);
        assertThat(fresh).hasSize(2);
        assertThat(timeline.entries()).hasSize(2);
        assertThat(timeline.entries().get(0).from()).isNull();
        assertThat(timeline.entries().get(0).to()).isEqualTo(BuzhouHealth.Status.UP);

        // 同快照：无变迁零行
        assertThat(timeline.record(Map.of("memory", BuzhouHealth.Status.UP, "spill", BuzhouHealth.Status.DOWN),
                t0.plusSeconds(15))).isEmpty();
        assertThat(timeline.entries()).hasSize(2);

        // 翻转一个：只记它
        var flipped = timeline.record(Map.of("memory", BuzhouHealth.Status.DOWN, "spill", BuzhouHealth.Status.DOWN),
                t0.plusSeconds(30));
        assertThat(flipped).hasSize(1);
        assertThat(flipped.get(0).mechanism()).isEqualTo("memory");
        assertThat(flipped.get(0).from()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(flipped.get(0).to()).isEqualTo(BuzhouHealth.Status.DOWN);

        // 新机制出现：初见行
        timeline.record(Map.of("memory", BuzhouHealth.Status.DOWN, "spill", BuzhouHealth.Status.DOWN, "new-one", BuzhouHealth.Status.UP),
                t0.plusSeconds(45));
        assertThat(timeline.entries()).hasSize(4);

        // 计数（抖动识别面）
        assertThat(timeline.transitionCounts()).containsEntry("memory", 2L);
    }

    @Test
    void shouldEvictOldest_whenCapacityExceeded() {
        HealthTimeline timeline = new HealthTimeline(2);
        Instant t = Instant.parse("2026-09-08T00:00:00Z");
        timeline.record(Map.of("a", BuzhouHealth.Status.UP), t);
        timeline.record(Map.of("a", BuzhouHealth.Status.DOWN), t.plusSeconds(1));
        timeline.record(Map.of("a", BuzhouHealth.Status.UP), t.plusSeconds(2));
        assertThat(timeline.entries()).hasSize(2); // 封顶挤出最旧
        assertThat(timeline.entries().get(0).to()).isEqualTo(BuzhouHealth.Status.DOWN);
    }

    @Test
    void shouldDriveRecorderPolls_andExportJsonl(@TempDir Path dir) throws Exception {
        StubHealth health = new StubHealth();
        Path jsonlPath = dir.resolve("timeline.jsonl");
        try (HealthTimelineJsonl jsonl = new HealthTimelineJsonl(jsonlPath)) {
            HealthTimelineRecorder recorder = new HealthTimelineRecorder(
                    () -> Map.of(health.mechanism(), (BuzhouHealth) health),
                    Duration.ofSeconds(15), jsonl, new HealthTimeline(16));
            Instant t0 = Instant.parse("2026-09-08T00:00:00Z");
            recorder.pollOnce(t0);             // 初见 UP
            health.status = BuzhouHealth.Status.DOWN;
            recorder.pollOnce(t0.plusSeconds(15)); // UP→DOWN
            recorder.pollOnce(t0.plusSeconds(30)); // 无变迁
            health.status = BuzhouHealth.Status.UP;
            recorder.pollOnce(t0.plusSeconds(45)); // DOWN→UP

            assertThat(recorder.timeline().entries()).hasSize(3);
            assertThat(recorder.timeline().transitionCounts()).containsEntry("stub", 3L);
            assertThat(jsonl.written()).isEqualTo(3);
        }
        List<String> lines = Files.readAllLines(jsonlPath);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1)).contains("\"mechanism\":\"stub\"")
                .contains("\"from\":\"UP\"").contains("\"to\":\"DOWN\"");

        // 端点载荷
        Map<String, Object> payload = new BuzhouTimelineEndpoint(null).timeline();
        assertThat(payload).containsEntry("entries", List.of());

        HealthTimelineRecorder recorder2 = new HealthTimelineRecorder(
                () -> Map.of(health.mechanism(), (BuzhouHealth) health),
                Duration.ofSeconds(15), null);
        recorder2.pollOnce(Instant.now());
        Map<String, Object> live = new BuzhouTimelineEndpoint(recorder2).timeline();
        assertThat((List<?>) live.get("entries")).hasSize(1);
        assertThat(live).containsEntry("capacity", HealthTimeline.DEFAULT_CAPACITY);
    }

    @Test
    void shouldAssembleFromYml_onlyWhenEnabled() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .withPropertyValues("buzhou.health.timeline.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouHealthTimelineRecorder");
                    assertThat(context).hasBean("buzhouTimelineEndpoint");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouHealthTimelineRecorder");
                    assertThat(context).doesNotHaveBean("buzhouTimelineEndpoint");
                });
    }
}
