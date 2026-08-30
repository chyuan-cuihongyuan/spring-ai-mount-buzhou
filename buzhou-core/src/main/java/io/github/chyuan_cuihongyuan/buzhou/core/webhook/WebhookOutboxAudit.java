package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * outbox due 索引审计（spec 96 §A / T361，spec 79 fog 项收口；StoreFsck 同思想——
 * 只读报告 + 按项修复）：due-time 索引（{@code due.<零垫ts>.<eventId>} 双写结构）
 * 的三类失真——
 * <ul>
 *   <li><b>孤儿</b>：索引指向的记录已删（正常由 due() 读路径自愈，审计可提前发现）；</li>
 *   <li><b>陈旧</b>：索引 ts 与记录当前 nextAttemptAt 不一致（update 迁键竞窗残留）；</li>
 *   <li><b>缺失</b>：outbox 记录无任何对应 due 索引（索引丢失——调度器将永远看不到
 *       该记录，投递停摆，须修复）。</li>
 * </ul>
 * 纯静态只读（{@link #audit} 不实例化 WebhookOutbox——构造期回填是写副作用）；
 * {@link #repair} 按项清/补。样本键有界各 {@value #SAMPLE_LIMIT} 条。
 */
public final class WebhookOutboxAudit {

    /** 单类样本上限。 */
    public static final int SAMPLE_LIMIT = 10;

    /** 审计报告（三类计数 + 样本键 + 在册记录/索引总数）。 */
    public record DueAuditReport(int orphanCount, int staleCount, int missingCount,
                                 int recordCount, int indexCount,
                                 List<String> orphanSamples, List<String> staleSamples,
                                 List<String> missingSamples) {

        public boolean clean() {
            return orphanCount == 0 && staleCount == 0 && missingCount == 0;
        }
    }

    private WebhookOutboxAudit() {
    }

    /** 只读对账（全量扫 outbox.* 与 due.*——审计是一次性运维动作，不做增量）。 */
    public static DueAuditReport audit(SessionStateStore store) {
        Map<String, String> sessionIdByIndexKey = new HashMap<>();
        Map<String, Long> tsByIndexKey = new HashMap<>();
        int indexCount = 0;
        for (Map.Entry<String, StateEntry> e
                : store.scanByPrefix(WebhookOutbox.SESSION_ID, WebhookOutbox.DUE_PREFIX).entrySet()) {
            indexCount++;
            String indexKey = e.getKey();
            String rest = indexKey.substring(WebhookOutbox.DUE_PREFIX.length());
            String ts = rest.substring(0, rest.indexOf('.'));
            String eventId = rest.substring(rest.indexOf('.') + 1);
            sessionIdByIndexKey.put(eventId, indexKey);
            tsByIndexKey.put(indexKey, Long.parseLong(ts));
        }

        int orphan = 0;
        int stale = 0;
        int missing = 0;
        int records = 0;
        List<String> orphanSamples = new ArrayList<>();
        List<String> staleSamples = new ArrayList<>();
        List<String> missingSamples = new ArrayList<>();
        for (Map.Entry<String, StateEntry> e
                : store.scanByPrefix(WebhookOutbox.SESSION_ID, WebhookOutbox.OUTBOX_PREFIX).entrySet()) {
            records++;
            String eventId = e.getKey().substring(WebhookOutbox.OUTBOX_PREFIX.length());
            WebhookOutbox.OutboxRecord record = parseRecord(e.getValue().value());
            if (record == null) {
                continue; // 损坏记录走 due() 隔离路径，不重复判
            }
            String indexKey = sessionIdByIndexKey.remove(eventId);
            if (indexKey == null) {
                missing++;
                if (missingSamples.size() < SAMPLE_LIMIT) {
                    missingSamples.add(e.getKey());
                }
                continue;
            }
            if (tsByIndexKey.get(indexKey) != record.nextAttemptAtEpochMs()) {
                stale++;
                if (staleSamples.size() < SAMPLE_LIMIT) {
                    staleSamples.add(indexKey);
                }
            }
        }
        // 剩余索引 = 指向已删记录（孤儿）
        for (String leftover : sessionIdByIndexKey.values()) {
            orphan++;
            if (orphanSamples.size() < SAMPLE_LIMIT) {
                orphanSamples.add(leftover);
            }
        }
        return new DueAuditReport(orphan, stale, missing, records, indexCount,
                List.copyOf(orphanSamples), List.copyOf(staleSamples), List.copyOf(missingSamples));
    }

    /** 修复选项（默认全 false——safe-by-default 同 fsck）。 */
    public record RepairOptions(boolean removeOrphans, boolean removeStale, boolean rebuildMissing) {
        public static RepairOptions none() {
            return new RepairOptions(false, false, false);
        }
    }

    /** 按项修复；返回各项实际处理数（孤儿/陈旧清键、缺失重建索引）。 */
    public static Map<String, Integer> repair(SessionStateStore store, DueAuditReport report,
            RepairOptions options) {
        Map<String, Integer> repaired = new LinkedHashMap<>();
        if (options.removeOrphans()) {
            int n = 0;
            for (String key : report.orphanSamples()) {
                store.delete(WebhookOutbox.SESSION_ID, key);
                n++;
            }
            repaired.put("orphans", n);
        }
        if (options.removeStale()) {
            int n = 0;
            for (String key : report.staleSamples()) {
                store.delete(WebhookOutbox.SESSION_ID, key);
                n++;
            }
            repaired.put("stale", n);
        }
        if (options.rebuildMissing()) {
            int n = 0;
            for (String recordKey : report.missingSamples()) {
                String eventId = recordKey.substring(WebhookOutbox.OUTBOX_PREFIX.length());
                store.get(WebhookOutbox.SESSION_ID, recordKey)
                        .ifPresent(entry -> {
                            WebhookOutbox.OutboxRecord record = parseRecord(entry.value());
                            if (record != null) {
                                store.put(WebhookOutbox.SESSION_ID, new StateEntry(
                                        WebhookOutbox.DUE_PREFIX
                                                + String.format("%016d", record.nextAttemptAtEpochMs())
                                                + "." + eventId,
                                        eventId, "webhook-outbox", 0, null, Instant.now()));
                            }
                        });
                n++;
            }
            repaired.put("missing", n);
        }
        return repaired;
    }

    private static WebhookOutbox.OutboxRecord parseRecord(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, WebhookOutbox.OutboxRecord.class);
        } catch (Exception e) {
            return null;
        }
    }
}
