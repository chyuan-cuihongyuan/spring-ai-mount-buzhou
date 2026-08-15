package io.github.chyuan_cuihongyuan.buzhou.examples.perf;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionExport;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookOutboxPerfAccess;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 新能力性能哨兵（spec 22 增补 / T125 / impl-100）：outbox 前缀扫描（千条积压）、
 * 会话索引过滤查询（万行）、会话导出 JSON 序列化（千消息）——同 PerfBaselineTest
 * 口径：10 倍宽幅粗粒度回归哨兵，nightly 以 -Dgroups=perf 激活。
 */
@Tag("perf")
class PerfEffort7SentinelsTest {

    /** outbox append+due 千条批扫 P95 上限 ms（内存 store 首轮实测 < 5ms，10 倍+冗余）。 */
    private static final double OUTBOX_SCAN_P95_MAX_MILLIS = 100;

    /** 索引万行过滤查询 P95 上限 ms（首轮实测 < 10ms，10 倍冗余）。 */
    private static final double INDEX_QUERY_P95_MAX_MILLIS = 150;

    /** 千消息 export toJson+fromJson 往返 P95 上限 ms（首轮实测 < 60ms，10 倍冗余）。 */
    private static final double EXPORT_ROUNDTRIP_P95_MAX_MILLIS = 800;

    private static final int SAMPLES = 30;

    /** ①outbox：千条未决积压下 append+due 批扫（spec 33 §C 前缀扫描路径）。 */
    @Test
    void outboxScanSentinel() {
        InMemorySessionStateStore store = new InMemorySessionStateStore();
        WebhookOutboxPerfAccess outbox = new WebhookOutboxPerfAccess(store, 10_000);
        for (int i = 0; i < 1_000; i++) {
            outbox.append(UUID.randomUUID().toString(), "perf.event",
                    "{\"eventId\":\"" + i + "\",\"payload\":\"x\"}");
        }

        double[] samples = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long start = System.nanoTime();
            outbox.append(UUID.randomUUID().toString(), "perf.event", "{\"p\":1}");
            assertThat(outbox.dueNow(50)).isNotEmpty();
            samples[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        assertThat(p95(samples)).isLessThan(OUTBOX_SCAN_P95_MAX_MILLIS);
    }

    /** ②索引：万行下过滤查询（tag 过滤 + lastActive 倒序分页）。 */
    @Test
    void indexQuerySentinel() {
        InMemorySessionIndexStore index = new InMemorySessionIndexStore();
        for (int i = 0; i < 10_000; i++) {
            index.upsert(new SessionInfo("s-" + i, "app-" + (i % 10), "ag",
                    SessionInfo.STATUS_ACTIVE, 1L, i, 1,
                    i % 3 == 0 ? Map.of("env", "prod") : Map.of()));
        }

        double[] samples = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long start = System.nanoTime();
            var page = index.list(new SessionIndexQuery(
                    "app-3", null, null, "env", "prod", 0, 50));
            assertThat(page).isNotEmpty();
            samples[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        assertThat(p95(samples)).isLessThan(INDEX_QUERY_P95_MAX_MILLIS);
    }

    /** ③导出：千消息 toJson+fromJson 往返。 */
    @Test
    void exportRoundTripSentinel() {
        List<BuzhouMessage> messages = new ArrayList<>();
        for (int turn = 1; turn <= 250; turn++) {
            messages.add(new BuzhouMessage(UUID.randomUUID().toString(), "perf-export",
                    turn, 0, Role.USER, "第 " + turn + " 轮提问（" + "x".repeat(200) + "）",
                    List.of(), null, null, null, Map.of(), Instant.now()));
            messages.add(new BuzhouMessage(UUID.randomUUID().toString(), "perf-export",
                    turn, 1, Role.ASSISTANT, "第 " + turn + " 轮回答",
                    List.of(), null, null, null, Map.of(), Instant.now()));
        }
        SessionExport export = SessionExport.of("perf-export", "app", "ag", messages, null, Map.of());

        double[] samples = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long start = System.nanoTime();
            SessionExport parsed = SessionExport.fromJson(export.toJson());
            assertThat(parsed.messages()).hasSize(500);
            samples[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        assertThat(p95(samples)).isLessThan(EXPORT_ROUNDTRIP_P95_MAX_MILLIS);
    }

    private static double p95(double[] samples) {
        double[] sorted = samples.clone();
        java.util.Arrays.sort(sorted);
        return sorted[(int) Math.ceil(sorted.length * 0.95) - 1];
    }
}
