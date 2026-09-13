package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/**
 * 会话导出校验和（spec 547 / T853——511 密文封缄的明文通道对偶；S3
 * checksum 同思想）：明文导出 JSON 旁写 sha256 校验和，导入前验校——
 * 传输/存储衰变在 fromJson 语义错误前被发现（损坏的 JSON 可能恰好仍可
 * 解析但内容已变——校验和是更强证据）。
 *
 * <p>与 511 归档校验和同 doctrine：防衰变/误写，不防蓄意同改（蓄意归
 * 510 密文封缄）。
 */
public final class SessionExportChecksum {

    /** 校验和前缀（版本化）。 */
    public static final String CHECKSUM_PREFIX = "sha256:";

    private SessionExportChecksum() {
    }

    /** 计算导出 JSON 的校验和（sha256 hex）。 */
    public static String of(String exportJson) {
        if (exportJson == null || exportJson.isEmpty()) {
            throw new IllegalArgumentException("导出 JSON 非空");
        }
        return CHECKSUM_PREFIX + sha256Hex(exportJson);
    }

    /** 验校：checksum 与导出 JSON 一致 true；不一致/格式不符 false。 */
    public static boolean verify(String exportJson, String checksum) {
        if (exportJson == null || exportJson.isEmpty()
                || checksum == null || !checksum.startsWith(CHECKSUM_PREFIX)) {
            return false;
        }
        return of(exportJson).equals(checksum);
    }

    /** 内容指纹前缀（版本化——与整体校验和 {@value #CHECKSUM_PREFIX} 显式区分防混用）。 */
    public static final String CONTENT_PREFIX = "sha256-c:";

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 内容指纹（spec 710 / T971——HTTP ETag 语义）：对导出的<b>内容投影</b>
     * （显式剔除 exportedAtEpochMs——导出时间不属内容）做 canonical JSON → sha256。
     * 同内容异时戳的两次导出指纹相同——协商（SessionExportConditional）与同步方
     * 去重的键。与 {@link #of}（整体 JSON 校验和，含时间戳）语义不同不可互换。
     */
    public static String contentFingerprint(SessionExport export) {
        if (export == null) {
            throw new IllegalArgumentException("export 非空");
        }
        try {
            Map<String, Object> doc = MAPPER.readValue(export.toJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<
                            java.util.LinkedHashMap<String, Object>>() {
                    });
            doc.remove("exportedAtEpochMs");
            return CONTENT_PREFIX + sha256Hex(MAPPER.writeValueAsString(doc));
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("内容指纹计算失败", e);
        }
    }

    private static String sha256Hex(String payload) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * impl-664 / spec 911：规范化内容指纹前缀（与 {@link #CONTENT_PREFIX} 显式区分
     * 防混用——710 前缀纪律：整体校验和/内容指纹/规范化指纹三值语义不同不可互换）。
     */
    public static final String CANONICAL_PREFIX = "sha256-j:";

    /**
     * 规范化内容指纹（RFC 8785 JCS 思想——指纹绑定内容而非序列化键序）：内容投影
     * 同 {@link #contentFingerprint}（剔除 exportedAtEpochMs），但反序列化树
     * <b>递归排序全部 Map 键</b>（字典序；List 元素保序——数组有序是语义）后求和。
     * 嵌套 metadata/state 键序跨实现漂移不再改变指纹。
     *
     * <p>诚实边界：数字按 Jackson 文本原样（不做 RFC 8785 §3.1 数字规范化完整
     * 实现——Java 生态内单产单消场景数字文本稳定，入档）。
     */
    public static String canonicalContentFingerprint(SessionExport export) {
        if (export == null) {
            throw new IllegalArgumentException("export 非空");
        }
        try {
            Map<String, Object> doc = MAPPER.readValue(export.toJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<
                            java.util.LinkedHashMap<String, Object>>() {
                    });
            doc.remove("exportedAtEpochMs");
            Map<String, Object> canonical = canonicalizeMap(doc);
            return CANONICAL_PREFIX + sha256Hex(MAPPER.writeValueAsString(canonical));
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("规范化指纹计算失败", e);
        }
    }

    /**
     * impl-687 / spec 935：单点规范化入口（供同包 ExportManifest 等复用）——
     * 解析 JSON → 递归 Map 键排序 → 紧凑序列化。非法 JSON 抛 IllegalArgumentException。
     */
    static String canonicalJson(String json) {
        try {
            // readValue 到 LinkedHashMap（非 readTree JsonNode）——树落在 java.util.Map
            // 分支，递归键排序才能生效
            Map<String, Object> root = MAPPER.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<
                            java.util.LinkedHashMap<String, Object>>() {
                    });
            Map<String, Object> canonical = canonicalizeMap(root);
            return MAPPER.writeValueAsString(canonical);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("规范化解析失败", e);
        }
    }

    private static Object canonicalizeAny(Object node) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new java.util.TreeMap<>();
            map.forEach((key, value) -> sorted.put(String.valueOf(key), canonicalizeAny(value)));
            return sorted;
        }
        if (node instanceof List<?> list) {
            List<Object> copied = new ArrayList<>(list.size());
            for (Object element : list) {
                copied.add(canonicalizeAny(element));
            }
            return copied;
        }
        return node;
    }

    /** 顶层规范化：doc 恒为 Map → 递归排序键。 */
    private static Map<String, Object> canonicalizeMap(Map<String, Object> doc) {
        Map<String, Object> sorted = new java.util.TreeMap<>();
        doc.forEach((key, value) -> sorted.put(key, canonicalize(value)));
        return sorted;
    }

    /** 递归键排序规范化：Map → TreeMap 字典序；List 逐元素递归；标量原样。 */
    private static Object canonicalize(Object node) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new java.util.TreeMap<>();
            map.forEach((key, value) -> sorted.put(String.valueOf(key), canonicalize(value)));
            return sorted;
        }
        if (node instanceof List<?> list) {
            List<Object> copied = new java.util.ArrayList<>(list.size());
            for (Object element : list) {
                copied.add(canonicalize(element));
            }
            return copied;
        }
        return node;
    }
}
