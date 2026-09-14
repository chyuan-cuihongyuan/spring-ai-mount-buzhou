package io.github.chyuan_cuihongyuan.buzhou.core.observability;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;

import java.time.Duration;
import java.util.List;

/**
 * 事件时序单调性审计（spec 1436 / T2175 / impl 1089）——事件溯源不变量
 * （EventStoreDB：同流内时间戳单调是消费者正确性的前提）思想：同会话事件
 * 的 occurredAt 应按存储序非递减——逆序对意味着时钟回拨/并发写入乱序/
 * 重放注入，消费方（回放/审计/漂移计算）的正确性会静默劣化。
 *
 * <p>纯函数零状态：吃单会话事件列表（存储序）；报告逆序对数 + 最大倒退
 * 毫秒 + 首个逆序位（定位用）。与 TurnSequenceAudit（cleanup 域 turn 序号
 * 缺号）辨义：那轴管轮号缺号，本轴管时间戳单调。
 */
public final class EventOrderAudit {

    private EventOrderAudit() {
    }

    /**
     * @param totalEvents         事件总数
     * @param inversions          逆序相邻对数（occurredAt 严格小于前一条）
     * @param maxInversionMillis  最大倒退量（毫秒；无逆序 0）
     * @param firstInversionIndex 首个逆序对的后件下标（无逆序 -1）
     */
    public record Report(int totalEvents, int inversions, long maxInversionMillis,
                         int firstInversionIndex) {
    }

    /** 审计入口：单会话事件（存储序）。 */
    public static Report analyze(List<EventRecord> events) {
        if (events == null || events.isEmpty()) {
            return new Report(0, 0, 0, -1);
        }
        int inversions = 0;
        long maxInversion = 0;
        int firstIndex = -1;
        for (int i = 1; i < events.size(); i++) {
            var prev = events.get(i - 1).occurredAt();
            var cur = events.get(i).occurredAt();
            if (prev == null || cur == null) {
                continue;
            }
            if (cur.isBefore(prev)) {
                inversions++;
                long back = Duration.between(cur, prev).toMillis();
                maxInversion = Math.max(maxInversion, back);
                if (firstIndex == -1) {
                    firstIndex = i;
                }
            }
        }
        return new Report(events.size(), inversions, maxInversion, firstIndex);
    }
}
