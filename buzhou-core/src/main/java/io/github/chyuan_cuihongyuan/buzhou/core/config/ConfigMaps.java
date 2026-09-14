package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 装配层配置桥（ticket 22）：把 Spring {@link Environment} 的某个前缀子树绑成
 * {@code Map<String,Object>}，供各机制模块既有的 {@code fromYml(Map)} 入口复用（DRY，
 * 不重复定义一份 @ConfigurationProperties 字段表）。
 *
 * <p><b>叶子归一化</b>：Binder 对 {@code Object} 目标型保留来源原生类型——YAML 里
 * {@code true/123} 是 Boolean/Integer，但 {@code .properties} 全是 String。各模块
 * {@code fromYml} 普遍以 {@code instanceof Boolean / Number} 判定，直接喂 raw map 会在
 * {@code .properties} 源下失效。本类统一把 String 叶子按 {@code true/false→Boolean}、
 * 纯整数→{@code Long}、纯小数→{@code Double} 归一化，使两种来源行为一致。
 */
public final class ConfigMaps {

    private ConfigMaps() {
    }

    /** 绑定 {@code prefix} 子树为归一化 Map；前缀缺失返回空 Map。 */
    public static Map<String, Object> sub(Environment env, String prefix) {
        Map<String, Object> raw = Binder.get(env)
                .bind(prefix, Bindable.mapOf(String.class, Object.class))
                .orElseGet(Map::of);
        return normalize(raw);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> normalize(Map<String, Object> map) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (map != null) {
            map.forEach((k, v) -> out.put(k, normalizeValue(v)));
        }
        return out;
    }

    private static Object normalizeValue(Object v) {
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, val) -> out.put(String.valueOf(k), normalizeValue(val)));
            // spec 1510 / T2271：indexed 属性数字键归一——properties/命令行/env-var 源的
            // key[0].f=v 被 Binder 绑成 {key={0={f=v}}}（mapOf 弱点），一切 fromYml 以
            // instanceof List 消费的列表键在这些源下静默失效；键集全为十进制数字即按
            // 数值序转 List（字典序会 10<2 乱序）。真实数字键 map 在 YAML 语义不存在，
            // 归一安全；混合键保持 Map 不误伤
            if (!out.isEmpty() && out.keySet().stream().allMatch(ConfigMaps::isDecimalIndex)) {
                return out.entrySet().stream()
                        .sorted(java.util.Map.Entry.comparingByKey(
                                java.util.Comparator.comparingLong(e -> Long.parseLong(e))))
                        .map(java.util.Map.Entry::getValue)
                        .toList();
            }
            return out;
        }
        if (v instanceof List<?> l) {
            return l.stream().map(ConfigMaps::normalizeValue).toList();
        }
        if (v instanceof String s) {
            return coerceLeaf(s);
        }
        return v;
    }

    private static boolean isDecimalIndex(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        for (int i = 0; i < key.length(); i++) {
            if (!Character.isDigit(key.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static Object coerceLeaf(String s) {
        String t = s.trim();
        if ("true".equalsIgnoreCase(t)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(t)) {
            return Boolean.FALSE;
        }
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException ignored) {
            // 不是整数，继续尝试浮点
        }
        try {
            double d = Double.parseDouble(t);
            if (!Double.isNaN(d) && !Double.isInfinite(d)) {
                return d;
            }
        } catch (NumberFormatException ignored) {
            // 非数值，原样返回
        }
        return s;
    }
}
