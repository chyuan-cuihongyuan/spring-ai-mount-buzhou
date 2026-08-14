package io.github.chyuan_cuihongyuan.buzhou.examples.perf;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultCompletedTurnDetector;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.DefaultMicroCompactor;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionPolicy;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 轻量性能基准（spec 21 / T93 / impl-68）：不引 JMH——目标是**粗粒度回归哨兵**（10 倍级越顶
 * 才失败，防环境噪声误报），nightly workflow 以 {@code -Dgroups=perf} 激活（默认 CI 排除）。
 *
 * <p>场景三件：①微压缩吞吐（msgs/s）；②100 轮会话端到端每轮 wall time P50/P95（零延迟模型
 * 度量 harness 自身开销——框架回归最敏感信号）；③消息存储 100 轮写入+全量读回 round-trip。
 *
 * <p><b>解读规则</b>：跨机器绝对值不可比，只看同机时间序列趋势；越顶 = 人工 profiling 信号，
 * 不许调阈值了事。首轮基线落档 docs/perf/baseline.md。
 */
@Tag("perf")
class PerfBaselineTest {

    /** 宽幅哨兵（观测档语义）：微压缩吞吐下限 msgs/s（首轮实测约 5 万+，10 倍冗余）。 */
    private static final double MICRO_COMPACTION_MIN_MSGS_PER_SEC = 3_000;

    /** 每轮 wall time P95 上限 ms（零延迟模型 + 内存存储，首轮实测 P95 < 30ms，10 倍冗余）。 */
    private static final double TURN_P95_MAX_MILLIS = 500;

    /** 存储读写 round-trip P95 上限 ms（100 轮写入 + 全量读回，首轮实测 < 20ms，10 倍冗余）。 */
    private static final double STORE_ROUNDTRIP_P95_MAX_MILLIS = 300;

    private static final int WARMUP_TURNS = 10;
    private static final int MEASURED_TURNS = 100;

    /** ①微压缩吞吐：500 条工具返回消息全量逐出，吞吐不低于哨兵。 */
    @Test
    void microCompactionThroughputSentinel() {
        List<BuzhouMessage> history = toolResultHistory(500);
        DefaultMicroCompactor compactor =
                new DefaultMicroCompactor(new DefaultCompletedTurnDetector());

        // warmup
        for (int i = 0; i < 3; i++) {
            compactor.compact(history, history.size(), tool -> MicroCompactionPolicy.defaults(), 1);
        }
        long start = System.nanoTime();
        var result = compactor.compact(history, history.size(),
                tool -> MicroCompactionPolicy.defaults(), 1);
        long elapsedNanos = System.nanoTime() - start;

        double msgsPerSec = history.size() / (elapsedNanos / 1e9);
        System.out.printf("[perf] micro-compaction: %d msgs, %.1f ms, %.0f msgs/s (哨兵 >= %.0f)%n",
                history.size(), elapsedNanos / 1e6, msgsPerSec, MICRO_COMPACTION_MIN_MSGS_PER_SEC);
        assertThat(result).isNotNull();
        assertThat(msgsPerSec).as("微压缩吞吐 10 倍级回归哨兵").isGreaterThan(MICRO_COMPACTION_MIN_MSGS_PER_SEC);
    }

    /** ②100 轮会话端到端：零延迟模型，每轮 wall time P50/P95 度量 harness 开销。 */
    @Test
    void hundredTurnSessionOverheadSentinel() {
        ScriptedChatModel model = new ScriptedChatModel();
        for (int i = 0; i < WARMUP_TURNS + MEASURED_TURNS; i++) {
            model.enqueueText("ok-" + i);
        }
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());
        AgentSession session = runtime.spawn("app", "perf-agent", "perf-sess");

        for (int i = 0; i < WARMUP_TURNS; i++) {
            session.chat("warmup-" + i);
        }
        long[] nanos = new long[MEASURED_TURNS];
        for (int i = 0; i < MEASURED_TURNS; i++) {
            long start = System.nanoTime();
            session.chat("q-" + i);
            nanos[i] = System.nanoTime() - start;
        }
        session.close();

        double p50 = percentile(nanos, 0.50) / 1e6;
        double p95 = percentile(nanos, 0.95) / 1e6;
        System.out.printf("[perf] %d-turn session: p50=%.2fms p95=%.2fms (哨兵 p95 < %.0fms)%n",
                MEASURED_TURNS, p50, p95, TURN_P95_MAX_MILLIS);
        assertThat(p95).as("会话每轮开销 10 倍级回归哨兵").isLessThan(TURN_P95_MAX_MILLIS);
    }

    /** ③存储 round-trip：100 轮逐轮写入 + 全量读回，P95 哨兵。 */
    @Test
    void messageStoreRoundTripSentinel() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "perf-store";
        long[] nanos = new long[MEASURED_TURNS];
        for (int i = 0; i < MEASURED_TURNS + WARMUP_TURNS; i++) {
            BuzhouMessage user = userMsg(sid, i, "第 " + i + " 轮提问，附带你需要记住的事实编号 " + i);
            BuzhouMessage assistant = new BuzhouMessage(UUID.randomUUID().toString(), sid, i, 1,
                    Role.ASSISTANT, "第 " + i + " 轮回答", List.of(), null, null, null, Map.of(),
                    Instant.now());
            long start = System.nanoTime();
            stores.messageStore().append(sid, List.of(user, assistant));
            int loaded = stores.messageStore().load(sid).size();
            nanos[i % MEASURED_TURNS] = System.nanoTime() - start;
            assertThat(loaded).isEqualTo((i + 1) * 2);
        }
        double p50 = percentile(nanos, 0.50) / 1e6;
        double p95 = percentile(nanos, 0.95) / 1e6;
        System.out.printf("[perf] store round-trip(append+load-all): p50=%.2fms p95=%.2fms (哨兵 p95 < %.0fms)%n",
                p50, p95, STORE_ROUNDTRIP_P95_MAX_MILLIS);
        assertThat(p95).as("存储读写 10 倍级回归哨兵").isLessThan(STORE_ROUNDTRIP_P95_MAX_MILLIS);
    }

    // ---- helpers ----

    private static List<BuzhouMessage> toolResultHistory(int count) {
        List<BuzhouMessage> history = new ArrayList<>(count * 2);
        for (int turn = 1; turn <= count; turn++) {
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), "perf", turn, 0, Role.USER,
                    "问题 " + turn, List.of(), null, null, null, Map.of(), Instant.now()));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), "perf", turn, 1,
                    Role.TOOL, "工具返回 ".repeat(50) + turn, List.of(), "tool_" + (turn % 5),
                    UUID.randomUUID().toString(), null, Map.of(), Instant.now()));
        }
        return history;
    }

    private static BuzhouMessage userMsg(String sid, int turn, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sid, turn, 0, Role.USER,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** 简单最近邻分位数（哨兵精度足够；不引流式库）。 */
    private static long percentile(long[] sorted, double q) {
        long[] copy = sorted.clone();
        Arrays.sort(copy);
        int index = (int) Math.min(copy.length - 1, Math.round(q * (copy.length - 1)));
        return copy[index];
    }
}
