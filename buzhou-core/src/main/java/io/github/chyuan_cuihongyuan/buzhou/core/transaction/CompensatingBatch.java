package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.UnitOfWork;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 事务批补偿（spec 304 / T599，saga 模式——Seata 长事务补偿族借鉴）：顺序步 +
 * 逐步补偿的跨步多写回退面。每步在 {@link UnitOfWork} 事务内执行；任一步失败 →
 * 已成步<b>倒序补偿</b>（每步补偿在自身事务内）后原异常上抛。
 *
 * <p><b>补偿失败即停止回退</b>（saga 经典语义）：更早步的补偿假设后者已成功——
 * 补偿失败时停止回退、log ERROR + 计数、原异常仍上抛（人工介入，证据留档：
 * 未补偿步即断点）。观测：{@code buzhou.saga.compensated{step}} /
 * {@code buzhou.saga.compensation-failed{step}}。
 *
 * @param <T> 步动作结果类型（补偿接收本步结果）
 */
public final class CompensatingBatch {

    private static final String COUNTER_COMPENSATED = "buzhou.saga.compensated";
    private static final String COUNTER_COMPENSATION_FAILED = "buzhou.saga.compensation-failed";

    // spec 1443 / T2183：saga 运行静态读数（进程级先例——metrics counter 之外的
    // 聚合面：成功率/补偿触发率/断点步名）。resetForTest 为归零注入点。
    private static final java.util.concurrent.atomic.AtomicLong RUNS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong SUCCESSES =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong COMPENSATION_RUNS =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong COMPENSATION_FAILURES =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong STEPS_EXECUTED =
            new java.util.concurrent.atomic.AtomicLong();
    private static volatile String lastFailedStep = null;
    private static volatile String currentStepName = null;

    /** 只读快照：saga 运行漏斗（runs=successes+compensationRuns 守恒）。 */
    public static SagaStats sagaStats() {
        return new SagaStats(RUNS.get(), SUCCESSES.get(), COMPENSATION_RUNS.get(),
                COMPENSATION_FAILURES.get(), STEPS_EXECUTED.get(), lastFailedStep);
    }

    /** 测试归零口。 */
    public static void resetSagaStatsForTest() {
        RUNS.set(0);
        SUCCESSES.set(0);
        COMPENSATION_RUNS.set(0);
        COMPENSATION_FAILURES.set(0);
        STEPS_EXECUTED.set(0);
        lastFailedStep = null;
        currentStepName = null;
    }

    /**
     * @param runs                  累计 saga 运行数
     * @param successes             全步成功数
     * @param compensationRuns      触发补偿的运行数
     * @param compensationFailures  补偿自身失败数（人工介入断点）
     * @param stepsExecuted         累计执行步数
     * @param lastFailedStep        末次失败步名（null = 从未失败）
     */
    public record SagaStats(long runs, long successes, long compensationRuns,
                            long compensationFailures, long stepsExecuted,
                            String lastFailedStep) {

        /** 守恒式：runs = successes + compensationRuns。 */
        public boolean conserved() {
            return runs == successes + compensationRuns;
        }
    }
    private static final System.Logger LOGGER =
            System.getLogger(CompensatingBatch.class.getName());

    private CompensatingBatch() {
    }

    /** saga 步：命名动作 + 接收本步结果的补偿。 */
    public record Step<T>(String name, Supplier<T> action, Consumer<T> compensation) {

        /**
         * 构造一步。
         *
         * @param name          步名（观测/断点报告用）
         * @param action        步动作（在事务内执行）
         * @param compensation  补偿（接收本步结果；null = 本步无需补偿）
         */
        public static <T> Step<T> of(String name, Supplier<T> action, Consumer<T> compensation) {
            if (name == null || name.isBlank() || action == null) {
                throw new IllegalArgumentException("name/action 必须非空（step=" + name + "）");
            }
            return new Step<>(name, action, compensation);
        }
    }

    /** 已成步 + 其结果（补偿入参）。 */
    private record Completed(Step<?> step, Object result) {
    }

    /**
     * 顺序执行全部步；返回最后一步结果。任一步失败：已成步倒序补偿（补偿失败
     * 即停止回退）后原异常上抛。
     *
     * @param uow   事务承载（每步/每补偿各自一个事务）
     * @param steps 有序步集（空 = 直接返回 null）
     */
    @SuppressWarnings("unchecked")
    public static <T> T run(UnitOfWork uow, List<Step<?>> steps) {
        return run(uow, null, steps);
    }

    /**
     * spec 623 / T896：per-session 事务域重载——步集在 {@code sessionId} 的会话级锁内
     * 执行（UnitOfWork SPI 已有 per-session 重载；无参版走全局锁是吞吐瓶颈——归档族
     * 跨会话本可并行）。sessionId null = 全局域（既有语义）；补偿步与正向步同域
     * （同锁序，防补偿与在途正向交错）。
     */
    @SuppressWarnings("unchecked")
    public static <T> T run(UnitOfWork uow, String sessionId, List<Step<?>> steps) {
        if (uow == null) {
            throw new IllegalArgumentException("uow 必须非空");
        }
        List<Completed> completed = new ArrayList<>();
        Object last = null;
        RUNS.incrementAndGet();
        try {
            for (Step<?> step : steps) {
                currentStepName = step.name();
                last = inTransaction(uow, sessionId, step.action());
                completed.add(new Completed(step, last));
                STEPS_EXECUTED.incrementAndGet();
            }
            SUCCESSES.incrementAndGet();
        } catch (RuntimeException e) {
            COMPENSATION_RUNS.incrementAndGet();
            lastFailedStep = currentStepName;
            unwind(uow, sessionId, completed);
            throw e;
        }
        return (T) last;
    }

    private static <T> T inTransaction(UnitOfWork uow, String sessionId,
            java.util.function.Supplier<T> action) {
        return sessionId == null
                ? uow.executeInTransaction(action)
                : uow.executeInTransaction(sessionId, action);
    }

    /** 倒序补偿；补偿失败即停止（更早步不补偿——人工介入断点）。 */
    private static void unwind(UnitOfWork uow, String sessionId, List<Completed> completed) {
        for (int i = completed.size() - 1; i >= 0; i--) {
            Completed done = completed.get(i);
            if (done.step().compensation() == null) {
                continue;
            }
            try {
                inTransaction(uow, sessionId, () -> {
                    compensate(done);
                    return null;
                });
                BuzhouMetricsHolder.metrics().counter(COUNTER_COMPENSATED, 1,
                        "step", done.step().name());
            } catch (RuntimeException compFailure) {
                COMPENSATION_FAILURES.incrementAndGet();
                BuzhouMetricsHolder.metrics().counter(COUNTER_COMPENSATION_FAILED, 1,
                        "step", done.step().name());
                LOGGER.log(System.Logger.Level.ERROR,
                        "saga 补偿失败，停止回退（人工介入断点：step={0}，未补偿步={1}）",
                        done.step().name(), remainingNames(completed, i), compFailure);
                return;
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void compensate(Completed done) {
        ((Consumer) done.step().compensation()).accept(done.result());
    }

    private static List<String> remainingNames(List<Completed> completed, int fromIndex) {
        List<String> names = new ArrayList<>();
        for (int j = fromIndex; j >= 0; j--) {
            names.add(completed.get(j).step().name());
        }
        return names;
    }
}
