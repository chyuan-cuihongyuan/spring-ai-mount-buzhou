package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Instant;
import java.util.List;

/**
 * 空闲会话监控 Holder（spec 1620 / T2391，spec 179/841 双孤类接线）：
 * 进程级特征仓 + 监控器 + 直方图——SessionFeaturesHook（spec 161）喂数、
 * afterTurn 每 32 轮节拍 sweep（空闲清单 + 翻转通知 + 分布入直方）。
 * 纯观测旁路：只判定不动作（压缩/归档以清单为候选——分层诚实）。
 */
public final class IdleMonitorHolder {

    /** sweep 节拍：每 N 轮一次（O(会话数) 内存扫描，32 轮摊薄）。 */
    public static final int SWEEP_EVERY_TURNS = 32;

    private static final SessionFeatureStore STORE = new SessionFeatureStore();
    private static final IdleSessionMonitor MONITOR =
            new IdleSessionMonitor(STORE, java.time.Duration.ofMinutes(15));
    private static final IdleDurationHistogram HISTOGRAM = new IdleDurationHistogram();

    private IdleMonitorHolder() {
    }

    /** 进程级特征仓（SessionFeaturesHook 喂数面）。 */
    public static SessionFeatureStore store() {
        return STORE;
    }

    /** 监控器（阈值 15 分钟；翻转监听经 onChange 挂）。 */
    public static IdleSessionMonitor monitor() {
        return MONITOR;
    }

    /** 空闲时长分布直方（sweep 喂数）。 */
    public static IdleDurationHistogram histogram() {
        return HISTOGRAM;
    }

    /** sweep 并把空闲时长喂直方（now 可注入——测试面）。 */
    public static List<IdleSessionMonitor.IdleInfo> sweepAndRecord(Instant now) {
        List<IdleSessionMonitor.IdleInfo> idle = MONITOR.sweep(now);
        for (IdleSessionMonitor.IdleInfo info : idle) {
            HISTOGRAM.record(info.idleMillis());
        }
        return idle;
    }
}
