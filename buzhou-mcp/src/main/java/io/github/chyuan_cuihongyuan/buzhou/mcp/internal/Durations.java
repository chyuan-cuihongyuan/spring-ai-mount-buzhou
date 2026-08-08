package io.github.chyuan_cuihongyuan.buzhou.mcp.internal;

import java.time.Duration;
import java.util.Map;

/**
 * 时长解析共享助手：支持 Duration 直传、ISO-8601（PT30S）、毫秒数与 30s/500ms/5m 后缀串。
 * 供 properties 清单解析与模块 fromYml 复用。
 */
public final class Durations {

    private Durations() {
    }

    /** 从 map 取键解析；缺失返回 null。 */
    public static Duration fromMap(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Duration d) {
            return d;
        }
        if (v instanceof Number n) {
            return Duration.ofMillis(n.longValue());
        }
        return parse(String.valueOf(v));
    }

    public static Duration parse(String text) {
        String s = text.trim().toLowerCase();
        if (s.startsWith("pt")) {
            return Duration.parse(text.trim());   // ISO-8601 原文解析（大小写敏感）
        }
        if (s.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(s.substring(0, s.length() - 2)));
        }
        if (s.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(s.substring(0, s.length() - 1)));
        }
        if (s.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(s.substring(0, s.length() - 1)));
        }
        return Duration.parse(text.trim());
    }
}
