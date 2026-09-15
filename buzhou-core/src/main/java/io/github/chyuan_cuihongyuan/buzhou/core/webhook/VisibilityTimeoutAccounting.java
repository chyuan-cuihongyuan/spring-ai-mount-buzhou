package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import java.util.List;

/**
 * 可见性超时账（spec 1864 / T2929 / impl 1465）——AWS SQS visibility
 * timeout 思想：消息**取走即隐藏**（别的消费者看不见——防重复消费），
 * 但隐藏有时限——**未确认超时即重回队列重投**（消费方死了消息不丢），
 * 重投计数超阈进**死信**（毒消息不再无限循环）。三段语义：在飞（隐藏
 * 中）/重投（超时未确认）/死信（重投穷尽）——「不丢」与「不死循环」
 * 两全。
 *
 * <p>纯函数零状态、只记账不投递（投递归宿主）。
 */
public final class VisibilityTimeoutAccounting {

    private VisibilityTimeoutAccounting() {
    }

    /**
     * 重投判定：未确认且 now ≥ deliveredAt + visibilityTimeout（边界含上
     *——到点即回队）。契约：时间戳与超时 ≥ 0（fail-fast）。
     */
    public static boolean shouldRedeliver(long deliveredAtMillis,
                                          long visibilityTimeoutMillis,
                                          long nowMillis, boolean acknowledged) {
        if (deliveredAtMillis < 0 || visibilityTimeoutMillis < 0 || nowMillis < 0) {
            throw new IllegalArgumentException(String.format(
                    "时间入参不能为负：deliveredAt=%d, timeout=%d, now=%d",
                    deliveredAtMillis, visibilityTimeoutMillis, nowMillis));
        }
        return !acknowledged
                && nowMillis >= deliveredAtMillis + visibilityTimeoutMillis;
    }

    /**
     * 死信判定：redeliveryCount ≥ maxRedeliveries（边界含上——第 max 次
     * 重投后仍失败即死信）。契约：计数与上限 ≥ 0；上限 0 合法（零容忍
     * 首败即死信）。
     */
    public static boolean shouldDeadLetter(int redeliveryCount, int maxRedeliveries) {
        if (redeliveryCount < 0 || maxRedeliveries < 0) {
            throw new IllegalArgumentException(String.format(
                    "计数不能为负：redeliveries=%d, max=%d",
                    redeliveryCount, maxRedeliveries));
        }
        return redeliveryCount >= maxRedeliveries;
    }

    /** 单消息投递状态事实：契约：时间戳 ≥ 0、redeliveries ≥ 0。 */
    public record DeliveryFact(long deliveredAtMillis, boolean acknowledged,
                               int redeliveries) {

        public DeliveryFact {
            if (deliveredAtMillis < 0 || redeliveries < 0) {
                throw new IllegalArgumentException(String.format(
                        "非法投递事实：deliveredAt=%d, redeliveries=%d",
                        deliveredAtMillis, redeliveries));
            }
        }
    }

    /**
     * @param inFlight       在飞数（隐藏中且未超时）
     * @param overdueRedeliver 超时未确认该重投数
     * @param deadLetterCandidates 死信候选数（重投穷尽）
     * @param oldestInFlightAgeMillis 最老在飞龄（无在飞 -1 哨兵）
     */
    public record Census(int messages, long inFlight, long overdueRedeliver,
                         long deadLetterCandidates, long oldestInFlightAgeMillis) {

        /** 在飞占比（无消息 -1 哨兵）。 */
        public double inFlightRatio() {
            return messages == 0 ? -1d : (double) inFlight / messages;
        }
    }

    /** 普查入口。null 按空表。 */
    public static Census census(long nowMillis, long visibilityTimeoutMillis,
                                int maxRedeliveries, List<DeliveryFact> facts) {
        List<DeliveryFact> window = facts == null ? List.of() : facts;
        long inFlight = 0;
        long overdue = 0;
        long dead = 0;
        long oldestAge = -1;
        for (DeliveryFact f : window) {
            if (f.acknowledged()) {
                continue;
            }
            if (shouldDeadLetter(f.redeliveries(), maxRedeliveries)) {
                dead++;
                continue;
            }
            if (shouldRedeliver(f.deliveredAtMillis(), visibilityTimeoutMillis,
                    nowMillis, false)) {
                overdue++;
            } else {
                inFlight++;
                oldestAge = Math.max(oldestAge, nowMillis - f.deliveredAtMillis());
            }
        }
        return new Census(window.size(), inFlight, overdue, dead, oldestAge);
    }
}
