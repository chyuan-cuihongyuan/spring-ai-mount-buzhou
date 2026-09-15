package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.List;

/**
 * 检查点滞后读面（spec 1806 / T2813 / impl 1407）——Kafka consumer-group
 * lag（log end offset − committed offset = 重放长度）/ SQLite WAL checkpoint
 * （未检查点化帧数 = 恢复期 replay 成本）思想：每会话「已产事件数 − 已检查
 * 点事件数」= 崩溃后该会话要重放多少；maxLag 即最坏恢复成本，越限会话数
 * 即恢复 SLA 风险面。滞后不是错——持续增长才是：读面只给事实，趋势裁决
 * 归宿主。
 *
 * <p>纯函数零状态、只读不裁决；采样口径（事件序列号 / 检查点时点）由调用
 * 方声明。
 */
public final class CheckpointLagReadout {

    private CheckpointLagReadout() {
    }

    /**
     * 单会话滞后事实契约：两计数非负且 checkpointed ≤ produced（检查点不
     * 超前于产出）；lag = produced − checkpointed。
     */
    public record SessionLag(String sessionId, long eventsProduced, long eventsCheckpointed) {

        public SessionLag {
            boolean malformed = eventsProduced < 0 || eventsCheckpointed < 0
                    || eventsCheckpointed > eventsProduced;
            if (malformed || sessionId == null || sessionId.isBlank()) {
                throw new IllegalArgumentException(
                        "非法会话滞后：id=" + sessionId + ", produced=" + eventsProduced
                                + ", checkpointed=" + eventsCheckpointed
                                + "（要求 id 非空且 0 ≤ checkpointed ≤ produced）");
            }
        }

        public long lag() {
            return eventsProduced - eventsCheckpointed;
        }
    }

    /**
     * @param lags          逐会话滞后（入参序）
     * @param totalLag      滞后合计（全会话重放成本）
     * @param maxLag        最坏单会话滞后（无会话 -1 哨兵）
     * @param laggiestUser  最坏会话 id（无会话或全追平 null）
     */
    public record LagReport(List<SessionLag> lags, long totalLag, long maxLag, String laggiestUser) {

        /** 滞后越限会话数（lag > threshold；无会话 0）。 */
        public long sessionsBeyond(long threshold) {
            return lags.stream().filter(s -> s.lag() > threshold).count();
        }

        /** 追平率 = lag==0 会话占比（无会话 -1 哨兵）。 */
        public double caughtUpRatio() {
            return lags.isEmpty() ? -1d
                    : (double) lags.stream().filter(s -> s.lag() == 0).count() / lags.size();
        }
    }

    /**
     * 滞后账目入口。null 按空表；会话事实逐条核契约，畸形即 fail-fast
     * （读面不吞脏事实）。并列最坏取首个（入参序，稳定可复现）。
     */
    public static LagReport analyze(List<SessionLag> lags) {
        List<SessionLag> window = lags == null ? List.of() : lags;
        long total = 0;
        long max = -1;
        String laggiest = null;
        for (SessionLag s : window) {
            total += s.lag();
            if (s.lag() > max) {
                max = s.lag();
                laggiest = s.sessionId();
            }
        }
        return new LagReport(List.copyOf(window), total, max, laggiest);
    }
}
