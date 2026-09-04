package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 应用层信封加密（spec 333 / T657，Vault transit / AWS KMS envelope
 * 借鉴——密钥不出进程，存储只见密文）。
 *
 * <ul>
 *   <li><b>算法</b>AES-256-GCM：随机 12 字节 IV + 128 位认证标签；</li>
 *   <li><b>信封格式</b>{@code buzhou:v1:<keyId>:<base64(iv‖ct+tag)>}——
 *       keyId = 主钥 SHA-256 前 8 字节 hex（信封自描述，解密按 keyId 路由）；</li>
 *   <li><b>AAD 绑定</b>：密文与调用方给的附加认证数据绑定——剪贴到别的
 *       AAD（消息/会话错位）认证失败；</li>
 *   <li><b>轮换面</b>：当前钥加密、当前+前代双钥解密（写新读旧，平滑换钥；
 *       新写入永远用新钥，密文面自然收敛）；</li>
 *   <li><b>完整性优先</b>：GCM 认证失败上抛（篡改宁可炸不可静默）。</li>
 * </ul>
 */
public final class EnvelopeCipher {

    public static final String ENVELOPE_PREFIX = "buzhou:v1:";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final KeyRing current;
    private final KeyRing previous; // 可空——无轮换窗口

    private record KeyRing(String keyId, SecretKey key) {
    }

    /**
     * @param masterKeyBase64        当前主钥（Base64，16/24/32 字节 AES 钥）
     * @param previousMasterKeyBase64 前代主钥（可空——仅解密轮换窗口）
     */
    public EnvelopeCipher(String masterKeyBase64, String previousMasterKeyBase64) {
        this.current = keyRing(masterKeyBase64);
        this.previous = previousMasterKeyBase64 == null || previousMasterKeyBase64.isBlank()
                ? null : keyRing(previousMasterKeyBase64);
        if (this.previous != null && this.previous.keyId.equals(this.current.keyId)) {
            throw new IllegalArgumentException(
                    "previous-master-key 与 master-key 相同——轮换窗口无意义（换一把真钥）");
        }
    }

    /** 加密为信封（当前钥；AAD 绑定防剪贴）。 */
    public String encrypt(String plaintext, String aad) {
        if (plaintext == null) {
            return null;
        }
        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, current.key(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aadBytes(aad));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] ivAndCt = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, ivAndCt, 0, iv.length);
            System.arraycopy(ct, 0, ivAndCt, iv.length, ct.length);
            return ENVELOPE_PREFIX + current.keyId() + ":"
                    + Base64.getEncoder().encodeToString(ivAndCt);
        } catch (Exception e) {
            throw new IllegalStateException("信封加密失败（JVM AES-GCM 不可用？）", e);
        }
    }

    /** 解密信封（按 keyId 路由当前/前代钥；AAD 不匹配或篡改上抛）。 */
    public String decrypt(String envelope, String aad) {
        if (envelope == null) {
            return null;
        }
        String[] parts = envelope.split(":", 4);
        if (parts.length != 4 || !ENVELOPE_PREFIX.equals(parts[0] + ":" + parts[1] + ":")) {
            throw new IllegalArgumentException("非信封格式（前缀不符）");
        }
        String keyId = parts[2];
        KeyRing ring = ringFor(keyId);
        byte[] ivAndCt;
        try {
            ivAndCt = Base64.getDecoder().decode(parts[3]);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("信封负载非 Base64（篡改或损坏）", e);
        }
        if (ivAndCt.length <= IV_BYTES) {
            throw new IllegalArgumentException("信封负载过短（篡改或损坏）");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, ring.key(),
                    new GCMParameterSpec(TAG_BITS, ivAndCt, 0, IV_BYTES));
            cipher.updateAAD(aadBytes(aad));
            byte[] plain = cipher.doFinal(ivAndCt, IV_BYTES, ivAndCt.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "信封解密失败（keyId=" + keyId + "——AAD 错位或密文被篡改；完整性优先不上抛不放行）", e);
        }
    }

    /** 是否本格式信封（载体检测/透传判定共用）。 */
    public static boolean isEnvelope(String value) {
        return value != null && value.startsWith(ENVELOPE_PREFIX);
    }

    /** 当前钥 keyId（观测面——轮换收敛可观测）。 */
    public String currentKeyId() {
        return current.keyId();
    }

    private KeyRing ringFor(String keyId) {
        if (current.keyId().equals(keyId)) {
            return current;
        }
        if (previous != null && previous.keyId().equals(keyId)) {
            return previous;
        }
        throw new IllegalArgumentException(
                "信封 keyId=" + keyId + " 不在当前/前代钥环（换钥后未保留 previous-master-key？）");
    }

    private static KeyRing keyRing(String base64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64 == null ? "" : base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("主钥非 Base64（应为基础密钥的 Base64 文本）", e);
        }
        if (bytes.length != 16 && bytes.length != 24 && bytes.length != 32) {
            throw new IllegalArgumentException("主钥长度 " + bytes.length
                    + " 字节非法——AES 钥须 16/24/32 字节（如 openssl rand -base64 32）");
        }
        return new KeyRing(deriveKeyId(bytes), new SecretKeySpec(bytes, "AES"));
    }

    private static String deriveKeyId(byte[] keyBytes) {
        byte[] digest = sha256(keyBytes);
        return java.util.HexFormat.of().formatHex(digest, 0, 8);
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static byte[] aadBytes(String aad) {
        return (aad == null ? "" : aad).getBytes(StandardCharsets.UTF_8);
    }

    /** 观测面：钥环 keyId 集（当前+前代）。 */
    public Map<String, Boolean> keyRingView() {
        Map<String, Boolean> view = new LinkedHashMap<>();
        view.put(current.keyId(), true);
        if (previous != null) {
            view.put(previous.keyId(), false);
        }
        return view;
    }
}
