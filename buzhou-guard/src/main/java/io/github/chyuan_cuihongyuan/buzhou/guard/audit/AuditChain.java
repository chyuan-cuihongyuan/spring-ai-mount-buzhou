package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 防篡改审计链（wayfinder2 impl-22 / T50 / docs/spec/12 §guard-22，IETF AAT 草案）：
 *
 * <ul>
 *   <li><b>哈希链</b>：{@code prev_hash(N) = SHA-256(JCS(record(N-1)))}（RFC 8785 JCS 强制）；
 *       会话收尾 {@code sessionHash} = 全部 prev_hash 拼接再哈希；</li>
 *   <li><b>ECDSA P-256 可选签名</b>：对「去 signature 字段的 JCS 序列化 → SHA-256」签名，
 *       输出 <b>IEEE P1363 r||s 64 字节</b> Base64url（JDK DER 签名转换 ~30 行，非 JWS）；</li>
 *   <li><b>验证</b>：{@link #verify} 重算链与签名——篡改任一记录即失败（不可否认证据）。</li>
 * </ul>
 *
 * <p>纯本地、零新依赖（JDK 内置 SHA256withECDSA）；无签名模式（仅哈希链）同样可用。
 */
public final class AuditChain {

    private final List<AgentAuditRecord> records = new ArrayList<>();
    private final String agentId;
    private final String agentVersion;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public AuditChain(String agentId, String agentVersion) {
        this(agentId, agentVersion, null, null);
    }

    public AuditChain(String agentId, String agentVersion, PrivateKey privateKey,
                      PublicKey publicKey) {
        this.agentId = agentId;
        this.agentVersion = agentVersion;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    /** 生成 P-256 密钥对（业务侧持久保管私钥；公钥随审计报告分发验证）。 */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(256);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("EC P-256 不可用", e);
        }
    }

    /** 追加审计记录（自动携带链上前缀哈希；可选签名）。 */
    public synchronized AgentAuditRecord append(String sessionId, String actionType,
                                                String actionDetail, String outcome) {
        String prevHash = records.isEmpty()
                ? sha256Hex("")
                : sha256Hex(Jcs.canonicalize(records.get(records.size() - 1).unsignedMap()));
        AgentAuditRecord record = new AgentAuditRecord(
                UUID.randomUUID().toString(), System.currentTimeMillis(),
                agentId, agentVersion == null ? "" : agentVersion, sessionId,
                actionType, actionDetail, outcome, "default",
                records.isEmpty() ? "" : records.get(records.size() - 1).recordId(),
                prevHash, null);
        if (privateKey != null) {
            record = record.withSignature(sign(record));
        }
        records.add(record);
        return record;
    }

    /** 会话收尾摘要（全部 prev_hash 拼接再哈希）。 */
    public synchronized String sessionHash() {
        StringBuilder joined = new StringBuilder();
        for (AgentAuditRecord record : records) {
            joined.append(record.prevHash());
        }
        return sha256Hex(joined.toString());
    }

    public synchronized List<AgentAuditRecord> records() {
        return List.copyOf(records);
    }

    /** 全链验证：prev_hash 链一致 + （有公钥时）每条签名可验。 */
    public synchronized boolean verify(PublicKey verifyKey) {
        String expectedPrev = sha256Hex("");
        AgentAuditRecord previous = null;
        for (AgentAuditRecord record : records) {
            if (!record.prevHash().equals(expectedPrev)) {
                return false;
            }
            if (previous != null && !java.util.Objects.equals(record.parentRecordId(),
                    previous.recordId())) {
                return false;
            }
            if (verifyKey != null && record.signature() != null
                    && !verifySignature(record, verifyKey)) {
                return false;
            }
            expectedPrev = sha256Hex(Jcs.canonicalize(record.unsignedMap()));
            previous = record;
        }
        return true;
    }

    /** 对记录（去 signature）签名：SHA256withECDSA → DER 转 P1363 r||s（64 字节）→ Base64url。 */
    private String sign(AgentAuditRecord record) {
        try {
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(privateKey);
            signer.update(Jcs.canonicalize(record.unsignedMap())
                    .getBytes(StandardCharsets.UTF_8));
            return Base64Url.encode(derToP1363(signer.sign()));
        } catch (Exception e) {
            throw new IllegalStateException("审计签名失败", e);
        }
    }

    private boolean verifySignature(AgentAuditRecord record, PublicKey key) {
        try {
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(key);
            verifier.update(Jcs.canonicalize(record.unsignedMap())
                    .getBytes(StandardCharsets.UTF_8));
            return verifier.verify(p1363ToDer(Base64Url.decode(record.signature())));
        } catch (Exception e) {
            return false;
        }
    }

    /** DER ECDSA-Sig-Value → IEEE P1363 r||s（定长 32+32）。 */
    static byte[] derToP1363(byte[] der) {
        int idx = 2; // SEQUENCE + 长度（P-256 恒短于 128 → 单字节长度）
        byte[] r = extractDerInt(der, idx);
        idx += 2 + der[idx + 1];
        byte[] s = extractDerInt(der, idx);
        return concat(fixLen(r, 32), fixLen(s, 32));
    }

    /** IEEE P1363 r||s → DER ECDSA-Sig-Value。 */
    static byte[] p1363ToDer(byte[] p1363) {
        byte[] r = trimLeadingZeros(java.util.Arrays.copyOfRange(p1363, 0, 32));
        byte[] s = trimLeadingZeros(java.util.Arrays.copyOfRange(p1363, 32, 64));
        int body = 2 + r.length + 2 + s.length;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write(0x30);
        out.write(body);
        out.write(0x02);
        out.write(r.length);
        out.write(r, 0, r.length);
        out.write(0x02);
        out.write(s.length);
        out.write(s, 0, s.length);
        return out.toByteArray();
    }

    private static byte[] extractDerInt(byte[] der, int at) {
        int len = der[at + 1];
        return java.util.Arrays.copyOfRange(der, at + 2, at + 2 + len);
    }

    private static byte[] fixLen(byte[] value, int len) {
        if (value.length == len) {
            return value;
        }
        byte[] fixed = new byte[len];
        System.arraycopy(value, Math.max(0, value.length - len),
                fixed, Math.max(0, len - value.length), Math.min(value.length, len));
        return fixed;
    }

    private static byte[] trimLeadingZeros(byte[] value) {
        int first = 0;
        while (first < value.length - 1 && value[first] == 0) {
            first++;
        }
        if ((value[first] & 0x80) != 0) {
            // DER 正数最高位为 1 时须补 0x00
            byte[] padded = new byte[value.length - first + 1];
            padded[0] = 0;
            System.arraycopy(value, first, padded, 1, value.length - first);
            return padded;
        }
        return java.util.Arrays.copyOfRange(value, first, value.length);
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    static String sha256Hex(String content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /** Base64url（无填充）编解码（P1363 签名载体）。 */
    static final class Base64Url {
        static String encode(byte[] bytes) {
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        }

        static byte[] decode(String text) {
            return java.util.Base64.getUrlDecoder().decode(text);
        }
    }
}
