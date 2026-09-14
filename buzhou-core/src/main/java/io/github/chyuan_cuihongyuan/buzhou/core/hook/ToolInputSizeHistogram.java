package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 工具入参字节直方分桶读面（spec 1412 / T2125 / impl 1065）——spec 1401
 * 结果侧的对称镜像（Datadog DogStatsD read/write 对称 tag 思想）：模型发来的
 * arguments 经 Jackson 序列化为 UTF-8 JSON 字节落固定幂次边界桶，超长入参
 * 吃上下文预算、误构造的巨大调用从不可见变分布显形。
 *
 * <p>opt-in Hook：经 {@code RuntimeConfig} hooks 注册后生效；{@link #beforeTool}
 * 只读不裁决（返回 CONTINUE，对链零影响）。**成本口径**：钩内自行序列化
 * arguments（与 HookedToolCallback 的序列化各一次）——仅注册本 hook 的宿主
 * 承担，未注册零开销。测量点为链前原始入参（{@code replaceArguments} 改写之前）。
 *
 * <p>守恒式：{@code executed = b0 + b1 + b2 + b3 + b4 + overflow}。
 */
public final class ToolInputSizeHistogram implements BuzhouHook {

    /** 幂次边界（字节）：256 / 1K / 4K / 16K / 64K，第 6 桶为溢出（与 1401 同边界）。 */
    static final int[] BOUNDARIES = {256, 1024, 4096, 16384, 65536};

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AtomicLong executed = new AtomicLong();
    private final AtomicLong totalBytes = new AtomicLong();
    private final AtomicLong[] buckets = {
            new AtomicLong(), new AtomicLong(), new AtomicLong(),
            new AtomicLong(), new AtomicLong(), new AtomicLong()};

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        int bytes;
        try {
            bytes = MAPPER.writeValueAsBytes(
                    ctx.arguments() == null ? java.util.Map.of() : ctx.arguments()).length;
        } catch (Exception e) {
            bytes = 0; // 序列化失败按 0 字节入最低桶（不裁决不中断链）
        }
        executed.incrementAndGet();
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

    /** 只读快照：六桶 + 两总量（守恒式见类注）。 */
    public Snapshot stats() {
        return new Snapshot(buckets[0].get(), buckets[1].get(), buckets[2].get(),
                buckets[3].get(), buckets[4].get(), buckets[5].get(),
                executed.get(), totalBytes.get());
    }

    /** 测试归零口（装配级实例的 reset 注入点）。 */
    public void resetForTest() {
        executed.set(0);
        totalBytes.set(0);
        for (AtomicLong bucket : buckets) {
            bucket.set(0);
        }
    }

    /**
     * @param b0         &lt;256B 桶计数
     * @param b1         &lt;1KB 桶计数
     * @param b2         &lt;4KB 桶计数
     * @param b3         &lt;16KB 桶计数
     * @param b4         &lt;64KB 桶计数
     * @param overflow   ≥64KB 溢出桶计数
     * @param executed   累计入参调用数
     * @param totalBytes 入参 JSON UTF-8 字节精确累计（均值 = totalBytes/executed）
     */
    public record Snapshot(long b0, long b1, long b2, long b3, long b4, long overflow,
                           long executed, long totalBytes) {

        /** 桶计数之和（= executed，守恒可核对）。 */
        public long bucketSum() {
            return b0 + b1 + b2 + b3 + b4 + overflow;
        }
    }
}
