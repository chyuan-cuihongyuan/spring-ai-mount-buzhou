package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.HexFormat;

/**
 * 影子读探针（spec 189 / T561，Istio mirror 借鉴）：确定性采样（sha256(key)%100
 * &lt; rate——同 key 稳定命中）下把主路已返回结果与影子调用<b>异步旁路对照</b>
 * （文本等值判断），agreed/diverged 计数 + 最近分歧样本环形 32；影子异常全吞
 * （旁路永不影响主路）；未命中采样零执行零成本。
 */
public final class ShadowProbe {

    /** 观测面：四计数 + 最近分歧样本（稳定序，新→旧）。 */
    public record Snapshot(long sampled, long agreed, long diverged, long errors,
                           List<String> recentDivergedKeys) {
    }

    private static final int SAMPLE_RING = 32;

    private final int ratePercent;
    private final AtomicLong sampled = new AtomicLong();
    private final AtomicLong agreed = new AtomicLong();
    private final AtomicLong diverged = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final Deque<String> recentDiverged = new ArrayDeque<>();

    public ShadowProbe(int ratePercent) {
        if (ratePercent < 0 || ratePercent > 100) {
            throw new IllegalArgumentException("ratePercent∈[0,100]（当前 " + ratePercent + "）");
        }
        this.ratePercent = ratePercent;
    }

    /** 确定性采样判定（同 key 同判定）。 */
    public boolean sampled(String key) {
        if (ratePercent == 0 || key == null) {
            return false;
        }
        if (ratePercent == 100) {
            return true;
        }
        return (sha256Positive(key) % 100) < ratePercent;
    }

    /**
     * 旁路对照：命中采样才异步执行影子并对照（submit 即忘——异常吞计 error）；
     * 未命中零执行。主路结果只读不改。
     */
    public void probe(String key, String primaryResult,
                      Callable<String> shadowCall, ExecutorService executor) {
        if (executor == null || shadowCall == null || !sampled(key)) {
            return;
        }
        sampled.incrementAndGet();
        executor.submit(() -> {
            try {
                String shadowResult = shadowCall.call();
                if (primaryResult == null ? shadowResult == null
                        : primaryResult.equals(shadowResult)) {
                    agreed.incrementAndGet();
                } else {
                    diverged.incrementAndGet();
                    synchronized (recentDiverged) {
                        recentDiverged.addFirst(key);
                        while (recentDiverged.size() > SAMPLE_RING) {
                            recentDiverged.removeLast();
                        }
                    }
                }
            } catch (RuntimeException e) {
                errors.incrementAndGet(); // 旁路异常吞——主路无感
            } catch (Exception e) {
                errors.incrementAndGet();
            }
        });
    }

    /** 观测快照。 */
    public Snapshot snapshot() {
        synchronized (recentDiverged) {
            return new Snapshot(sampled.get(), agreed.get(), diverged.get(), errors.get(),
                    List.copyOf(recentDiverged));
        }
    }

    private static long sha256Positive(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return ((hash[0] & 0x7FL) << 56) | (hash[1] & 0xFFL) << 48
                    | (hash[2] & 0xFFL) << 40 | (hash[3] & 0xFFL) << 32
                    | (hash[4] & 0xFFL) << 24 | (hash[5] & 0xFFL) << 16
                    | (hash[6] & 0xFFL) << 8 | (hash[7] & 0xFFL);
        } catch (Exception e) {
            return HexFormat.of().formatHex(
                    key.getBytes(StandardCharsets.UTF_8)).hashCode() & 0x7FFFFFFFL;
        }
    }
}
