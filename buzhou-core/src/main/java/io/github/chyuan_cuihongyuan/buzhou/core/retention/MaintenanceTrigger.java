package io.github.chyuan_cuihongyuan.buzhou.core.retention;

/**
 * impl-37 / spec 13 §stores-6：维护触发公式（PostgreSQL 21.8K★ autovacuum 阈值四件套：
 * {@code base + scale_factor × 总量}、封顶、硬性下限兜底）。
 *
 * <p>在本套件中的用途 = <b>单周期批删限量</b>：批删量随总量水涨船高（大表每周期清更多、
 * 摊还扫描成本），封顶防长事务，硬性下限保证小表/空闲系统每个周期也至少兑现 floor 个——
 * ClickHouse 低频兑现与 PG 阈值两者的合取。
 *
 * @param base       基础批量；null/＜1 → 默认 50
 * @param scaleFactor 总量比例因子；null/＜0 → 默认 0.2
 * @param cap        批量封顶；null/＜1 → 默认 5,000
 * @param hardFloor  硬性下限（兜底——批删量永不低于此值）；null/＜1 → 默认 50
 */
public record MaintenanceTrigger(Integer base, Double scaleFactor, Integer cap, Integer hardFloor) {

    public static final int DEFAULT_BASE = 50;
    public static final double DEFAULT_SCALE_FACTOR = 0.2;
    public static final int DEFAULT_CAP = 5_000;
    public static final int DEFAULT_HARD_FLOOR = 50;

    public MaintenanceTrigger {
        base = base == null || base < 1 ? DEFAULT_BASE : base;
        scaleFactor = scaleFactor == null || scaleFactor < 0 ? DEFAULT_SCALE_FACTOR : scaleFactor;
        cap = cap == null || cap < 1 ? DEFAULT_CAP : cap;
        hardFloor = hardFloor == null || hardFloor < 1 ? DEFAULT_HARD_FLOOR : hardFloor;
    }

    public static MaintenanceTrigger defaults() {
        return new MaintenanceTrigger(null, null, null, null);
    }

    /**
     * 单周期批删限量：{@code clamp(round(base + scaleFactor × totalCount), hardFloor, cap)}。
     * 小表 → 下限/基础值兜底；中表 → 公式；大表 → 封顶。
     */
    public int batchLimit(int totalCount) {
        long formula = Math.round((double) base + scaleFactor * Math.max(0, totalCount));
        long lowerBound = Math.min(hardFloor, cap); // floor 不得越过 cap
        return (int) Math.max(lowerBound, Math.min(cap, formula));
    }
}
