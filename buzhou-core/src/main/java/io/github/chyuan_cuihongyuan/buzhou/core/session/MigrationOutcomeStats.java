package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 会话迁移结果普查（L 会话 1700 系 R10 = effort #1709 / spec 1709 /
 * 票 T2619 + T2620 / impl 1309）——Kafka 分区再均衡的过程结果普查思想：
 * {@link MigrationReconciliation}（spec 825）管「搬对了没」（数据对账），
 * 本面管「搬成了没/为何没搬」（过程结果）——成功/跳过（已在）/跳过（空）/
 * 失败四桶 + 尝试成功率。
 *
 * <p>实例面线程安全（AtomicLong）；宿主（SessionMigrator 包装方）逐次迁移
 * 记一笔；默认零行为变化，读面 opt-in。
 *
 * @since 1.0.0
 */
public final class MigrationOutcomeStats {

    /** 迁移结果闭集。 */
    public enum Outcome { MIGRATED, SKIPPED_CURRENT, SKIPPED_EMPTY, FAILED }

    private final Map<Outcome, AtomicLong> counters =
            new EnumMap<>(Map.ofEntries(
                    Map.entry(Outcome.MIGRATED, new AtomicLong()),
                    Map.entry(Outcome.SKIPPED_CURRENT, new AtomicLong()),
                    Map.entry(Outcome.SKIPPED_EMPTY, new AtomicLong()),
                    Map.entry(Outcome.FAILED, new AtomicLong())));

    /** 记录一次迁移结果。 */
    public void record(Outcome outcome) {
        counters.get(outcome).incrementAndGet();
    }

    /**
     * @param total          全部记录数
     * @param migrated       成功搬运数
     * @param skippedCurrent 已在目标侧跳过数
     * @param skippedEmpty   源为空跳过数
     * @param failed         失败数
     * @param attemptSuccessRatio 尝试成功率 migrated/(migrated+failed)；无尝试哨兵 −1
     */
    public record MigrationCensus(long total, long migrated, long skippedCurrent,
                                  long skippedEmpty, long failed, double attemptSuccessRatio) {
    }

    /** 普查快照。 */
    public MigrationCensus census() {
        long migrated = counters.get(Outcome.MIGRATED).get();
        long skippedCurrent = counters.get(Outcome.SKIPPED_CURRENT).get();
        long skippedEmpty = counters.get(Outcome.SKIPPED_EMPTY).get();
        long failed = counters.get(Outcome.FAILED).get();
        long attempted = migrated + failed;
        double ratio = attempted == 0 ? -1d : (double) migrated / attempted;
        return new MigrationCensus(migrated + skippedCurrent + skippedEmpty + failed,
                migrated, skippedCurrent, skippedEmpty, failed, ratio);
    }

    /** 测试归零（house 惯例）。 */
    public void resetForTest() {
        counters.values().forEach(c -> c.set(0));
    }
}
