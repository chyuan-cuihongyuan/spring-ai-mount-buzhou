package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import java.util.List;

/**
 * 事务半消息审计（spec 1812 / T2825 / impl 1413）——RocketMQ 事务消息
 * 思想：发送侧先落**半消息**（对消费者不可见），本地事务成功才 commit
 * 放行、失败则 rollback 丢弃；broker 周期**回查**滞留半消息补裁决。「发
 * 消息」与「做事务」的原子性靠半消息两阶段 + 回查兜底，而不是分布式
 * 事务。审计读面回答：半消息滞留多少（未裁决面）、多少已超回查阈（该
 * 回查）、裁决率多高（本地事务健康度）。
 *
 * <p>纯函数零状态、只读不裁决（回查动作归宿主）；意图键与年龄口径由
 * 调用方声明。
 */
public final class HalfMessageAudit {

    private HalfMessageAudit() {
    }

    /** 意图三态：HALF 半消息（未裁决）/ COMMITTED 放行 / ROLLED_BACK 丢弃。 */
    public enum IntentState {

        /** 半消息滞留——本地事务未裁决（超阈即回查候选）。 */
        HALF,

        /** 本地事务成功已放行。 */
        COMMITTED,

        /** 本地事务失败/超时已丢弃。 */
        ROLLED_BACK
    }

    /** 单意图事实契约：key 非空白、age ≥ 0。 */
    public record Intent(String key, IntentState state, long ageMillis) {

        public Intent {
            if (key == null || key.isBlank() || ageMillis < 0 || state == null) {
                throw new IllegalArgumentException(
                        "非法意图：key=" + key + ", state=" + state + ", age=" + ageMillis
                                + "（要求 key 非空白、state 非 null、age ≥ 0）");
            }
        }
    }

    /**
     * @param total       意图总数
     * @param halves      半消息滞留数（未裁决面）
     * @param committed   已放行数
     * @param rolledBack  已丢弃数
     * @param staleHalves 超回查阈的滞留数（HALF 且 age ≥ staleThresholdMillis）
     */
    public record Census(int total, long halves, long committed, long rolledBack,
                         long staleHalves) {

        /** 裁决率 = (committed+rolledBack)/total（无意图 -1 哨兵）。 */
        public double resolutionRatio() {
            return total == 0 ? -1d : (double) (committed + rolledBack) / total;
        }

        /** 滞留率 = halves/total（无意图 -1 哨兵）。 */
        public double pendingRatio() {
            return total == 0 ? -1d : (double) halves / total;
        }
    }

    /**
     * 审计入口。契约：staleThresholdMillis ≥ 0（fail-fast）；null 按空表；
     * 意图逐条核契约，畸形即 fail-fast。
     */
    public static Census audit(long staleThresholdMillis, List<Intent> intents) {
        if (staleThresholdMillis < 0) {
            throw new IllegalArgumentException(
                    "staleThresholdMillis 不能为负：" + staleThresholdMillis);
        }
        List<Intent> window = intents == null ? List.of() : intents;
        long halves = 0;
        long committed = 0;
        long rolledBack = 0;
        long stale = 0;
        for (Intent i : window) {
            switch (i.state()) {
                case HALF -> {
                    halves++;
                    if (i.ageMillis() >= staleThresholdMillis) {
                        stale++;
                    }
                }
                case COMMITTED -> committed++;
                case ROLLED_BACK -> rolledBack++;
            }
        }
        return new Census(window.size(), halves, committed, rolledBack, stale);
    }
}
