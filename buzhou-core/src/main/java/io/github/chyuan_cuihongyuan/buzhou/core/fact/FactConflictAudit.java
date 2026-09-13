package io.github.chyuan_cuihongyuan.buzhou.core.fact;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 共享事实冲突审计（spec 717 / T1034，mem0 冲突治理思想）：对事实快照
 * （导出集/合并产物/跨实例聚合）按键分组——同键异值（CONFLICT）与同键
 * 同值多 owner（DUPLICATE，重复发布信号）变结构化证据。
 *
 * <p>纯读数：所有权模型（410）守住单 store 写入，聚合场景防不住静默冲突；
 * 合并裁决是业务语义，本面只把「精神分裂」变可见。value 等价用
 * {@link Objects#equals}（保守口径）。rows 按键字典序。
 */
public final class FactConflictAudit {

    /** 冲突类别。 */
    public enum Kind { CONFLICT, DUPLICATE }

    /**
     * 单键发现（entries = owner=value 全列——裁决证据不截断；
     * DUPLICATE 时 value 唯一、owners ≥2）。
     */
    public record Row(Kind kind, String key, List<String> entries) {
    }

    /** 不可变报告（rows 字典序）。 */
    public record Report(List<Row> rows, int keysScanned, int conflictKeys, int duplicateKeys) {
    }

    private FactConflictAudit() {
    }

    /** 审计（null fail-fast；空表 = 零发现）。 */
    public static Report audit(List<SharedFact> facts) {
        Objects.requireNonNull(facts, "facts");
        Map<String, List<SharedFact>> byKey = new LinkedHashMap<>();
        for (SharedFact fact : facts) {
            if (fact == null) {
                continue;
            }
            byKey.computeIfAbsent(fact.key(), k -> new ArrayList<>()).add(fact);
        }
        List<Row> rows = new ArrayList<>();
        int conflicts = 0;
        int duplicates = 0;
        for (Map.Entry<String, List<SharedFact>> entry : new java.util.TreeMap<>(byKey).entrySet()) {
            List<SharedFact> group = entry.getValue();
            Set<Object> distinctValues = new LinkedHashSet<>();
            for (SharedFact fact : group) {
                distinctValues.add(fact.value());
            }
            if (distinctValues.size() > 1) {
                rows.add(new Row(Kind.CONFLICT, entry.getKey(), entriesOf(group)));
                conflicts++;
            } else if (distinctValues.size() == 1 && ownersOf(group).size() > 1) {
                rows.add(new Row(Kind.DUPLICATE, entry.getKey(), entriesOf(group)));
                duplicates++;
            }
        }
        return new Report(List.copyOf(rows), byKey.size(), conflicts, duplicates);
    }

    private static List<String> entriesOf(List<SharedFact> group) {
        List<String> entries = new ArrayList<>(group.size());
        for (SharedFact fact : group) {
            entries.add(fact.owner() + "=" + fact.value());
        }
        return entries;
    }

    private static Set<String> ownersOf(List<SharedFact> group) {
        Set<String> owners = new LinkedHashSet<>();
        for (SharedFact fact : group) {
            owners.add(fact.owner());
        }
        return owners;
    }
}
