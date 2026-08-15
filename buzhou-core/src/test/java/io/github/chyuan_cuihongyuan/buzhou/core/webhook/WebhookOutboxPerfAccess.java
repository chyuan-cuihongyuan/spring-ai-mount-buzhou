package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;

import java.time.Instant;
import java.util.List;

/**
 * WebhookOutbox 性能/测试访问桥（test-jar 发布；同包访问包私有 outbox——T125 / impl-100）：
 * 供 perf 哨兵与跨模块测试直接驱动 append/due 路径（不经 HTTP 转发器）。
 */
public final class WebhookOutboxPerfAccess {

    /** 合成会话常量（造死信/outbox 键用；透传包私有 WebhookOutbox 的口径）。 */
    public static final String SESSION_ID = WebhookOutbox.SESSION_ID;

    private final WebhookOutbox outbox;

    public WebhookOutboxPerfAccess(SessionStateStore store, int capacity) {
        this.outbox = new WebhookOutbox(store, capacity);
    }

    public boolean append(String eventId, String type, String body) {
        return outbox.append(eventId, type, body);
    }

    /** 立即到期的记录（limit 截断；perf 断言非空用）。 */
    public List<WebhookOutbox.OutboxRecord> dueNow(int limit) {
        return outbox.due(Instant.now(), limit);
    }

    public int pendingCount() {
        return outbox.pendingCount();
    }

    /** 死信迁回 outbox（perf 哨兵直驱；同 forwarder.replayDeadLetters 的存储面）。 */
    public int requeueDead(int limit) {
        return outbox.requeueDead(limit);
    }
}
