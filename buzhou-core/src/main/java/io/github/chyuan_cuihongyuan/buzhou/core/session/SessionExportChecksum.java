package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(exportJson.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return CHECKSUM_PREFIX + hex;
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /** 验校：checksum 与导出 JSON 一致 true；不一致/格式不符 false。 */
    public static boolean verify(String exportJson, String checksum) {
        if (exportJson == null || exportJson.isEmpty()
                || checksum == null || !checksum.startsWith(CHECKSUM_PREFIX)) {
            return false;
        }
        return of(exportJson).equals(checksum);
    }
}
