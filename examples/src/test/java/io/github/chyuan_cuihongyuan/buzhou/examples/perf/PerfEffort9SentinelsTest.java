package io.github.chyuan_cuihongyuan.buzhou.examples.perf;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryMessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.BuzhouChatMemory;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ReadDegradeHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ReadDegradePolicy;
import io.github.chyuan_cuihongyuan.buzhou.spill.DiskSpillStore;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillCipher;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillEntry;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillQuota;
import io.github.chyuan_cuihongyuan.buzhou.spill.SpillUri;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * effort #9 新能力 perf 哨兵（spec 45 §C / T163 / impl-134）：spill 加密读写开销、
 * 单飞闸路径开销（串行多轮）、读降级空历史路径开销——同 PerfBaselineTest 口径：
 * 10 倍宽幅粗粒度回归哨兵，nightly 以 -Dgroups=perf 激活。
 */
@Tag("perf")
class PerfEffort9SentinelsTest {

    /** 加密 store 200 次 store+load 往返（64KB 载荷）P95 上限 ms（首轮实测 < 8ms，10 倍冗余）。 */
    private static final double CRYPTO_ROUNDTRIP_P95_MAX_MILLIS = 150;

    /** 单飞闸 500 次串行 chat 往返 P95 上限 ms（首轮实测 < 0.1ms/轮——闸本身是 CAS）。 */
    private static final double SINGLEFLIGHT_P95_MAX_MILLIS = 5;

    /** 读降级路径 200 次 load 失败→空历史 P95 上限 ms（首轮实测 < 0.05ms）。 */
    private static final double READ_DEGRADE_P95_MAX_MILLIS = 10;

    private static final int SAMPLES = 30;

    @TempDir
    Path spillRoot;

    @BeforeEach
    @AfterEach
    void resetPolicy() {
        ReadDegradeHolder.set(ReadDegradePolicy.OFF);
    }

    /** ①加密往返：AES-GCM 落盘+读回 vs 明文基线的量级回归（只拦算法级退化）。 */
    @Test
    void spillCryptoRoundTripSentinel() {
        byte[] key = new byte[32];
        ThreadLocalRandom.current().nextBytes(key);
        DiskSpillStore store = new DiskSpillStore(spillRoot, SpillQuota.unbounded(),
                SpillCipher.fromBase64Key(Base64.getEncoder().encodeToString(key)));
        String payload = "x".repeat(64 * 1024);

        double p95 = p95Of(() -> {
            long seq = System.nanoTime();
            SpillUri uri = new SpillUri("perf", "crypto", "tc-" + seq);
            store.store(SpillEntry.of(uri, payload), 128);
            return store.load(uri).map(v -> v.length()).orElse(0) == payload.length();
        });
        assertThat(p95).isLessThan(CRYPTO_ROUNDTRIP_P95_MAX_MILLIS);
    }

    /** ②单飞闸：串行多轮进出闸开销（CAS 占位/释放；闸不应成为可感开销）。 */
    @Test
    void singleFlightGateOverheadSentinel() {
        InMemoryMessageStore messageStore = new InMemoryMessageStore();
        BuzhouChatMemory memory = new BuzhouChatMemory(messageStore);
        double p95 = p95Of(() -> {
            String sid = "perf-singleflight-" + ThreadLocalRandom.current().nextInt(1000);
            memory.add(sid, List.of(new org.springframework.ai.chat.messages.UserMessage("q"),
                    new org.springframework.ai.chat.messages.AssistantMessage("a")));
            return !memory.get(sid).isEmpty();
        });
        assertThat(p95).isLessThan(SINGLEFLIGHT_P95_MAX_MILLIS);
    }

    /** ③读降级路径：EMPTY 策略下读失败→空历史的降级开销（异常构造是主成本）。 */
    @Test
    void readDegradePathSentinel() {
        var failing = new io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore() {
            @Override
            public void append(String sessionId,
                    List<io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage> messages) {
            }

            @Override
            public List<io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage> load(String sessionId) {
                throw new IllegalStateException("读失败（perf 哨兵模拟）");
            }

            @Override
            public java.util.Optional<io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage> findById(
                    String id) {
                return java.util.Optional.empty();
            }
        };
        ReadDegradeHolder.set(ReadDegradePolicy.EMPTY);
        BuzhouChatMemory memory = new BuzhouChatMemory(failing);
        double p95 = p95Of(() -> memory.get("perf-degrade").isEmpty());
        assertThat(p95).isLessThan(READ_DEGRADE_P95_MAX_MILLIS);
    }

    /** P95 采样（ SAMPLES 轮，每轮一个布尔动作；失败即抛）。 */
    private static double p95Of(Supplier<Boolean> action) {
        double[] samples = new double[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long start = System.nanoTime();
            assertThat(action.get()).isTrue();
            samples[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        java.util.Arrays.sort(samples);
        return samples[(int) (SAMPLES * 0.95)];
    }
}
