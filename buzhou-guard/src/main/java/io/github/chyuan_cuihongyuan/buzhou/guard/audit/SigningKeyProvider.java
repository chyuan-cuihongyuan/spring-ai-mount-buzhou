package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

/**
 * 审计签名密钥加载 SPI（impl-39 / spec 13 §T64）：密钥轮换是运维动作（换文件/换 KMS 指向），
 * 不是代码变更。实现负责把外部密钥材料解析为版本化密钥集（最高版本 = active 签名钥）。
 */
public interface SigningKeyProvider {

    /**
     * 加载全部版本化密钥（可验历史 + 当前签名钥）。返回空集 = 无签名（调用方降级纯哈希链）。
     */
    List<VersionedSigningKey> load();

    /**
     * 版本化密钥对。{@code publicKey} 可为 null（PKCS#8 私钥材料通常可导出公钥）；
     * 非 active 版本的私钥由 {@link SigningKeyRing} 即刻丢弃（只验不签）。
     */
    record VersionedSigningKey(int version, PrivateKey privateKey, PublicKey publicKey) {

        public VersionedSigningKey {
            if (version <= 0) {
                throw new IllegalArgumentException("keyVersion 必须为正整数（收到 " + version + "）");
            }
        }
    }
}
