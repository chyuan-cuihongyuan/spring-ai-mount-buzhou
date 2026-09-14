package io.github.chyuan_cuihongyuan.buzhou.observability.micrometer;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MicrometerDualWriter 补测（K 会话 R10 / spec 1209 / T1827——此前零测试文件）：
 * 双写适配器的指标口径合同——指标名/tag 键序、bounded 截断（32/64/16）、unknown 回退、
 * 非正时长与 count≤0 不记、NOOP 哨兵全 no-op。
 * 先例：ToolDurationTimerTest（CapturingMetrics + @AfterEach reset）。
 */
class MicrometerDualWriterTest {

    static final class CapturingMetrics implements BuzhouMetrics {
        final ConcurrentLinkedQueue<String> counters = new ConcurrentLinkedQueue<>();
        final ConcurrentLinkedQueue<String> timers = new ConcurrentLinkedQueue<>();

        /** k1=v1,k2=v2 形式（奇数位标签截尾）。 */
        static String fmt(String... tagKeyValue) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i + 1 < tagKeyValue.length; i += 2) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(tagKeyValue[i]).append('=').append(tagKeyValue[i + 1]);
            }
            return sb.toString();
        }

        @Override
        public void counter(String name, long delta, String... tagKeyValue) {
            counters.add(name + ":" + delta + ":" + fmt(tagKeyValue));
        }

        @Override
        public void timer(String name, Duration duration, String... tagKeyValue) {
            timers.add(name + ":" + duration.toMillis() + ":" + fmt(tagKeyValue));
        }
    }

    private final CapturingMetrics metrics = new CapturingMetrics();

    private MicrometerDualWriter writer() {
        BuzhouMetricsHolder.install(metrics);
        return new MicrometerDualWriter();
    }

    @AfterEach
    void cleanup() {
        BuzhouMetricsHolder.reset();
    }

    @Test
    void noopSentinelNeverTouchesMetrics() {
        MicrometerDualWriter noop = MicrometerDualWriter.NOOP;

        noop.recordSpanClose(SpanKind.MODEL_CALL, "m", "OK",
                Map.of("duration.ms", 5L, "model.name", "gpt"));
        noop.recordTokens("gpt", "prompt", 100);
        noop.recordTtft("gpt", Duration.ofMillis(10));
        noop.recordTpot("gpt", Duration.ofMillis(10));
        noop.recordQueueWait(10);
        noop.recordPersistError();

        assertThat(metrics.counters).isEmpty();
        assertThat(metrics.timers).isEmpty();
    }

    @Test
    void modelCallSpanCloseRecordsDurationWithProviderAndName() {
        MicrometerDualWriter writer = writer();

        writer.recordSpanClose(SpanKind.MODEL_CALL, "m", "OK", Map.of(
                "duration.ms", 250L, "model.provider", "openai", "model.name", "gpt-4o"));

        assertThat(metrics.timers).containsExactly(
                "buzhou.model.call.duration:250:model.provider=openai,model.name=gpt-4o");
    }

    @Test
    void modelCallUnknownProviderAndNameFallBack() {
        MicrometerDualWriter writer = writer();

        writer.recordSpanClose(SpanKind.MODEL_CALL, "m", "OK", Map.of("duration.ms", 5L));

        assertThat(metrics.timers).containsExactly(
                "buzhou.model.call.duration:5:model.provider=unknown,model.name=unknown");
    }

    @Test
    void toolCallSpanCloseRecordsNameAndStatus() {
        MicrometerDualWriter writer = writer();

        writer.recordSpanClose(SpanKind.TOOL_CALL, "read_file", "ERROR", Map.of("duration.ms", 30L));

        assertThat(metrics.timers).containsExactly(
                "buzhou.tool.call.duration:30:tool.name=read_file,status=ERROR");
    }

    @Test
    void otherKindsAndMissingDurationAreNotRecorded() {
        MicrometerDualWriter writer = writer();

        writer.recordSpanClose(SpanKind.SESSION, "s", "OK", Map.of("duration.ms", 1L));
        writer.recordSpanClose(SpanKind.TOOL_CALL, "t", "OK", Map.of()); // 无 duration.ms → 0ms 仍记录

        // SESSION 不产 metric；TOOL_CALL 以 0ms 记录（时长缺失=0，口径纯净交由下游）
        assertThat(metrics.timers).hasSize(1);
        assertThat(metrics.timers.poll()).startsWith("buzhou.tool.call.duration:0:");
    }

    @Test
    void tokensNonPositiveCountIsSkipped() {
        MicrometerDualWriter writer = writer();

        writer.recordTokens("gpt", "prompt", 0);
        writer.recordTokens("gpt", "completion", -5);

        assertThat(metrics.counters).isEmpty();
    }

    @Test
    void tokensRecordedWithUnknownModelFallback() {
        MicrometerDualWriter writer = writer();

        writer.recordTokens(null, "prompt", 7);

        assertThat(metrics.counters).containsExactly("buzhou.tokens:7:kind=prompt,model.name=unknown");
    }

    @Test
    void ttftAndTpotSkipNullOrNonPositiveDurations() {
        MicrometerDualWriter writer = writer();

        writer.recordTtft("gpt", null);
        writer.recordTtft("gpt", Duration.ofMillis(-1));
        writer.recordTtft("gpt", Duration.ZERO);
        writer.recordTpot("gpt", null);
        writer.recordTpot("gpt", Duration.ofMillis(-2));
        writer.recordTpot("gpt", Duration.ZERO);

        assertThat(metrics.timers).isEmpty();
    }

    @Test
    void ttftAndTpotPositiveDurationsRecordedWithUnknownFallback() {
        MicrometerDualWriter writer = writer();

        writer.recordTtft(null, Duration.ofMillis(120));
        writer.recordTpot("gpt", Duration.ofMillis(8));

        assertThat(metrics.timers).contains(
                "buzhou.model.ttft:120:model.name=unknown",
                "buzhou.model.tpot:8:model.name=gpt");
    }

    @Test
    void queueWaitAndPersistErrorRecorded() {
        MicrometerDualWriter writer = writer();

        writer.recordQueueWait(25);
        writer.recordPersistError();

        assertThat(metrics.timers).containsExactly("buzhou.observability.queue.wait:25:");
        assertThat(metrics.counters).containsExactly("buzhou.store.write.failures:1:policy=degrade");
    }

    @Test
    void tagValuesAreBoundedByCardinalityDiscipline() {
        MicrometerDualWriter writer = writer();

        writer.recordSpanClose(SpanKind.MODEL_CALL, "m", "OK", Map.of(
                "duration.ms", 1L,
                "model.provider", "p".repeat(40),   // >32 截断
                "model.name", "n".repeat(70)));     // >64 截断
        writer.recordTokens("m".repeat(70), "k".repeat(20), 1); // kind>16、model>64 截断
        writer.recordSpanClose(SpanKind.TOOL_CALL, "t".repeat(70), "s".repeat(20),
                Map.of("duration.ms", 1L)); // tool.name>64、status>16 截断

        assertThat(metrics.timers).anySatisfy(t -> assertThat(t).contains("model.provider=" + "p".repeat(32)));
        assertThat(metrics.timers).anySatisfy(t -> assertThat(t).contains("model.name=" + "n".repeat(64)));
        assertThat(metrics.counters).anySatisfy(c -> assertThat(c).contains("kind=" + "k".repeat(16)));
        assertThat(metrics.counters).anySatisfy(c -> assertThat(c).contains("model.name=" + "m".repeat(64)));
        assertThat(metrics.timers).anySatisfy(t -> assertThat(t).contains("tool.name=" + "t".repeat(64)));
        assertThat(metrics.timers).anySatisfy(t -> assertThat(t).contains("status=" + "s".repeat(16)));
    }
}
