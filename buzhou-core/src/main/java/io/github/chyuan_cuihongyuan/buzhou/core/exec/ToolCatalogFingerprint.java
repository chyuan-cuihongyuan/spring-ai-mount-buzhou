package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.springframework.ai.tool.definition.ToolDefinition;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 工具目录指纹（spec 175 / T543，SBOM/锁单 inventory diff 借鉴）：per 工具
 * {@code name → sha256(strip(schema))} 指纹表 + 整体摘要（一版目录一条锚）；
 * {@link #diff} 三分类（ADDED/REMOVED/CHANGED）——装配面工具漂移可对账可审计。
 *
 * <p>纪律：strip 后哈希（空白差不构成变更）；diff 只比 name+指纹——
 * description 变更不计（描述是文档不是契约）。与 MCP drift（spec 18 协议单源）
 * 互补：这是装配面全景。
 */
public final class ToolCatalogFingerprint {

    /** 两版目录的对账结果（三分类，稳定序）。 */
    public record Diff(List<String> added, List<String> removed, List<String> changed) {

        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty() && changed.isEmpty();
        }
    }

    private final Map<String, String> fingerprints;
    private final String summaryHex;

    private ToolCatalogFingerprint(Map<String, String> fingerprints) {
        Map<String, String> sorted = new TreeMap<>(fingerprints);
        this.fingerprints = Map.copyOf(sorted);
        this.summaryHex = sha256(sorted.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + ";" + b)
                .orElse(""));
    }

    /** 从工具定义表构建（null 安全 → 空目录）。 */
    public static ToolCatalogFingerprint of(List<ToolDefinition> definitions) {
        Map<String, String> out = new TreeMap<>();
        if (definitions != null) {
            for (ToolDefinition definition : definitions) {
                out.put(definition.name(), sha256(definition.inputSchema()));
            }
        }
        return new ToolCatalogFingerprint(out);
    }

    /** 整体摘要（一版目录一条锚——入日志/工单对账）。 */
    public String summaryHex() {
        return summaryHex;
    }

    /** 工具数。 */
    public int size() {
        return fingerprints.size();
    }

    /** 单工具指纹（无 = null）。 */
    public String fingerprintOf(String toolName) {
        return fingerprints.get(toolName);
    }

    /** 对账：本版 → other 的三分类（added = other 有本无）。 */
    public Diff diff(ToolCatalogFingerprint other) {
        ToolCatalogFingerprint target = other == null
                ? ToolCatalogFingerprint.of(null) : other;
        List<String> added = target.fingerprints.keySet().stream()
                .filter(name -> !fingerprints.containsKey(name))
                .toList();
        List<String> removed = fingerprints.keySet().stream()
                .filter(name -> !target.fingerprints.containsKey(name))
                .toList();
        List<String> changed = fingerprints.entrySet().stream()
                .filter(e -> target.fingerprints.containsKey(e.getKey())
                        && !target.fingerprints.get(e.getKey()).equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        return new Diff(added, removed, changed);
    }

    private static String sha256(String text) {
        String normalized = text == null ? "" : text.strip();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "unhashed:" + normalized.hashCode();
        }
    }
}
