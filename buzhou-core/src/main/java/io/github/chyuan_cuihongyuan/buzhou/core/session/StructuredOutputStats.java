package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 结构化输出 REASK 读数（spec 1413 / T2127 / impl 1066）——Instructor
 * max_retries 的可观测面思想：{@code chatForEntity} 的「首解析→REASK→再解析→
 * 或失败」漏斗全程埋点，模型合规率从「感觉经常 reask」变漏斗显形。
 *
 * <p>进程级静态读面（ToolArgsValidator.validationStats 同款先例——埋点在
 * internal 主路径，读面归公共类）：双守恒式
 * {@code attempts = firstPassParsed + reaskParsed + failures}、
 * {@code reasks = reaskParsed + failures}；reask 恰一次（既有语义，REASK 后
 * 不再二次重问）。{@link #resetForTest()} 为归零注入点。
 */
public final class StructuredOutputStats {

    private static final AtomicLong ATTEMPTS = new AtomicLong();
    private static final AtomicLong FIRST_PASS_PARSED = new AtomicLong();
    private static final AtomicLong REASKS = new AtomicLong();
    private static final AtomicLong REASK_PARSED = new AtomicLong();
    private static final AtomicLong FAILURES = new AtomicLong();

    private StructuredOutputStats() {
    }

    public static void recordAttempt() {
        ATTEMPTS.incrementAndGet();
    }

    public static void recordFirstPassParsed() {
        FIRST_PASS_PARSED.incrementAndGet();
    }

    public static void recordReask() {
        REASKS.incrementAndGet();
    }

    public static void recordReaskParsed() {
        REASK_PARSED.incrementAndGet();
    }

    public static void recordFailure() {
        FAILURES.incrementAndGet();
    }

    /** 只读快照：漏斗五计数（双守恒式见类注）。 */
    public static Snapshot stats() {
        return new Snapshot(ATTEMPTS.get(), FIRST_PASS_PARSED.get(),
                REASKS.get(), REASK_PARSED.get(), FAILURES.get());
    }

    /** 测试归零口：静态读数的 reset 注入点。 */
    public static void resetForTest() {
        ATTEMPTS.set(0);
        FIRST_PASS_PARSED.set(0);
        REASKS.set(0);
        REASK_PARSED.set(0);
        FAILURES.set(0);
    }

    /**
     * @param attempts        累计结构化输出调用数（首解析起点）
     * @param firstPassParsed 首轮即合规解析数
     * @param reasks          REASK 触发数（= reaskParsed + failures）
     * @param reaskParsed     REASK 后合规解析数
     * @param failures        REASK 后仍失败（StructuredOutputException）数
     */
    public record Snapshot(long attempts, long firstPassParsed, long reasks,
                           long reaskParsed, long failures) {

        /** 模型首过合规率（attempts=0 哨兵 -1）。 */
        public double firstPassRate() {
            return attempts == 0 ? -1d : (double) firstPassParsed / attempts;
        }
    }
}
