package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 会话关闭耗时读数（spec 1430 / T2163 / impl 1084）——k8s graceful shutdown
 * terminationGracePeriod 思想：关闭耗时分布是排空健康的第一信号——close 卡在
 * executor 排空/observer onClose/资源注册表逆序关闭会让停机窗口超限。
 * 进程级静态读面（ToolArgsValidator.validationStats 同款先例）：
 * DefaultAgentSession.close() 埋点（清理优先/异常聚合/幂等语义逐位不变，
 * 只增记账），{@link #resetForTest()} 归零注入点。
 */
public final class SessionCloseStats {

    private static final AtomicLong CLOSED = new AtomicLong();
    private static final AtomicLong CLOSE_FAILURES = new AtomicLong();
    private static final AtomicLong LAST_DURATION_MILLIS = new AtomicLong();
    private static final AtomicLong MAX_DURATION_MILLIS = new AtomicLong();

    private SessionCloseStats() {
    }

    /** DefaultAgentSession.close() 埋点：一次完整关闭的耗时（毫秒）。 */
    public static void recordClose(long durationMillis) {
        LAST_DURATION_MILLIS.set(durationMillis);
        MAX_DURATION_MILLIS.accumulateAndGet(durationMillis, Math::max);
        CLOSED.incrementAndGet();
    }

    /** DefaultAgentSession.close() 埋点：本次关闭收集到清理失败（逐 observer 隔离后首失败上抛前）。 */
    public static void recordCloseFailure() {
        CLOSE_FAILURES.incrementAndGet();
    }

    /** 只读快照：成功关闭数/失败数/末次与最长耗时。 */
    public static Snapshot stats() {
        return new Snapshot(CLOSED.get(), CLOSE_FAILURES.get(),
                LAST_DURATION_MILLIS.get(), MAX_DURATION_MILLIS.get());
    }

    /** 测试归零口：静态读数的 reset 注入点。 */
    public static void resetForTest() {
        CLOSED.set(0);
        CLOSE_FAILURES.set(0);
        LAST_DURATION_MILLIS.set(0);
        MAX_DURATION_MILLIS.set(0);
    }

    /**
     * @param closed                累计关闭会话数
     * @param closeFailures         收集到清理失败的关闭数
     * @param lastCloseDurationMillis 末次关闭耗时（毫秒）
     * @param maxCloseDurationMillis  历史最长关闭耗时（毫秒水位，单调不回退）
     */
    public record Snapshot(long closed, long closeFailures,
                           long lastCloseDurationMillis, long maxCloseDurationMillis) {
    }
}
