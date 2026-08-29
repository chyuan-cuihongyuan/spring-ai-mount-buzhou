package io.github.chyuan_cuihongyuan.buzhou.core.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 生效配置指纹（spec 187 / T559，SBOM 思想延续到配置面）：键排序 + 值 strip
 * 归一 → 键级指纹表 + 整体 summaryHex（一版配置一条锚）；diff 三分类——
 * 配置漂移可对账。null 键/值跳过（提取侧质量不炸审计侧）；纯函数零 Spring
 * 绑定（键集提取归宿主）。
 */
public final class ConfigFingerprint {

    /** 两版配置对账（三分类，稳定序）。 */
    public record Diff(List<String> added, List<String> removed, List<String> changed) {

        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty() && changed.isEmpty();
        }
    }

    private final Map<String, String> normalized;
    private final String summaryHex;

    private ConfigFingerprint(Map<String, String> normalized) {
        this.normalized = Map.copyOf(normalized);
        this.summaryHex = sha256(normalized.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + ";" + b)
                .orElse(""));
    }

    /** 构建（null 键/值跳过；值 strip 归一）。 */
    public static ConfigFingerprint of(Map<String, String> effectiveConfig) {
        Map<String, String> normalized = new TreeMap<>();
        if (effectiveConfig != null) {
            effectiveConfig.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null) {
                    normalized.put(key.strip(), value.strip());
                }
            });
        }
        return new ConfigFingerprint(normalized);
    }

    /** 整体摘要（部署记录锚）。 */
    public String summaryHex() {
        return summaryHex;
    }

    /** 键数。 */
    public int size() {
        return normalized.size();
    }

    /** 单键值（无 = null）。 */
    public String valueOf(String key) {
        return normalized.get(key);
    }

    /** 对账：本版 → other 的三分类。 */
    public Diff diff(ConfigFingerprint other) {
        ConfigFingerprint target = other == null ? ConfigFingerprint.of(null) : other;
        List<String> added = target.normalized.keySet().stream()
                .filter(k -> !normalized.containsKey(k)).sorted().toList();
        List<String> removed = normalized.keySet().stream()
                .filter(k -> !target.normalized.containsKey(k)).sorted().toList();
        List<String> changed = normalized.entrySet().stream()
                .filter(e -> target.normalized.containsKey(e.getKey())
                        && !target.normalized.get(e.getKey()).equals(e.getValue()))
                .map(Map.Entry::getKey).sorted().toList();
        return new Diff(added, removed, changed);
    }

    /** 观测视图（稳定序拷贝）。 */
    public Map<String, String> asMap() {
        return new HashMap<>(normalized);
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "unhashed:" + text.hashCode();
        }
    }
}
