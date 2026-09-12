package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 时段路由窗口切换器（spec 503 / T758，K8s CronJob / Argo Rollouts
 * schedule 思想）：周期 tick（注入 Clock）评估当前时刻 → 命中首窗取
 * 窗权（整表替换——未知 bean 名 setWeight 跳过不红，340 同口径）、无窗
 * 回落基础权重；目标快照变化才逐路 setWeight + WARN 留痕 + 计数
 * （417 价目热载同款审计面）——同窗内 tick 幂等零动作。
 *
 * <p>诚实边界：同日窗（跨午夜两窗拼）；轮询粒度 = checkInterval（非秒级
 * 精度）；多实例各自独立切换（无协调——时钟同源天然一致，415 同思想）。
 */
public class RoutingScheduleAdjuster implements SmartLifecycle {

    /** 时段窗口激活指标（spec 715：全字面常量）。 */
    public static final String METRIC_SCHEDULE_APPLIED = "buzhou.routing.schedule.applied";
    /** 时段窗口回落指标。 */
    public static final String METRIC_SCHEDULE_REVERTED = "buzhou.routing.schedule.reverted";

    private static final Logger LOG =
            LoggerFactory.getLogger(RoutingScheduleAdjuster.class);

    private final WeightedChatModel chatModel;
    private final List<BuzhouRoutingScheduleProperties.RoutingWindow> windows;
    private final Map<String, Integer> baseWeights;
    private final java.time.Duration checkInterval;
    private final Clock clock;

    private final AtomicBoolean running = new AtomicBoolean();
    private volatile ScheduledExecutorService scheduler;
    /** 上次已应用的目标快照（窗名|base + 权重串）——同目标幂等零动作。 */
    private final AtomicReference<String> lastApplied = new AtomicReference<>("");

    public RoutingScheduleAdjuster(WeightedChatModel chatModel,
            List<BuzhouRoutingScheduleProperties.RoutingWindow> windows,
            Map<String, Integer> baseWeights, java.time.Duration checkInterval, Clock clock) {
        this.chatModel = chatModel;
        this.windows = windows == null ? List.of() : List.copyOf(windows);
        this.baseWeights = baseWeights == null ? Map.of() : Map.copyOf(baseWeights);
        this.checkInterval = checkInterval;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler = Executors.newSingleThreadScheduledExecutor(
                    BuzhouThreadFactory.platform("buzhou-routing-schedule"));
            scheduler.scheduleAtFixedRate(this::gatedTick,
                    checkInterval.toMillis(), checkInterval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** tick 异常隔离防调度线程死亡（housekeeper 同法）。 */
    private void gatedTick() {
        try {
            evaluateOnce(Instant.now(clock));
        } catch (RuntimeException ignored) {
            // 防调度线程被意外异常杀死；下个 tick 重评
        }
    }

    /** 单次评估：首窗命中取窗权、无窗回落基础；目标变化才切换。 */
    public synchronized void evaluateOnce(Instant now) {
        LocalTime time = LocalTime.ofInstant(now, clock.getZone());
        BuzhouRoutingScheduleProperties.RoutingWindow active = null;
        for (BuzhouRoutingScheduleProperties.RoutingWindow window : windows) {
            if (window.contains(time)) {
                active = window;
                break; // 序优先——首命中
            }
        }
        Map<String, Integer> target = active == null ? baseWeights : active.weights();
        String snapshot = (active == null ? "base" : active.start() + "-" + active.end())
                + "|" + target;
        if (snapshot.equals(lastApplied.get())) {
            return; // 同窗内 tick 幂等零动作
        }
        Map<String, Integer> applied = new LinkedHashMap<>();
        target.forEach((beanName, weight) -> {
            try {
                chatModel.setWeight(beanName, weight);
                applied.put(beanName, weight);
            } catch (RuntimeException ignored) {
                // 未知 bean 名跳过不红（340 同口径）
            }
        });
        boolean revert = active == null;
        LOG.warn("时段路由窗口切换：{} 应用权重 {}（此前 {}）",
                revert ? "结束回落" : "激活[" + active.start() + "-" + active.end() + "]",
                applied, lastApplied.get());
        // spec 715：全字面指标名常量（动态拼接前缀违命名守卫口径）
        BuzhouMetricsHolder.metrics().counter(revert
                ? METRIC_SCHEDULE_REVERTED : METRIC_SCHEDULE_APPLIED, 1);
        lastApplied.set(snapshot);
    }

    /** 当前生效快照（观测）。 */
    public String appliedSnapshot() {
        return lastApplied.get();
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            ScheduledExecutorService current = scheduler;
            if (current != null) {
                current.shutdownNow();
                scheduler = null;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
