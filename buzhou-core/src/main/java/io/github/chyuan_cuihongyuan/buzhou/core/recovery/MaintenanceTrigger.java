package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

/**
 * 维护触发器（spec 2033 / T3167 / impl 1584）——Postgres autovacuum
 * 思想：死数据（已消费事件 / 已投递 outbox / 过期索引项）的清理不在
 * 每次操作后即时做（写放大），而是**死/活比例超阈值**才批量触发；
 * 配最大间隔兜底（低流量场景比例永不达标也要定期清——防死数据无限
 * 陈化）；触发记账（次数/上次时刻）——触发频率即维护健康度。
 *
 * <p>纯判定无副作用（触发后的动作归调用方）；时间由调用方传入
 *（确定性可回放）。
 */
public final class MaintenanceTrigger {

    /** 默认死/活比例阈值（autovacuum 20% 惯例）。 */
    public static final double DEFAULT_DEAD_RATIO_THRESHOLD = 0.2d;

    /** 默认最大触发间隔（毫秒）——低流量兜底。 */
    public static final long DEFAULT_MAX_INTERVAL_MILLIS = 24L * 60 * 60 * 1000;

    private final double deadRatioThreshold;
    private final long maxIntervalMillis;
    private long triggerCount;
    private long lastTriggeredAt = -1;

    /** 契约：threshold ∈ (0,1]、maxIntervalMillis &gt; 0（fail-fast）。 */
    public MaintenanceTrigger(double deadRatioThreshold, long maxIntervalMillis) {
        if (!(deadRatioThreshold > 0) || deadRatioThreshold > 1 || Double.isNaN(deadRatioThreshold)) {
            throw new IllegalArgumentException("threshold 须在 (0,1]：" + deadRatioThreshold);
        }
        if (maxIntervalMillis <= 0) {
            throw new IllegalArgumentException("maxIntervalMillis 须 > 0：" + maxIntervalMillis);
        }
        this.deadRatioThreshold = deadRatioThreshold;
        this.maxIntervalMillis = maxIntervalMillis;
    }

    public MaintenanceTrigger() {
        this(DEFAULT_DEAD_RATIO_THRESHOLD, DEFAULT_MAX_INTERVAL_MILLIS);
    }

    /**
     * 是否该触发：死/活 ≥ 阈值（live==0 且 dead&gt;0 全死必清；双零不
     * 触——无事可做）**或**距上次触发 ≥ 最大间隔（低流量兜底——从未
     * 触发过自 0 起算）。
     */
    public boolean shouldTrigger(long deadCount, long liveCount, long nowMillis) {
        if (deadCount < 0 || liveCount < 0 || nowMillis < 0) {
            throw new IllegalArgumentException("计数/时刻须非负：dead=" + deadCount
                    + " live=" + liveCount + " now=" + nowMillis);
        }
        boolean ratioReached = liveCount == 0
                ? deadCount > 0
                : (double) deadCount / liveCount >= deadRatioThreshold;
        boolean intervalReached = nowMillis - Math.max(lastTriggeredAt, 0) >= maxIntervalMillis;
        return ratioReached || intervalReached;
    }

    /** 触发记账（调用方执行清理后回调——次数 + 时刻）。 */
    public void noteTriggered(long nowMillis) {
        if (nowMillis < 0) {
            throw new IllegalArgumentException("nowMillis 须 ≥ 0：" + nowMillis);
        }
        triggerCount++;
        lastTriggeredAt = nowMillis;
    }

    /** 触发次数（维护频率健康度）。 */
    public long triggerCount() {
        return triggerCount;
    }

    /** 上次触发时刻（-1 = 从未；间隔兜底的基点）。 */
    public long lastTriggeredAt() {
        return lastTriggeredAt;
    }
}
