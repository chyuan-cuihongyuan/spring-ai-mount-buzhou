package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.springframework.context.SmartLifecycle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 维护窗口 cordon（spec 342 / T676，K8s cordon/uncordon 借鉴）：窗内把
 * spawn 准入地板的 maintenance 源抬 {@link SpawnPriority#HIGH}——不接新
 * 会话（NORMAL/LOW 即拒）、在途自然排空（drain 走 318 PDB 职责）；
 * 窗出落回。与 335 预算冻结正交（多源合成——谁先结束谁落回，另一方
 * 仍生效）。
 *
 * <p>双入口：yml 声明窗（{@code buzhou.maintenance.from/until/reason}——
 * <b>过期窗启动即 no-op 不追溯</b>）+ 运行时事故按钮
 * {@link #cordon(String)}/{@link #uncordon()}（即时生效不重启——325
 * 「按钮必须预先在场」纪律，bean 恒在）。SmartLifecycle 自管虚拟线程
 * 轮询（fixed-period interruptible sleep——335 同款）；时钟可注入。
 */
public final class MaintenanceCordon implements SmartLifecycle {

    /** 地板源名。 */
    public static final String SOURCE = "maintenance";

    private static final System.Logger LOGGER = System.getLogger(MaintenanceCordon.class.getName());

    private final SpawnAdmissionFloor floor;
    private final Instant windowFrom;
    private final Instant windowUntil;
    private final String windowReason;
    private final Duration pollInterval;
    private final Clock clock;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean cordoned = new AtomicBoolean(false);
    private final AtomicReference<String> reason = new AtomicReference<>();
    private final AtomicReference<Instant> until = new AtomicReference<>();
    private final AtomicLong cordonedCount = new AtomicLong();
    private volatile Thread worker;

    /**
     * @param floor        准入地板（maintenance 源的写者）
     * @param windowFrom   声明窗起点（null = 无声明窗——仅运行时按钮）
     * @param windowUntil  声明窗终点（null = 无声明窗）
     * @param windowReason 声明窗备注
     * @param pollInterval 轮询周期
     * @param clock        时钟（测试注入伪时钟）
     */
    public MaintenanceCordon(SpawnAdmissionFloor floor, Instant windowFrom, Instant windowUntil,
            String windowReason, Duration pollInterval, Clock clock) {
        this.floor = java.util.Objects.requireNonNull(floor);
        if (windowFrom != null && windowUntil != null && !windowFrom.isBefore(windowUntil)) {
            throw new IllegalArgumentException(
                    "buzhou.maintenance.from 须早于 until（from=" + windowFrom + ", until=" + windowUntil + "）");
        }
        this.windowFrom = windowFrom;
        this.windowUntil = windowUntil;
        this.windowReason = windowReason == null ? "" : windowReason;
        this.pollInterval = pollInterval == null || pollInterval.isZero() || pollInterval.isNegative()
                ? Duration.ofSeconds(15) : pollInterval;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        if (windowFrom != null && windowUntil != null
                && !Instant.now(this.clock).isBefore(windowUntil)) {
            // 过期窗启动即 no-op（不追溯）——窗字段保留观测，轮询恒出窗
            this.until.set(null);
        } else if (windowUntil != null) {
            this.until.set(windowUntil);
        }
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            worker = io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory
                    .virtual("maintenance-cordon").newThread(this::run);
            worker.start();
        }
    }

    private void run() {
        while (running.get()) {
            try {
                evaluate();
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.WARNING, "维护窗评估异常（继续轮询）", e);
            }
        }
    }

    /** 单轮评估（调度周期调；测试/运维可手动）。 */
    public void evaluate() {
        Instant now = Instant.now(clock);
        Instant effectiveUntil = until.get();
        if (effectiveUntil == null) {
            if (cordoned.get()) {
                uncordonInternal(); // 手动解除/无窗兜底
            }
            return;
        }
        boolean within = now.isBefore(effectiveUntil)
                && (windowFrom == null || !now.isBefore(windowFrom));
        if (within) {
            String why = windowReason.isBlank()
                    ? (reason.get() == null ? "declared-window" : reason.get()) : windowReason;
            cordonInternal(why);
        } else if (cordoned.get()) {
            uncordonInternal(); // 窗出自动解除
        }
    }

    private void cordonInternal(String why) {
        if (cordoned.compareAndSet(false, true)) {
            floor.set(SOURCE, SpawnPriority.HIGH);
            cordonedCount.incrementAndGet();
            BuzhouMetricsHolder.metrics().counter("buzhou.maintenance.cordoned", 1);
            LOGGER.log(System.Logger.Level.WARNING,
                    "维护 cordon 生效——不接新会话（NORMAL/LOW 即拒），在途自然排空：{0}", why);
        }
        if (reason.get() == null) {
            reason.set(why);
        }
    }

    private void uncordonInternal() {
        if (cordoned.compareAndSet(true, false)) {
            floor.set(SOURCE, null);
            reason.set(null);
            BuzhouMetricsHolder.metrics().counter("buzhou.maintenance.uncordoned", 1);
            LOGGER.log(System.Logger.Level.WARNING, "维护 cordon 解除——恢复接新会话");
        }
    }

    /** 运行时事故按钮：立即 cordon（不重启即时生效；手动解除前持续）。 */
    public void cordon(String why) {
        until.set(Instant.MAX); // 无限窗——直到 uncordon 或停机
        reason.set(why == null ? "manual" : why);
        cordonInternal(reason.get());
    }

    /** 运行时事故按钮：解除（声明窗一并作废——手动解除优先）。 */
    public void uncordon() {
        until.set(null);
        uncordonInternal();
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            Thread current = worker;
            worker = null;
            if (current != null) {
                current.interrupt();
            }
            floor.set(SOURCE, null); // 停机即解除——离场不留抬着的地板
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /** 观测面。 */
    public Map<String, Object> view() {
        return Map.of("cordoned", cordoned.get(),
                "reason", String.valueOf(reason.get()),
                "until", String.valueOf(until.get()),
                "cordonedCount", cordonedCount.get());
    }

    /** 是否 cordon 中（观测/测试面）。 */
    public boolean cordoned() {
        return cordoned.get();
    }
}
