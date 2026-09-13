package io.github.chyuan_cuihongyuan.buzhou.core.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 导出防篡改清单（spec 193 / T565，OCI manifest / TUF 借鉴）：逐会话
 * sha256(strip(content)) 摘要 + 总摘要的 manifest JSON；verify 三列校验
 * （mismatched/missing/unexpected）——合规搬运的完整性凭证。纯函数，
 * 不绑定导出管线（装配侧组合 exportSession 与本类）。
 */
public final class ExportManifest {

    /** verify 结果。 */
    public record Verification(boolean ok, List<String> mismatchedIds,
                               List<String> missingIds, List<String> unexpectedIds) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, String> digests = new TreeMap<>();

    /** 逐项登记（同 id 覆盖；content 非 null）。 */
    public ExportManifest add(String sessionId, String contentJson) {
        if (sessionId == null || sessionId.isBlank() || contentJson == null) {
            throw new IllegalArgumentException("sessionId 非空、content 非 null");
        }
        digests.put(sessionId, sha256(contentJson.strip()));
        return this;
    }

    /**
     * impl-687 / spec 935：规范化登记——JSON 树递归键排序后 sha256（spec 911 JCS
     * 同源单点实现 SessionExportChecksum.canonicalJson）。键序漂移不误报 mismatch；
     * 与 {@link #add} 并存（同批登记用同一方法即自洽——混用由运维纪律约束）。
     */
    public ExportManifest addCanonical(String sessionId, String contentJson) {
        if (sessionId == null || sessionId.isBlank() || contentJson == null) {
            throw new IllegalArgumentException("sessionId 非空、content 非 null");
        }
        digests.put(sessionId, sha256(SessionExportChecksum.canonicalJson(contentJson).strip()));
        return this;
    }

    /** 清单 JSON（entries 按 id 序 + totalDigest 一票总凭证）。 */
    public String manifestJson() {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            ArrayNode entries = root.putArray("entries");
            digests.forEach((id, digest) -> {
                ObjectNode entry = entries.addObject();
                entry.put("id", id);
                entry.put("digest", digest);
            });
            root.put("totalDigest", totalDigest());
            return MAPPER.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("manifest 序列化失败", e);
        }
    }

    /** 总摘要（对 id=digest;… 归一串再哈希——顺序无关）。 */
    public String totalDigest() {
        StringBuilder canonical = new StringBuilder();
        digests.forEach((id, digest) -> canonical.append(id).append('=')
                .append(digest).append(';'));
        return sha256(canonical.toString());
    }

    /**
     * impl-687 / spec 935：规范化校验——与 {@link #addCanonical} 配对（内容先
     * canonicalJson 再比对；「口径必须成对」教训同 spec 913）。键序漂移不误报。
     */
    public static Verification verifyCanonical(String manifestJson, Map<String, String> contents) {
        if (contents == null) {
            return verify(manifestJson, null);
        }
        Map<String, String> canonicalized = new LinkedHashMap<>();
        contents.forEach((id, content) -> {
            if (id != null && content != null) {
                canonicalized.put(id, SessionExportChecksum.canonicalJson(content));
            }
        });
        return verify(manifestJson, canonicalized);
    }

    /**
     * impl-690 / spec 941：子集校验（增量搬运场景——只核对提供的会话子集，
     * manifest 中未提供的条目不计 missing/unexpected；rsync --partial 思想）。
     * 与 {@link #verifyCanonical} 配对使用（规范化口径）。
     */
    public static Verification verifySubset(String manifestJson, Map<String, String> contents) {
        if (contents == null || contents.isEmpty()) {
            throw new IllegalArgumentException("contents 必须非空（子集校验至少验一项；全量校验用 verify）");
        }
        Map<String, String> canonicalized = new LinkedHashMap<>();
        contents.forEach((id, content) -> {
            if (id != null && content != null) {
                canonicalized.put(id, SessionExportChecksum.canonicalJson(content));
            }
        });
        try {
            JsonNode root = MAPPER.readTree(manifestJson);
            Map<String, String> expected = new LinkedHashMap<>();
            for (JsonNode entry : root.path("entries")) {
                expected.put(entry.path("id").asText(), entry.path("digest").asText());
            }
            List<String> mismatched = new ArrayList<>();
            for (Map.Entry<String, String> e : canonicalized.entrySet()) {
                String manifestDigest = expected.get(e.getKey());
                if (manifestDigest == null) {
                    mismatched.add(e.getKey()); // 子集含 manifest 没有的会话 = 异常
                } else if (!sha256(e.getValue().strip()).equals(manifestDigest)) {
                    mismatched.add(e.getKey());
                }
            }
            boolean ok = mismatched.isEmpty();
            return new Verification(ok, mismatched, List.of(), List.of());
        } catch (RuntimeException e) {
            throw new IllegalStateException("manifest 解析失败（凭证损坏）", e);
        } catch (Exception e) {
            throw new IllegalStateException("manifest 解析失败（凭证损坏）", e);
        }
    }

    /**
     * 校验：manifest（本类产出的 JSON）对照实际内容集——逐项核对 + 缺失/
     * 多出列出；manifest 解析失败抛 IllegalStateException（凭证损坏是硬错）。
     */
    public static Verification verify(String manifestJson, Map<String, String> contents) {
        try {
            JsonNode root = MAPPER.readTree(manifestJson);
            Map<String, String> expected = new LinkedHashMap<>();
            for (JsonNode entry : root.path("entries")) {
                expected.put(entry.path("id").asText(), entry.path("digest").asText());
            }
            Map<String, String> actual = new TreeMap<>();
            if (contents != null) {
                contents.forEach((id, content) -> {
                    if (id != null && content != null) {
                        actual.put(id, sha256(content.strip()));
                    }
                });
            }
            List<String> mismatched = expected.entrySet().stream()
                    .filter(e -> actual.containsKey(e.getKey())
                            && !actual.get(e.getKey()).equals(e.getValue()))
                    .map(Map.Entry::getKey).toList();
            List<String> missing = expected.keySet().stream()
                    .filter(id -> !actual.containsKey(id)).toList();
            List<String> unexpected = actual.keySet().stream()
                    .filter(id -> !expected.containsKey(id)).toList();
            boolean ok = mismatched.isEmpty() && missing.isEmpty() && unexpected.isEmpty()
                    && expected.size() == actual.size();
            return new Verification(ok, mismatched, missing, unexpected);
        } catch (RuntimeException e) {
            throw new IllegalStateException("manifest 解析失败（凭证损坏）", e);
        } catch (Exception e) {
            throw new IllegalStateException("manifest 解析失败（凭证损坏）", e);
        }
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "unhashed:" + text.hashCode();
        }
    }
}
