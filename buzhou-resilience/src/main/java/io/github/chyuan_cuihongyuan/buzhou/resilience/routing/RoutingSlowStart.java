package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 路由慢启动爬坡（spec 702 / T955，nginx upstream {@code slow_start} 借鉴）：
 * 权重上调（恢复/新加入/热调升配）不瞬时到位——先落 floor 再按线性插值分
 * STEPS 步爬到 target，给连接池与上游缓存暖机时间，避免「升配即打爆→又触发
 * 离群驱逐」的震荡环。降权不 ramp（瞬时到位永远安全）。
 *
 * <p>{@link #tick()} 由后台 daemon 定时器驱动（每 slowStart/STEPS 一次），
 * 包内可见——测试可直接手动推进（零等待零抖动）。进行中 ramp 可升级 target
 * （重算剩余步，仍收敛）；AutoCloseable 关停调度器。
 */
public final class RoutingSlowStart implements AutoCloseable {

    /** 爬坡步数（nginx 逐秒连续爬坡的离散近似——4 步在暖机粒度与调度开销间取平）。 */
    static final int STEPS = 4;

    /** 爬坡起步权重（floor——nginx slow_start 从 0 起步，WRR 权重下界 1）。 */
    static final int FLOOR_WEIGHT = 1;

    private final WeightedChatModel router;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, Ramp> active = new ConcurrentHashMap<>();

    /** 进行中爬坡快照行（不可变）。 */
    public record Ramp(String route, int targetWeight, int currentWeight) {
    }

    public RoutingSlowStart(WeightedChatModel router, Duration slowStart) {
        this(router, slowStart, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "buzhou-routing-slow-start");
            t.setDaemon(true);
            return t;
        }));
    }

    /** 测试/定制注入口（scheduler 归本类生命周期）。 */
    RoutingSlowStart(WeightedChatModel router, Duration slowStart,
                     ScheduledExecutorService scheduler) {
        if (router == null || slowStart == null || slowStart.isZero() || slowStart.isNegative()) {
            throw new IllegalArgumentException("router 非空、slowStart 正时长");
        }
        this.router = router;
        this.scheduler = scheduler;
        long stepMillis = slowStart.toMillis() / STEPS;
        scheduler.scheduleAtFixedRate(this::safeTick, stepMillis, stepMillis,
                TimeUnit.MILLISECONDS);
    }

    private void safeTick() {
        try {
            tick();
        } catch (RuntimeException e) {
            // 调度线程绝不能被单次异常打死——爬坡停滞 ≤ 一个 step 的后果可接受
        }
    }

    /**
     * 启动/升级某路爬坡：新启动权重立即落 floor；进行中升级 target 保留当前
     * 权重继续爬（升级降 target 且当前已超新目标 = 瞬时降权到位并结束——降权
     * 永远安全）。未知路由抛错（候选面构造期定死）。
     */
    public synchronized void ramp(String route, int targetWeight) {
        if (targetWeight < FLOOR_WEIGHT) {
            throw new IllegalArgumentException(
                    "爬坡 target 必须 ≥1（当前 " + targetWeight + "）");
        }
        Ramp previous = active.get(route);
        if (previous == null) {
            router.setWeight(route, FLOOR_WEIGHT); // 未知路由由 router 拒绝
            active.put(route, new Ramp(route, targetWeight, FLOOR_WEIGHT));
            BuzhouMetricsHolder.metrics().counter("buzhou.routing.slow-start");
            return;
        }
        if (previous.currentWeight() >= targetWeight) {
            router.setWeight(route, targetWeight); // 瞬时降权（安全）——爬坡结束
            active.remove(route);
            return;
        }
        active.put(route, new Ramp(route, targetWeight, previous.currentWeight()));
    }

    /** 推进全部进行中 ramp 一步（到达 target 的即移除——router 停在恰 target）。 */
    synchronized void tick() {
        active.forEach((route, ramp) -> {
            int current = ramp.currentWeight();
            if (current >= ramp.targetWeight()) {
                return;
            }
            int next = Math.min(ramp.targetWeight(),
                    current + (int) Math.ceil(
                            (double) (ramp.targetWeight() - FLOOR_WEIGHT) / STEPS));
            router.setWeight(route, next);
            if (next >= ramp.targetWeight()) {
                active.remove(route);
            } else {
                active.put(route, new Ramp(route, ramp.targetWeight(), next));
            }
        });
    }

    /** 进行中爬坡快照（不可变）。 */
    public Map<String, Ramp> ramps() {
        Map<String, Ramp> out = new LinkedHashMap<>();
        active.forEach((route, ramp) -> out.put(route, ramp));
        return Map.copyOf(out);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
