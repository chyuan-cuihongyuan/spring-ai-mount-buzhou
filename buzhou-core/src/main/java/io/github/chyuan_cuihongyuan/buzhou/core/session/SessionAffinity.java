package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 会话黏性路由键（spec 415 / T721，Ketama 确定性键思想）：多实例部署的
 * LB 哈希锚——纯函数无状态，跨实例/跨语言天然一致。键不掺实例名（掺了
 * 就不黏）；sha256 而非 String.hashCode（跨实现稳定）。
 *
 * <p>LB 配方：nginx {@code hash $arg_affinity consistent}；网关按同一
 * {@code sha256("appId|sessionId")} 前 8 字节取模（Ketama ring 本体在
 * LB——不在进程内）。
 */
public final class SessionAffinity {

    private SessionAffinity() {
    }

    /** 亲和键：sha256("appId|sessionId") 前 8 hex（LB 哈希锚）。 */
    public static String key(String appId, String sessionId) {
        byte[] digest = sha256(appId + "|" + sessionId);
        StringBuilder hex = new StringBuilder(8);
        for (int i = 0; i < 4; i++) {
            hex.append(String.format("%02x", digest[i]));
        }
        return hex.toString();
    }

    /** 桶位：sha256 首 8 字节无符号取模（0..buckets-1；buckets ≥ 1）。 */
    public static int bucket(String appId, String sessionId, int buckets) {
        if (buckets < 1) {
            throw new IllegalArgumentException("buckets >= 1（当前 " + buckets + "）");
        }
        byte[] digest = sha256(appId + "|" + sessionId);
        long unsigned = 0;
        for (int i = 0; i < 8; i++) {
            unsigned = (unsigned << 8) | (digest[i] & 0xFF);
        }
        return (int) Long.remainderUnsigned(unsigned, buckets);
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
