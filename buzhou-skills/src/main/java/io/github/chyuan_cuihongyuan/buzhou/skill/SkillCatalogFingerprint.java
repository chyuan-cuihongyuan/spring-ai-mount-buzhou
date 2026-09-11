package io.github.chyuan_cuihongyuan.buzhou.skill;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 技能目录清单指纹（spec 616 / T882，cosign/SBOM 清单思想；spec 175
 * ToolCatalogFingerprint 的 skills 镜像）：per 技能 {@code name →
 * sha256(description)} 指纹表 + 整体摘要（一版目录一条锚）；{@link #diff}
 * 三分类（ADDED/REMOVED/CHANGED）——技能目录（yml/DB 动态源）漂移可对账可审计。
 *
 * <p>纪律（与 175 同口径）：技能的「契约面」= name + description（技能无 schema；
 * description 是模型选技能的依据，属契约不是文档）；allowedTools 变更计 CHANGED——
 * 权限面变化必须显形。输入序不敏感（按 name 排序后哈希）。
 */
public final class SkillCatalogFingerprint {

    /** 两版目录的对账结果（三分类，稳定序）。 */
    public record Diff(List<String> added, List<String> removed, List<String> changed) {

        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty() && changed.isEmpty();
        }
    }

    private static final String HASH_ALGORITHM = "SHA-256";

    private final Map<String, String> fingerprints;
    private final String summaryHex;

    private SkillCatalogFingerprint(Map<String, String> fingerprints) {
        Map<String, String> sorted = new TreeMap<>(fingerprints);
        this.fingerprints = Map.copyOf(sorted);
        this.summaryHex = sha256(sorted.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((a, b) -> a + ";" + b)
                .orElse(""));
    }

    /** 从技能清单构建（null 安全 → 空目录）。 */
    public static SkillCatalogFingerprint of(List<SkillMetadata> catalog) {
        Map<String, String> out = new TreeMap<>();
        if (catalog != null) {
            for (SkillMetadata meta : catalog) {
                out.put(meta.name() == null ? "" : meta.name(), sha256(
                        (meta.description() == null ? "" : meta.description())
                                + "|" + meta.allowedTools()));
            }
        }
        return new SkillCatalogFingerprint(out);
    }

    /** 整体摘要（一版目录一条锚——入日志/工单对账）。 */
    public String summaryHex() {
        return summaryHex;
    }

    /** 技能数。 */
    public int size() {
        return fingerprints.size();
    }

    /** 单技能指纹（缺失 null）。 */
    public String fingerprintOf(String name) {
        return fingerprints.get(name);
    }

    /** 与另一版对账（三分类；name 消失 = REMOVED、新增 = ADDED、指纹变 = CHANGED）。 */
    public Diff diff(SkillCatalogFingerprint other) {
        if (other == null) {
            return new Diff(List.of(), List.copyOf(fingerprints.keySet()), List.of());
        }
        List<String> added = fingerprints.keySet().stream()
                .filter(n -> !other.fingerprints.containsKey(n)).sorted().toList();
        List<String> removed = other.fingerprints.keySet().stream()
                .filter(n -> !fingerprints.containsKey(n)).sorted().toList();
        List<String> changed = fingerprints.entrySet().stream()
                .filter(e -> other.fingerprints.containsKey(e.getKey())
                        && !other.fingerprints.get(e.getKey()).equals(e.getValue()))
                .map(Map.Entry::getKey).sorted().toList();
        return new Diff(added, removed, changed);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return HexFormat.of().formatHex(
                    digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 必须提供 " + HASH_ALGORITHM, e);
        }
    }
}
