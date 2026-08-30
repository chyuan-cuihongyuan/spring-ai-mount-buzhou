package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 96 §B / T362：due 索引审计红队——三类失真计数与样本（孤儿/陈旧/缺失）；
 * 干净面 clean()；修复清/补后复审计清零；只读审计不落键。spec 79 fog 项收口
 * （StoreFsck 同思想——只读报告 + 按项修复）。
 */
class WebhookOutboxAuditTest {

    @Test
    void classifiesThreeDistortionsWithSamples() {
        InMemorySessionStateStore fresh = new InMemorySessionStateStore();
        long now = System.currentTimeMillis();
        // 健康：一条记录 + 正确索引
        putRecordInto(fresh, "ok", now + 1000, now + 1000);
        putIndexInto(fresh, "ok", now + 1000);
        // 孤儿：索引存在、记录已删
        putIndexInto(fresh, "ghost", now + 2000);
        // 陈旧：索引 ts ≠ 记录 nextAttemptAt
        putRecordInto(fresh, "stale", now + 3000, now + 3000);
        putIndexInto(fresh, "stale", now + 9999);
        // 缺失：记录存在、无索引（投递停摆——须修）
        putRecordInto(fresh, "lost", now + 4000, now + 4000);

        WebhookOutboxAudit.DueAuditReport report = WebhookOutboxAudit.audit(fresh);

        assertThat(report.orphanCount()).isEqualTo(1);
        assertThat(report.staleCount()).isEqualTo(1);
        assertThat(report.missingCount()).isEqualTo(1);
        assertThat(report.recordCount()).isEqualTo(3);
        assertThat(report.indexCount()).isEqualTo(3);
        assertThat(report.orphanSamples().getFirst()).endsWith(".ghost");
        assertThat(report.staleSamples().getFirst()).endsWith(".stale");
        assertThat(report.missingSamples().getFirst()).isEqualTo("outbox.lost");
        assertThat(report.clean()).isFalse();
    }

    @Test
    void repairHealsAllThreeAndReauditIsClean() {
        InMemorySessionStateStore fresh = new InMemorySessionStateStore();
        long now = System.currentTimeMillis();
        putRecordInto(fresh, "lost", now + 1000, now + 1000);
        putIndexInto(fresh, "ghost", now + 2000);
        putRecordInto(fresh, "stale", now + 3000, now + 3000);
        putIndexInto(fresh, "stale", now + 9999);

        WebhookOutboxAudit.DueAuditReport before = WebhookOutboxAudit.audit(fresh);
        Map<String, Integer> repaired = WebhookOutboxAudit.repair(fresh, before,
                new WebhookOutboxAudit.RepairOptions(true, true, true));

        assertThat(repaired).containsEntry("orphans", 1)
                .containsEntry("stale", 1).containsEntry("missing", 1);
        // 复审计：孤儿/陈旧清零；缺失重建后记录-索引一致（stale 键清了但记录的索引也要重建……
        // 陈旧修复=清旧键，正确 ts 的新键本就在位（update 双写）——此处直铺场景陈旧清后缺失
        // 由下一轮 audit+repair 兜底。本断言验证三类处理数，终态一致性见下）
        WebhookOutboxAudit.DueAuditReport after = WebhookOutboxAudit.audit(fresh);
        assertThat(after.orphanCount()).isZero();
        // 陈旧清键后记录无索引 → 转为缺失（可再修）；直铺场景一轮修复到位：
        assertThat(after.missingCount()).isEqualTo(1); // stale 记录的索引被清（旧键），转缺失
        WebhookOutboxAudit.repair(fresh, after, new WebhookOutboxAudit.RepairOptions(false, false, true));
        assertThat(WebhookOutboxAudit.audit(fresh).clean()).isTrue();
    }

    @Test
    void cleanOutboxReportsCleanAndAuditDoesNotWrite() {
        InMemorySessionStateStore fresh = new InMemorySessionStateStore();
        new WebhookOutbox(fresh, 16).append("live", "t", "{}"); // 正常入队（双写在位）

        int keysBefore = fresh.getAll(WebhookOutbox.SESSION_ID).size();
        WebhookOutboxAudit.DueAuditReport report = WebhookOutboxAudit.audit(fresh);

        assertThat(report.clean()).isTrue();
        assertThat(report.recordCount()).isEqualTo(1);
        assertThat(fresh.getAll(WebhookOutbox.SESSION_ID).size()).isEqualTo(keysBefore); // 只读
    }

    // ---- helpers（写指定 store 实例） ----

    private static void putRecordInto(InMemorySessionStateStore target, String eventId,
            long nextAttemptAt, long createdAt) {
        String json = "{\"eventId\":\"" + eventId + "\",\"type\":\"t\",\"body\":\"{}\",\"seq\":1,"
                + "\"attempts\":0,\"nextAttemptAtEpochMs\":" + nextAttemptAt
                + ",\"createdAtEpochMs\":" + createdAt + "}";
        target.put(WebhookOutbox.SESSION_ID, new StateEntry(
                WebhookOutbox.OUTBOX_PREFIX + eventId, json, "webhook-outbox", 0, null, Instant.now()));
    }

    private static void putIndexInto(InMemorySessionStateStore target, String eventId, long ts) {
        target.put(WebhookOutbox.SESSION_ID, new StateEntry(
                WebhookOutbox.DUE_PREFIX + String.format("%016d", ts) + "." + eventId,
                eventId, "webhook-outbox", 0, null, Instant.now()));
    }
}
