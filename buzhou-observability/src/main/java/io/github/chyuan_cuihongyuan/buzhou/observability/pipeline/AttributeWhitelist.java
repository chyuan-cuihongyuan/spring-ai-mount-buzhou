package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 属性白名单过滤器（spec 2045 / T3191 / impl 1596）——OpenTelemetry
 * View / processor 思想：观测导出前的属性瘦身——白名单内属性保留、
 * 盘外属性**聚合丢弃计数**（dropped 显形——被丢属性不静默：哪个属性
 * 被丢多少次一眼可见，配额治理有据）。默认策略可选通配放行（
 * allowAll——未配置白名单即全放，不因治理而断流）。
 *
 * <p>纯函数无状态、确定性；过滤逐属性（value 原样传递）。
 */
public final class AttributeWhitelist {

    /** 过滤结果：留存属性 + 丢弃计数（聚合面）。 */
    public record Filtered(Map<String, Object> retained, Map<String, Long> droppedByAttribute) {
    }

    private final boolean allowAll;
    private final Set<String> allowed;

    /** 白名单模式（集合字典序稳定；空集合 = 全拒——显式收紧口径）。 */
    public AttributeWhitelist(List<String> allowedAttributes) {
        if (allowedAttributes == null) {
            throw new IllegalArgumentException("allowedAttributes 不能为 null（空表=全拒；通配用 allowAll）");
        }
        allowedAttributes.forEach(a -> {
            if (a == null || a.isBlank()) {
                throw new IllegalArgumentException("白名单项不能为空");
            }
        });
        this.allowAll = false;
        this.allowed = new TreeSet<>(allowedAttributes);
    }

    private AttributeWhitelist() {
        this.allowAll = true;
        this.allowed = Set.of();
    }

    /** 通配放行件（治理未配置时不因治理断流——全保留零丢弃）。 */
    public static AttributeWhitelist allowAll() {
        return new AttributeWhitelist();
    }

    /** 过滤：白名单内保留、盘外计数丢弃；通配全保留。契约：attributes 非 null。 */
    public Filtered filter(Map<String, Object> attributes) {
        if (attributes == null) {
            throw new IllegalArgumentException("attributes 不能为 null");
        }
        Map<String, Object> retained = new LinkedHashMap<>();
        Map<String, Long> dropped = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : attributes.entrySet()) {
            if (allowAll || allowed.contains(e.getKey())) {
                retained.put(e.getKey(), e.getValue());
            } else {
                dropped.merge(e.getKey(), 1L, Long::sum);
            }
        }
        return new Filtered(retained, dropped);
    }

    /** 白名单面（字典序快照——TreeSet 兑现有序承诺；通配返回空——语义为全放非零放）。 */
    public java.util.Set<String> allowedAttributes() {
        return new java.util.TreeSet<>(allowed);
    }

    /** 是否通配模式。 */
    public boolean isAllowAll() {
        return allowAll;
    }
}
