package io.github.chyuan_cuihongyuan.buzhou.examples.perf;

import io.github.chyuan_cuihongyuan.buzhou.resilience.cache.SemanticCacheStore;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.time.Duration;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * effort#15 / spec 55 §E / T245 / impl-194：语义缓存哨兵（@Tag("perf")，nightly 激活）。
 * 口径：stub 嵌入（确定性哈希向量——真实嵌入延迟归 provider 另计，baseline 标注）；
 * 128 条满桶 × 384 维全量 cosine 扫描 = 查询路径的进程内开销本体。
 * 哨兵硬顶 10 倍宽幅（预期微秒~毫秒级）：越顶 = 量级回归（扫描复杂度劣化），人工 profiling。
 */
@Tag("perf")
class PerfEffort15SentinelsTest {

    /** 满桶最近邻查询 P95 硬顶 ms（预期 <1ms，10 倍宽幅）。 */
    private static final double FULL_BUCKET_P95_MAX_MILLIS = 10;

    @Test
    void fullBucketNearestNeighborP95() {
        SemanticCacheStore store = new SemanticCacheStore(128, Duration.ofHours(1), 0.9);
        // 填满 128 条（同一桶，384 维确定性伪随机向量）
        java.util.Random rng = new java.util.Random(42);
        for (int i = 0; i < 128; i++) {
            store.put("model=perf", vectorOf(rng), responseOf("答案-" + i));
        }
        long[] nanos = new long[200];
        for (int i = 0; i < nanos.length; i++) {
            float[] query = vectorOf(rng);
            long start = System.nanoTime();
            store.findNearest("model=perf", query);
            nanos[i] = System.nanoTime() - start;
        }
        Arrays.sort(nanos);
        double p95 = nanos[(int) Math.round(0.95 * (nanos.length - 1))] / 1e6;
        System.out.printf("[perf] semantic full-bucket(128x384) nearest: p95=%.3fms (哨兵 < %.0fms)%n",
                p95, FULL_BUCKET_P95_MAX_MILLIS);
        assertThat(p95).as("语义满桶查询 10 倍级回归哨兵").isLessThan(FULL_BUCKET_P95_MAX_MILLIS);
    }

    /** 确定性 384 维单位向量（哈希种子驱动——stub 嵌入口径）。 */
    private static float[] vectorOf(java.util.Random rng) {
        float[] v = new float[384];
        float norm = 0;
        for (int i = 0; i < v.length; i++) {
            v[i] = rng.nextFloat() - 0.5f;
            norm += v[i] * v[i];
        }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < v.length; i++) {
            v[i] /= norm;
        }
        return v;
    }

    private static ChatResponse responseOf(String text) {
        return new ChatResponse(java.util.List.of(new Generation(new AssistantMessage(text))));
    }
}
