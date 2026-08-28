package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 79 §B / T312：outbox due-time 索引红队——退避积压不放大读（due 只走键序区间）；
 * 孤儿/陈旧索引自愈；旧版数据（无索引）构造期回填；生命周期出口清键。
 * 借鉴：Kafka log+index / LSM base+index 双结构（自愈容错双写竞窗）。
 */
class WebhookOutboxDueIndexTest {

    private static InMemorySessionStateStore freshStore() {
        return new InMemorySessionStateStore();
    }

    /** 背退避记录后 due() 不再返回它；迁回（更早 ts）后恢复可见——索引键随迁。 */
    @Test
    void backoffMovesIndexKeyAndDueRespectsIt() {
        InMemorySessionStateStore store = freshStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 16);
        outbox.append("e1", "t1", "{}");

        WebhookOutbox.OutboxRecord current = outbox.due(Instant.now(), 10).getFirst();
        WebhookOutbox.OutboxRecord future = new WebhookOutbox.OutboxRecord("e1", "t1", "{}",
                current.seq(), 1, System.currentTimeMillis() + 60_000, current.createdAtEpochMs());
        outbox.update(current, future);

        assertThat(outbox.due(Instant.now(), 10)).isEmpty(); // 未来到期：不可见
        assertThat(outbox.pendingCount()).isEqualTo(1); // 仍在册

        WebhookOutbox.OutboxRecord revived = new WebhookOutbox.OutboxRecord("e1", "t1", "{}",
                current.seq(), 1, System.currentTimeMillis(), current.createdAtEpochMs());
        outbox.update(future, revived);
        assertThat(outbox.due(Instant.now(), 10)).hasSize(1); // 索引迁回：恢复可见
    }

    /** 多条到期：limit 按到期序取最早（重退避者不被挤饿——spec 79 §A 定案）。 */
    @Test
    void dueTakesEarliestFirstUnderLimit() {
        InMemorySessionStateStore store = freshStore();
        long now = System.currentTimeMillis();
        // 直接铺旧格式记录（不同到期时间），构造期回填索引——同路径双验
        putLegacyRecord(store, "late", 2, now + 5_000);
        putLegacyRecord(store, "early", 1, now - 5_000);
        WebhookOutbox outbox = new WebhookOutbox(store, 16);

        List<WebhookOutbox.OutboxRecord> due = outbox.due(Instant.now(), 1);

        assertThat(due).hasSize(1);
        assertThat(due.getFirst().eventId()).isEqualTo("early"); // 最早到期优先（limit=1）
    }

    /** 孤儿（记录已删）与陈旧（记录已后移）索引：due() 就地清键自愈，不误投不报错。 */
    @Test
    void orphanAndStaleIndexEntriesSelfHeal() {
        InMemorySessionStateStore store = freshStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 16);
        outbox.append("real", "t", "{}");
        long past = System.currentTimeMillis() - 1_000;
        // 孤儿：指向不存在的记录
        store.put(WebhookOutbox.SESSION_ID, new StateEntry(
                WebhookOutbox.DUE_PREFIX + String.format("%016d", past) + ".ghost",
                "ghost", "webhook-outbox", 0, null, Instant.now()));
        // 陈旧：real 已在当前 ts（新键在位），旧位置残留一个过去 ts 的键
        WebhookOutbox.OutboxRecord current = outbox.due(Instant.now(), 10).getFirst();
        WebhookOutbox.OutboxRecord future = new WebhookOutbox.OutboxRecord("real", "t", "{}",
                current.seq(), 1, System.currentTimeMillis() + 60_000, current.createdAtEpochMs());
        outbox.update(current, future);
        store.put(WebhookOutbox.SESSION_ID, new StateEntry(
                WebhookOutbox.DUE_PREFIX + String.format("%016d", past) + ".real",
                "real", "webhook-outbox", 0, null, Instant.now()));

        assertThat(outbox.due(Instant.now(), 10)).isEmpty(); // 陈旧指向的未来记录不可见
        // 自愈清键：孤儿与陈旧索引都不在了
        assertThat(store.getAll(WebhookOutbox.SESSION_ID).keySet())
                .noneMatch(k -> k.startsWith(WebhookOutbox.DUE_PREFIX + String.format("%016d", past)));
    }

    /** 生命周期出口清键：delete/markDead 不残留；requeueDead 换新键后可再投。 */
    @Test
    void lifecycleExitsLeaveNoStaleDueKeys() {
        InMemorySessionStateStore store = freshStore();
        WebhookOutbox outbox = new WebhookOutbox(store, 16);
        outbox.append("gone", "t", "{}");
        outbox.append("dying", "t", "{}");

        outbox.delete(outbox.due(Instant.now(), 10).stream()
                .filter(r -> r.eventId().equals("gone")).findFirst().orElseThrow());
        outbox.markDead(outbox.due(Instant.now(), 10).stream()
                .filter(r -> r.eventId().equals("dying")).findFirst().orElseThrow());

        assertThat(store.getAll(WebhookOutbox.SESSION_ID).keySet())
                .noneMatch(k -> k.startsWith(WebhookOutbox.DUE_PREFIX)); // 出口清键
        assertThat(outbox.pendingCount()).isZero();

        assertThat(outbox.requeueDead(10)).isEqualTo(1);
        assertThat(outbox.due(Instant.now(), 10)).hasSize(1); // 死信复活：新键在位
    }

    /** 旧版数据（无 due 索引）直接铺 store，构造期回填后照常可投。 */
    private static void putLegacyRecord(SessionStateStore store, String eventId, long seq,
            long nextAttemptAtEpochMs) {
        WebhookOutbox.OutboxRecord record = new WebhookOutbox.OutboxRecord(
                eventId, "legacy", "{}", seq, 0, nextAttemptAtEpochMs,
                nextAttemptAtEpochMs);
        store.put(WebhookOutbox.SESSION_ID, new StateEntry(
                WebhookOutbox.OUTBOX_PREFIX + eventId, legacyJson(record),
                "webhook-outbox", 0, null, Instant.now()));
    }

    private static String legacyJson(WebhookOutbox.OutboxRecord record) {
        return "{\"eventId\":\"" + record.eventId() + "\",\"type\":\"legacy\",\"body\":\"{}\",\"seq\":"
                + record.seq() + ",\"attempts\":0,\"nextAttemptAtEpochMs\":"
                + record.nextAttemptAtEpochMs() + ",\"createdAtEpochMs\":"
                + record.createdAtEpochMs() + "}";
    }
}
