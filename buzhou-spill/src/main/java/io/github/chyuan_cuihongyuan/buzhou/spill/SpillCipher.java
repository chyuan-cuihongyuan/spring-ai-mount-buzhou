package io.github.chyuan_cuihongyuan.buzhou.spill;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * spill 落盘静态加密（spec 40 §A / T151 / impl-122）：JDK AES-256-GCM 信封，零新依赖。
 *
 * <p>wire 格式：首行魔法 {@code BUZHOU-ENC-V1} + 换行 + Base64(随机 12 字节 IV ‖ 密文+GCM tag)。
 * 每次加密随机 IV——同明文两次落盘密文不同。读侧以魔法前缀探测：无魔法视为旧明文直通
 * （向后兼容），有魔法则解密；GCM 验签失败（密钥错配/文件损坏）快速失败。
 *
 * <p>密钥经 {@link #fromBase64Key(String)} 构造（Base64 编码的 32 字节 AES-256 密钥），
 * 非法长度/编码构造期即拒。语义借鉴 Dify（凭据 AES-GCM 加密），实现纯 JDK。
 *
 * @since 1.0.0
 */
public final class SpillCipher {

    /** 加密文件首行魔法（版本化：格式演进换 V2，读侧只认 V1）。 */
    public static final String MAGIC = "BUZHOU-ENC-V1";

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    private SpillCipher(SecretKeySpec key) {
        this.key = key;
    }

    /** Base64 编码 32 字节密钥构造（AES-256）；长度/编码非法立即拒绝。 */
    public static SpillCipher fromBase64Key(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("encryption-key 不可为空");
        }
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("encryption-key 不是合法 Base64", e);
        }
        if (raw.length != 32) {
            throw new IllegalArgumentException("encryption-key 解码后必须为 32 字节（AES-256），收到 "
                    + raw.length + " 字节");
        }
        return new SpillCipher(new SecretKeySpec(raw, "AES"));
    }

    /** 是否为加密 wire 格式（魔法前缀探测；读侧兼容旧明文的判据）。 */
    public static boolean isEncrypted(String content) {
        return content != null && content.startsWith(MAGIC + "\n");
    }

    /** 明文 → wire 格式密文。 */
    public String encrypt(String plaintext) {
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] wire = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, wire, 0, iv.length);
            System.arraycopy(ciphertext, 0, wire, iv.length, ciphertext.length);
            return MAGIC + "\n" + Base64.getEncoder().encodeToString(wire);
        } catch (Exception e) {
            throw new IllegalStateException("spill 加密失败", e);
        }
    }

    /**
     * 密文（wire 格式）→ 明文；无魔法前缀的原样返回（旧明文兼容）。
     *
     * @throws IllegalStateException GCM 验签失败（密钥不匹配或文件损坏）
     */
    public String decryptIfEncrypted(String content) {
        if (!isEncrypted(content)) {
            return content;
        }
        byte[] wire;
        try {
            wire = Base64.getDecoder().decode(content.substring(MAGIC.length() + 1));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("spill 加密文件损坏（Base64 解码失败）", e);
        }
        if (wire.length <= IV_BYTES) {
            throw new IllegalStateException("spill 加密文件损坏（长度不足）");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(TAG_BITS, wire, 0, IV_BYTES));
            byte[] plaintext = cipher.doFinal(wire, IV_BYTES, wire.length - IV_BYTES);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "spill 解密失败（密钥不匹配或文件损坏）——检查 buzhou.spill.encryption-key 是否为"
                            + "写入该文件时使用的密钥", e);
        }
    }
}
