package io.github.chyuan_cuihongyuan.buzhou.core.health;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * 生效配置 diff（spec 719 / T989，kubectl diff 借鉴——变化即清单）：两份配置
 * 快照（如 {@link BuzhouConfigSnapshotEndpoint} 全量视图）的三分类差异——
 * ADDED / REMOVED / CHANGED，不变键不出现；按 key 字典序稳定输出。
 *
 * <p><b>掩码语义</b>：消费方传入快照端点同源 Map（敏感值已 {@code ***} 掩码）——
 * 掩码值相等 = 未变；掩码底变化不可见（诚实边界）。diff 不做二次掩码（不猜
 * 敏感键——上游已定）。纯函数零副作用。
 */
public final class ConfigDiff {

    /** 变更类型。 */
    public enum Kind { ADDED, REMOVED, CHANGED }

    /** 单条变更（不可变）。 */
    public record Entry(String key, String before, String after, Kind kind) {
    }

    private ConfigDiff() {
    }

    /** 两份快照的差异（key 字典序；不可变）。 */
    public static List<Entry> diff(Map<String, String> before, Map<String, String> after) {
        if (before == null || after == null) {
            throw new IllegalArgumentException("before/after 快照非空（无差异用空表表达）");
        }
        TreeSet<String> keys = new TreeSet<>();
        keys.addAll(before.keySet());
        keys.addAll(after.keySet());
        List<Entry> out = new ArrayList<>();
        for (String key : keys) {
            String b = before.get(key);
            String a = after.get(key);
            if (b == null) {
                out.add(new Entry(key, null, a, Kind.ADDED));
            } else if (a == null) {
                out.add(new Entry(key, b, null, Kind.REMOVED));
            } else if (!Objects.equals(b, a)) {
                out.add(new Entry(key, b, a, Kind.CHANGED));
            }
            // 两有同值 = 未变——不出现
        }
        return List.copyOf(out);
    }
}
