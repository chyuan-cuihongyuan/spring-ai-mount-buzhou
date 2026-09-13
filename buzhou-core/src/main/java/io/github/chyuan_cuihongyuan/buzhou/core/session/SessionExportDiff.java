package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * impl-665 / spec 912：会话导出 diff 读面（spec 719 ConfigDiff 的会话导出域
 * 同构扩散——kubectl diff 直觉：排障问「两份导出差在哪」应有结构化答案而非
 * 人眼比 JSON）。
 *
 * <p>口径：sessionId 不同 fail-fast（跨会话对比无意义）；exportedAtEpochMs
 * 不参与（时戳非内容——spec 710 口径一致）；消息按 id 对齐分三桶（仅 A/仅 B/
 * 两侧语义字段不同）；各差异桶 {@value #MAX_DIFFS} 条封顶截断（identical
 * 布尔先于截断全量判定，不受封顶影响）。
 */
public final class SessionExportDiff {

    /** 单桶差异封顶（719 有界纪律同款）。 */
    public static final int MAX_DIFFS = 32;

    /** 标量字段差异。 */
    public record FieldDiff(String field, String valueA, String valueB) {
    }

    /** 消息差异（kind = added | removed | changed）。 */
    public record MessageDiff(String itemId, String kind, String detailA, String detailB) {
    }

    /** state/extensions 差异（kind = added | removed | changed）。 */
    public record StateDiff(String key, String kind, String valueA, String valueB) {
    }

    /** diff 总报告（identical 先全量判定——不受桶封顶截断影响）。 */
    public record DiffReport(boolean identical, List<FieldDiff> fieldDiffs,
                             List<MessageDiff> messageDiffs, List<StateDiff> stateDiffs,
                             List<StateDiff> extensionDiffs) {
    }

    private SessionExportDiff() {
    }

    /** 结构化对比（sessionId 不同 fail-fast；null 入参 fail-fast）。 */
    public static DiffReport between(SessionExport a, SessionExport b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("两份导出都必须非空");
        }
        if (!a.sessionId().equals(b.sessionId())) {
            throw new IllegalArgumentException("跨会话对比无意义（sessionId: "
                    + a.sessionId() + " vs " + b.sessionId() + "）");
        }
        boolean identical = true;
        List<FieldDiff> fieldDiffs = new ArrayList<>();
        List<MessageDiff> messageDiffs = new ArrayList<>();
        List<StateDiff> stateDiffs = new ArrayList<>();
        List<StateDiff> extensionDiffs = new ArrayList<>();

        // 标量字段（exportedAtEpochMs 时戳非内容——不比）
        identical &= compareField(fieldDiffs, "version",
                String.valueOf(a.version()), String.valueOf(b.version()));
        identical &= compareField(fieldDiffs, "appId", a.appId(), b.appId());
        identical &= compareField(fieldDiffs, "agentName", a.agentName(), b.agentName());
        identical &= compareField(fieldDiffs, "summary",
                String.valueOf(a.summary()), String.valueOf(b.summary()));

        // messages：按 id 对齐（LinkedHashMap 保序）
        Map<String, BuzhouMessage> msgsA = byId(a.messages());
        Map<String, BuzhouMessage> msgsB = byId(b.messages());
        Set<String> allMsgIds = new LinkedHashSet<>(msgsA.keySet());
        allMsgIds.addAll(msgsB.keySet());
        for (String id : allMsgIds) {
            BuzhouMessage ma = msgsA.get(id);
            BuzhouMessage mb = msgsB.get(id);
            if (ma == null) {
                messageDiffs.add(new MessageDiff(id, "added", null, describe(mb)));
            } else if (mb == null) {
                messageDiffs.add(new MessageDiff(id, "removed", describe(ma), null));
            } else if (!ma.content().equals(mb.content()) || ma.role() != mb.role()) {
                messageDiffs.add(new MessageDiff(id, "changed",
                        ma.role() + ":" + ma.content(), mb.role() + ":" + mb.content()));
            } else {
                continue;
            }
            identical = false;
            if (messageDiffs.size() >= MAX_DIFFS) {
                break;
            }
        }

        Map<String, String> stateValuesA = stateValues(a.state());
        Map<String, String> stateValuesB = stateValues(b.state());
        identical &= compareMaps(stateDiffs, "state", stateValuesA, stateValuesB);
        identical &= compareMaps(extensionDiffs, "extensions", a.extensions(), b.extensions());
        return new DiffReport(identical, List.copyOf(fieldDiffs), List.copyOf(messageDiffs),
                List.copyOf(stateDiffs), List.copyOf(extensionDiffs));
    }

    private static boolean compareField(List<FieldDiff> diffs, String field,
            String valueA, String valueB) {
        if (Objects.equals(valueA, valueB)) {
            return true;
        }
        if (diffs.size() < MAX_DIFFS) {
            diffs.add(new FieldDiff(field, valueA, valueB));
        }
        return false;
    }

    private static boolean compareMaps(List<StateDiff> diffs, String kind,
            Map<String, String> mapA, Map<String, String> mapB) {
        boolean same = true;
        Set<String> allKeys = new LinkedHashSet<>(mapA.keySet());
        allKeys.addAll(mapB.keySet());
        for (String key : allKeys) {
            String va = mapA.get(key);
            String vb = mapB.get(key);
            if (Objects.equals(va, vb)) {
                continue;
            }
            same = false;
            String kindTag = va == null ? "added" : (vb == null ? "removed" : "changed");
            if (diffs.size() < MAX_DIFFS) {
                diffs.add(new StateDiff(kind + "." + key, kindTag, va, vb));
            }
        }
        return same;
    }

    /** state 值投影（StateEntry.value——producer/turn 等元数据不参与内容比对）。 */
    private static Map<String, String> stateValues(Map<String, io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry> state) {
        Map<String, String> values = new LinkedHashMap<>();
        state.forEach((key, entry) -> values.put(key, entry.value()));
        return values;
    }

    private static Map<String, BuzhouMessage> byId(List<BuzhouMessage> messages) {
        Map<String, BuzhouMessage> byId = new LinkedHashMap<>();
        for (BuzhouMessage message : messages) {
            byId.put(message.id(), message);
        }
        return byId;
    }

    private static String describe(BuzhouMessage message) {
        return message.role() + ":" + message.content();
    }

}
