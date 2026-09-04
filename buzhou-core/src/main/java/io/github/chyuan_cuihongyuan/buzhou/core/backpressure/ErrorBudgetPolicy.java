package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import io.github.chyuan_cuihongyuan.buzhou.core.health.ErrorBudget;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 错误预算政策（spec 335 / T662，Google SRE error budget policy 借鉴）：
 * 预算烧穿 = 冻结低优先级工作（用可靠性换稳定），预算回充才解冻——
 * 预算治理从「知道」（321 观察/告警）走到「行动」。
 *
 * <p>周期评估 {@link ErrorBudget#anyBreaching()}（任一 scope 烧穿即全局
 * 冻结——保守）：烧穿 → 地板抬 {@link SpawnPriority#HIGH}（NORMAL/LOW
 * spawn 即拒——SRE 冻结期只保关键租户/运维接管通道）；<b>连续两轮</b>
 * 评估清明 → 解冻回 LOW（防抖——单轮清明不立即解冻，边界抖动不振荡）。
 * 冻结/解冻 WARN + 计数；SmartLifecycle 自管虚拟线程（fixed-period
 * interruptible sleep）。
 */
public final class ErrorBudgetPolicy implements SmartLifecycle {

    private static final System.Logger LOGGER =
            System.getLogger(ErrorBudgetPolicy.class.getName());

    /** 解冻防抖：连续清明轮数。 */
    static final int UNFREEZE_AFTER_CLEAR_ROUNDS = 2;

    private final ErrorBudget budget;
    private final SpawnAdmissionFloor floor;
    private final Duration interval;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean frozen = new AtomicBoolean(false);
    private volatile Instant frozenSince;
    private final AtomicLong evaluations = new AtomicLong();
    private volatile int clearStreak;
    private volatile Thread worker;

    public ErrorBudgetPolicy(ErrorBudget budget, SpawnAdmissionFloor floor, Duration interval) {
        this.budget = java.util.Objects.requireNonNull(budget, "budget 非空——无观察面的政策是盲动");
        this.floor = java.util.Objects.requireNonNull(floor);
        this.interval = interval == null || interval.isZero() || interval.isNegative()
                ? Duration.ofSeconds(15) : interval;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            worker = io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory
                    .virtual("error-budget-policy").newThread(this::run);
            worker.start();
        }
    }

    private void run() {
        while (running.get()) {
            try {
                evaluate();
                Thread.sleep(interval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // stop() 打断——退出
                return;
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.WARNING, "错误预算政策评估异常（继续轮询）", e);
            }
        }
    }

    /** 单轮评估（调度器周期调；测试/运维可手动）。 */
    public void evaluate() {
        evaluations.incrementAndGet();
        boolean breaching = budget.anyBreaching();
        if (breaching) {
            clearStreak = 0;
            if (frozen.compareAndSet(false, true)) {
                frozenSince = Instant.now();
                floor.set(SpawnPriority.HIGH);
                io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder
                        .metrics().counter("buzhou.errorbudget.freeze", 1);
                LOGGER.log(System.Logger.Level.WARNING,
                        "错误预算烧穿——spawn 准入地板抬 HIGH（冻结 NORMAL/LOW；scope 清单={0}）",
                        budget.topBreaching(3));
            }
            return;
        }
        if (!frozen.get()) {
            return;
        }
        clearStreak++;
        if (clearStreak >= UNFREEZE_AFTER_CLEAR_ROUNDS && frozen.compareAndSet(true, false)) {
            frozenSince = null;
            clearStreak = 0;
            floor.set(SpawnPriority.LOW);
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder
                    .metrics().counter("buzhou.errorbudget.unfreeze", 1);
            LOGGER.log(System.Logger.Level.WARNING,
                    "错误预算回充（连续 {0} 轮清明）——spawn 准入地板回 LOW（解冻）",
                    UNFREEZE_AFTER_CLEAR_ROUNDS);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            Thread current = worker;
            worker = null;
            if (current != null) {
                current.interrupt();
            }
            floor.set(SpawnPriority.LOW); // 停机即解冻——政策离场不留下抬着的地板
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /** 观测面。 */
    public Map<String, Object> view() {
        return Map.of("frozen", frozen.get(),
                "frozenSince", String.valueOf(frozenSince),
                "evaluations", evaluations.get(),
                "floor", floor.get().name(),
                "interval", interval.toString());
    }

    /** 冻结态（观测/测试面）。 */
    public boolean frozen() {
        return frozen.get();
    }
}
