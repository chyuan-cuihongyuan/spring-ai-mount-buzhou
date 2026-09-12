package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
}
