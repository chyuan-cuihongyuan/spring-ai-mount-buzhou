package io.github.chyuan_cuihongyuan.buzhou.core.testsupport;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.CancellationToken;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 故障注入测试构件（spec 13 §cross-43 / impl-28；随 core test-jar 发布）：装饰
 * {@link ToolCallback} 的确定性故障源，供全 effort 韧性测试统一使用——延迟、失败率、
 * 永久挂起、资源泄漏、中途取消五类故障（{@link FaultSpec}），不再各测试自造假工具。
 *
 * <p>行为语义（按优先级）：
 *
 * <ol>
 *   <li>{@code hangForever}：永不返回且<b>不响应中断</b>（分片 sleep 吞掉 interrupt）——
 *       模拟最坏情况的挂死工具，专测 Deadline 兜底；命中后其余参数不评估；</li>
 *   <li>{@code leakResource}：打开一个流且故意不 close（计数入 {@link #leakedStreams()}），
 *       随后继续后续行为；</li>
 *   <li>{@code delayMs}：返回前延迟（可被中断，中断转为失败异常）；
 *       {@code cancelMidFlight} 时延迟改为分片轮询 {@link CancellationToken}，
 *       令牌置位即抛「cancelled mid-flight」异常（协作式取消的工具样本）；</li>
 *   <li>{@code failRate}：延迟后按<b>种子 Random</b> 决定成败（同种子 + 同调用序 →
 *       同成败序列，断言可复现）；命中抛 {@code IllegalStateException}。</li>
 * </ol>
 *
 * <p>调用计数：{@link #invocations()}；泄漏计数：{@link #leakedStreams()}。
 * hangForever 泄漏的线程为守护虚拟线程，随进程结束回收（测试期安全）。
 */
public final class FaultInjectingToolCallback implements ToolCallback {

    /** 默认工具名（同名单工具多实例时用 {@link #FaultInjectingToolCallback(String, FaultSpec)}）。 */
    public static final String DEFAULT_TOOL_NAME = "fault_injecting_tool";

    /** hangForever 的分片睡眠步长（吞中断、总时长无限）。 */
    private static final long HANG_SLICE_MILLIS = 1_000L;
    /** cancelMidFlight 的取消轮询步长。 */
    private static final long CANCEL_POLL_SLICE_MILLIS = 25L;
    /** 泄漏样本流的字节数（打开即持有、永不关闭）。 */
    private static final byte[] LEAK_PAYLOAD = "fault-injected leaked resource".getBytes(StandardCharsets.UTF_8);

    /**
     * 故障规格（全部确定性：{@code failRate} 由 {@code seed} 驱动）。
     *
     * @param delayMs         返回前延迟（毫秒，>=0）
     * @param failRate        失败概率（0-1；0 = 恒成功、1 = 恒失败）
     * @param hangForever     永久挂起且不响应中断（最高优先级，压测 Deadline 兜底）
     * @param leakResource    每次调用泄漏一个不关闭的流（计数可见）
     * @param cancelMidFlight 延迟期间轮询取消令牌，置位即抛「cancelled mid-flight」
     * @param seed            failRate 的随机种子（同种子同序列）
     */
    public record FaultSpec(long delayMs, double failRate, boolean hangForever,
                            boolean leakResource, boolean cancelMidFlight, long seed) {

        public FaultSpec {
            if (delayMs < 0) {
                throw new IllegalArgumentException("delayMs must be >= 0: " + delayMs);
            }
            if (failRate < 0D || failRate > 1D) {
                throw new IllegalArgumentException("failRate must be within [0,1]: " + failRate);
            }
        }

        /** 无故障（健康基线：立即成功）。 */
        public static FaultSpec none() {
            return new FaultSpec(0L, 0D, false, false, false, 0L);
        }

        /** 固定延迟。 */
        public static FaultSpec delayOf(long delayMs) {
            return new FaultSpec(delayMs, 0D, false, false, false, 0L);
        }

        /** 按失败率随机失败（种子确定 → 序列确定）。 */
        public static FaultSpec failRate(double rate, long seed) {
            return new FaultSpec(0L, rate, false, false, false, seed);
        }

        /** 永久挂起且不响应中断（工厂名避开与 record 组件 {@code hangForever} 的访问器签名冲突）。 */
        public static FaultSpec hanging() {
            return new FaultSpec(0L, 0D, true, false, false, 0L);
        }

        /** 每次调用泄漏一个流。 */
        public static FaultSpec leaking() {
            return new FaultSpec(0L, 0D, false, true, false, 0L);
        }

        /** 延迟期间协作响应取消令牌。 */
        public static FaultSpec cancelAware(long delayMs) {
            return new FaultSpec(delayMs, 0D, false, false, true, 0L);
        }
    }

    private final String name;
    private final FaultSpec spec;
    private final Random random;
    private final AtomicInteger invocations = new AtomicInteger();
    private final AtomicLong leakedStreams = new AtomicLong();

    public FaultInjectingToolCallback(FaultSpec spec) {
        this(DEFAULT_TOOL_NAME, spec);
    }

    public FaultInjectingToolCallback(String name, FaultSpec spec) {
        this.name = java.util.Objects.requireNonNull(name, "name");
        this.spec = java.util.Objects.requireNonNull(spec, "spec");
        this.random = new Random(spec.seed());
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder().name(name)
                .description("故障注入测试工具（delay/failRate/hangForever/leakResource/cancelMidFlight）")
                .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        int sequence = invocations.incrementAndGet();
        if (spec.hangForever()) {
            hangIgnoringInterruption();
        }
        if (spec.leakResource()) {
            leakStreamAndNeverClose();
        }
        if (spec.delayMs() > 0L) {
            try {
                if (spec.cancelMidFlight()) {
                    awaitWithCancelPolling(toolContext);
                } else {
                    Thread.sleep(spec.delayMs());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("fault-injected: interrupted during delay #" + sequence);
            }
        }
        if (random.nextDouble() < spec.failRate()) {
            throw new IllegalStateException("fault-injected failure #" + sequence);
        }
        return name + " ok #" + sequence;
    }

    /** 累计调用次数。 */
    public int invocations() {
        return invocations.get();
    }

    /** 累计泄漏的流数量（仅 leakResource 生效时递增）。 */
    public long leakedStreams() {
        return leakedStreams.get();
    }

    /** 永久挂起：分片睡眠并吞掉中断——模拟不可中断的挂死工具。 */
    private static void hangIgnoringInterruption() {
        while (true) {
            try {
                Thread.sleep(HANG_SLICE_MILLIS);
            } catch (InterruptedException e) {
                // 故意忽略中断：压测「外层必须靠 Deadline 兜底」的最坏样本
                Thread.currentThread().interrupt();
            }
        }
    }

    /** 打开流且永不 close（泄漏计数可见；样本小、GC 可回收内容，检测靠计数而非 OOM）。 */
    private void leakStreamAndNeverClose() {
        InputStream leaked = new ByteArrayInputStream(LEAK_PAYLOAD);
        leakedStreams.incrementAndGet();
        // 故意不 close——泄漏语义本体（静态分析可据此识别未关闭资源）
    }

    /** 分片延迟并在片间轮询取消令牌：置位即抛「cancelled mid-flight」（协作式取消样本）。 */
    private void awaitWithCancelPolling(ToolContext toolContext) throws InterruptedException {
        CancellationToken token = CancellationToken.from(toolContext);
        long waited = 0L;
        while (waited < spec.delayMs()) {
            if (token.isCancelled()) {
                throw new IllegalStateException(
                        "fault-injected: cancelled mid-flight after " + waited + "ms");
            }
            Thread.sleep(CANCEL_POLL_SLICE_MILLIS);
            waited += CANCEL_POLL_SLICE_MILLIS;
        }
    }
}
