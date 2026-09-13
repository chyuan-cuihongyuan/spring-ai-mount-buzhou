package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 韧性层运维面（impl-44 / spec 14 §A）：线程安全计数器 + {@link BuzhouHealth} 健康委托。
 *
 * <p>计数项（impl-41 对齐 core 四模块标准）：重试次数 / 重试耗尽 / 限流拒绝 / 模型超时 /
 * 内容拒绝检出 / 最近错误分类（五类标签，有界枚举）。健康语义：韧性层是纯内存机制，
 * <b>无 DOWN 态</b>——机制禁用时由健康装配层报 UNKNOWN（disabled 详情）。
 */
public final class ResilienceStats implements BuzhouHealth {

    private final AtomicLong retryAttempts = new AtomicLong();
    private final AtomicLong retryExhausted = new AtomicLong();
    private final AtomicLong rateLimitRejections = new AtomicLong();
    /** spec 637 / T924：生效限流后端标识（configure 时一次性写入）。 */
    private volatile String rateLimitBackend = "none";
    /** spec 638 / T926：熔断时间窗（毫秒；0 = count 窗——configure 时一次性写入）。 */
    private volatile long circuitTimeWindowMs;
    private final AtomicLong modelTimeouts = new AtomicLong();
    private final AtomicLong contentRefusals = new AtomicLong();
    private final AtomicLong circuitRejections = new AtomicLong();
    private final AtomicLong circuitTrips = new AtomicLong();
    private final AtomicLong fallbackSwitches = new AtomicLong();
    private final AtomicLong fallbackExhausted = new AtomicLong();
    private final AtomicLong quotaRejections = new AtomicLong();
    /** spec 56 §B / T250：配额 CAS 重试耗尽回退覆写次数（极端竞争观测——出现即说明原子路径长期抢败）。 */
    private final AtomicLong quotaCasFallbacks = new AtomicLong();
    private final AtomicReference<String> lastErrorCategory = new AtomicReference<>();
    /** 各模型熔断态（有界：模型名实际有限集）。 */
    private final Map<String, String> circuitStates = new java.util.concurrent.ConcurrentHashMap<>();
    /** 各模型当前冷却退避倍数（spec 25 / T104；CLOSED 复位后为 1）。 */
    private final Map<String, Long> circuitBackoff = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public String mechanism() {
        return "resilience";
    }

    @Override
    public Status status() {
        // 纯内存机制：恒 UP（无存储/外部依赖可 DOWN）；禁用态由装配层置 UNKNOWN。
        return Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("retryAttempts", retryAttempts.get());
        details.put("retryExhausted", retryExhausted.get());
        details.put("rateLimitRejections", rateLimitRejections.get());
        details.put("rateLimitBackend", rateLimitBackend); // spec 637：memory/memory-gcra/redis 一读便知
        details.put("circuitTimeWindowMs", circuitTimeWindowMs); // spec 638：时间窗生效读面（0=count 窗）
        details.put("modelTimeouts", modelTimeouts.get());
        details.put("contentRefusals", contentRefusals.get());
        details.put("circuitRejections", circuitRejections.get());
        details.put("circuitTrips", circuitTrips.get());
        details.put("fallbackSwitches", fallbackSwitches.get());
        details.put("fallbackExhausted", fallbackExhausted.get());
        details.put("quotaRejections", quotaRejections.get());
        details.put("quotaCasFallbacks", quotaCasFallbacks.get());
        double providerUtilization = lastProviderUtilization;
        if (!Double.isNaN(providerUtilization)) {
            details.put("providerUtilization", providerUtilization); // spec 740：供应商余量前瞻
        }
        if (!circuitStates.isEmpty()) {
            details.put("circuitStates", new LinkedHashMap<>(circuitStates));
        }
        if (!circuitBackoff.isEmpty()) {
            details.put("circuitBackoff", new LinkedHashMap<>(circuitBackoff));
        }
        String category = lastErrorCategory.get();
        if (category != null) {
            details.put("lastErrorCategory", category);
        }
        return details;
    }

    public void recordRetryAttempt() {
        retryAttempts.incrementAndGet();
    }

    public void recordRetryExhausted() {
        retryExhausted.incrementAndGet();
    }

    /** spec 637：生效限流后端标识（configure 时设置——health details 直读）。 */
    /** spec 740 / T1082：最近一次供应商限流头利用率（NaN = 尚无信号）。 */
    private volatile double lastProviderUtilization = Double.NaN;

    /**
     * spec 740 / T1082（719 前瞻信号的 stats 接线）：记录最近一次解析的
     * 供应商限流利用率（request/token 取大者）。消费端（advisor 拦截响应头）
     * 调用——本类只做聚合读数。
     */
    public void updateProviderUtilization(double utilization) {
        this.lastProviderUtilization = utilization;
    }

    /** spec 740：最近一次供应商限流利用率读数（NaN = 尚无信号）。 */
    public double lastProviderUtilization() {
        return lastProviderUtilization;
    }

    public void updateRateLimitBackend(String kind) {
        this.rateLimitBackend = kind == null ? "none" : kind;
    }

    /** spec 638：熔断时间窗毫秒（configure 时设置——声明是否生效一读便知）。 */
    public void updateCircuitTimeWindowMs(long ms) {
        this.circuitTimeWindowMs = Math.max(0, ms);
    }

    public void recordRateLimitRejection() {
        rateLimitRejections.incrementAndGet();
    }

    public void recordModelTimeout() {
        modelTimeouts.incrementAndGet();
    }

    public void recordContentRefusal() {
        contentRefusals.incrementAndGet();
    }

    public void recordErrorCategory(String category) {
        lastErrorCategory.set(category);
    }

    /** 熔断器 OPEN/HALF_OPEN 占位期调用被拒（impl-56）。 */
    public void recordCircuitRejected() {
        circuitRejections.incrementAndGet();
    }

    /** 熔断器跳闸一次（CLOSED→OPEN 或 HALF_OPEN→OPEN）。 */
    public void recordCircuitTrip(String modelName) {
        circuitTrips.incrementAndGet();
    }

    /** 模型熔断态快照更新（有界枚举值字符串）。 */
    public void updateCircuitState(String modelName, Object state) {
        circuitStates.put(modelName, String.valueOf(state));
    }

    /** 冷却退避倍数快照更新（spec 25 / T104 / impl-79）。 */
    public void updateCircuitBackoff(String modelName, long multiplier) {
        circuitBackoff.put(modelName, multiplier);
    }

    /** 降级切换成功一次（impl-57）。 */
    public void recordFallbackSwitch() {
        fallbackSwitches.incrementAndGet();
    }

    /** 备模型链全部耗尽一次（impl-57）。 */
    public void recordFallbackExhausted() {
        fallbackExhausted.incrementAndGet();
    }

    /** per-session 日配额拦截一次（impl-59）。 */
    public void recordQuotaRejection() {
        quotaRejections.incrementAndGet();
    }

    /** spec 56 §B / T250：配额 CAS 重试耗尽回退覆写一次（极端竞争可观测）。 */
    public void recordQuotaCasFallback() {
        quotaCasFallbacks.incrementAndGet();
    }

    public long quotaCasFallbacks() {
        return quotaCasFallbacks.get();
    }

    public long retryAttempts() {
        return retryAttempts.get();
    }

    public long rateLimitRejections() {
        return rateLimitRejections.get();
    }

    public long retryExhausted() {
        return retryExhausted.get();
    }
}
