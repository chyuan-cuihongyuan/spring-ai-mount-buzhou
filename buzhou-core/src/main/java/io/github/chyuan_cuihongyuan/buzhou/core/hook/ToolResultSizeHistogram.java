package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 工具结果字节直方分桶读面（spec 1401 / T2103 / impl 1054）——Prometheus
 * histogram（le 桶 + 累计计数）思想：工具执行产物按 UTF-8 字节落入固定
 * 幂次边界桶，溢出桶收尾；分布形状（结果普遍是玩具级短串还是占满预算的
 * 大块头）从桶占比直接读出，回喂模型的 token 预算压力有据可查。
 *
 * <p>opt-in Hook：经 {@code RuntimeConfig} hooks 注册后生效，未注册零开销；
 * {@link #afterTool} 只读不裁决（返回 CONTINUE，对链零影响）。字节口径与
 * ReadFileStats/WriteFileStats（J 会话 R46–R47）一致：UTF-8 编码字节。
 *
 * <p>守恒式：{@code successes = b0 + b1 + b2 + b3 + b4 + overflow}；
 * {@code executed = successes + failed}（error 路径产物是错误反馈文案，
 * 不入字节分布）。测量点为 hook 链改写前的原始执行产物。
 */
public final class ToolResultSizeHistogram implements BuzhouHook {

    /** 幂次边界（字节）：256 / 1K / 4K / 16K / 64K，第 6 桶为溢出。 */
    static final int[] BOUNDARIES = {256, 1024, 4096, 16384, 65536};

    private final AtomicLong executed = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong totalBytes = new AtomicLong();
    private final AtomicLong[] buckets = {
            new AtomicLong(), new AtomicLong(), new AtomicLong(),
            new AtomicLong(), new AtomicLong(), new AtomicLong()};

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        executed.incrementAndGet();
        if (ctx.error() != null) {
            failed.incrementAndGet();
            return HookResult.CONTINUE;
        }
        Object result = ctx.result();
        int bytes = result == null ? 0
                : String.valueOf(result).getBytes(StandardCharsets.UTF_8).length;
        totalBytes.addAndGet(bytes);
        buckets[bucketIndex(bytes)].incrementAndGet();
        return HookResult.CONTINUE;
    }

    private static int bucketIndex(int bytes) {
        for (int i = 0; i < BOUNDARIES.length; i++) {
            if (bytes < BOUNDARIES[i]) {
                return i;
            }
        }
        return BOUNDARIES.length;
    }

    /** 只读快照：六桶 + 三总量（守恒式见类注）。 */
    public Snapshot stats() {
        return new Snapshot(buckets[0].get(), buckets[1].get(), buckets[2].get(),
                buckets[3].get(), buckets[4].get(), buckets[5].get(),
                executed.get(), failed.get(), totalBytes.get());
    }

    /** 测试归零口（装配级实例的 reset 注入点）。 */
    public void resetForTest() {
        executed.set(0);
        failed.set(0);
        totalBytes.set(0);
        for (AtomicLong bucket : buckets) {
            bucket.set(0);
        }
    }

    /**
     * @param b0       &lt;256B 桶计数
     * @param b1       &lt;1KB 桶计数
     * @param b2       &lt;4KB 桶计数
     * @param b3       &lt;16KB 桶计数
     * @param b4       &lt;64KB 桶计数
     * @param overflow ≥64KB 溢出桶计数
     * @param executed 累计执行（含失败）
     * @param failed   累计失败（错误反馈文案不计入字节分布）
     * @param totalBytes 成功产物 UTF-8 字节精确累计（均值 = totalBytes/successes）
     */
    public record Snapshot(long b0, long b1, long b2, long b3, long b4, long overflow,
                           long executed, long failed, long totalBytes) {

        /** 成功产物数（= 六桶之和）。 */
        public long successes() {
            return b0 + b1 + b2 + b3 + b4 + overflow;
        }
    }
}
