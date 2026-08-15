package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 内容寻址完整性（wayfinder2 impl-17 / T45 / docs/spec/12 §spill-17，git 惯例：对象名即内容
 * hash、读回重算必校验）：sha256 工具 + 回读 envelope 形状 + 校验谓词。
 * chunk 级 = 返回切片的 sha256；whole 级 = 完整落盘内容 sha256（meta 记录、读回复验）。
 */
public final class ReadIntegrity {

    private ReadIntegrity() {
    }

    /** 回读完整性 envelope：随切片附带的密码学证明（模型/调用方可自行复验 chunk）。 */
    public record IntegrityEnvelope(String data, String byteRange, String chunkSha256,
                                    String wholeSha256) {
    }

    public static String sha256(String content) {
        if (content == null) {
            content = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException(
                    io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode.CONFIG_INVALID,
                    "运行环境缺少 SHA-256（JVM 安全提供者异常）", e);
        }
    }

    /** 为一次回读构造 envelope（chunk = 切片；whole = 完整内容）。 */
    public static IntegrityEnvelope envelope(String data, int offset, int length, String wholeContent) {
        return new IntegrityEnvelope(data, offset + ".." + (offset + length),
                sha256(data), sha256(wholeContent));
    }

    /** 复验 chunk：data 的 sha256 与 envelope 声明一致。 */
    public static boolean chunkVerified(IntegrityEnvelope envelope) {
        return envelope != null && sha256(envelope.data()).equals(envelope.chunkSha256());
    }

    /** 完整性告警前缀（读侧 lenient=warning 透传语义；写侧 strict 阻断走 Onload 既有语义）。 */
    public static final String CORRUPTION_WARNING =
            "[完整性告警：spill 内容与落盘摘要不一致（可能已损坏或被篡改），请谨慎使用]";
}
