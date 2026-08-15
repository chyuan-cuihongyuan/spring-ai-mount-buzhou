package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 版本化审计签名密钥环（impl-39 / spec 13 §T64，源 Vault Transit 版本化密钥思想）：
 *
 * <ul>
 *   <li><b>keyVersion 嵌记录</b>：签名记录携带所用密钥版本，验证按版本取公钥；</li>
 *   <li><b>rotate() 原子切换</b>：active 引用 volatile 单写——切换后新记录即刻用新钥，
 *       并发 append 不会出现「半新半旧」；</li>
 *   <li><b>旧钥只验不签</b>：轮换后旧版私钥即从内存丢弃（仅保留公钥供历史验证），
 *       物理上不可能再用旧钥产生新签名；</li>
 *   <li><b>minVerifyVersion</b>：低于该版本的签名直接判不可验（收窄历史窗口的运维开关）；</li>
 *   <li><b>无密钥降级</b>：{@link #hasSigningKey()} 为 false 时调用方应降级纯哈希链 + WARN。</li>
 * </ul>
 */
public final class SigningKeyRing {

    private static final int UNVERSIONED = 0;

    private record ActiveKey(int version, PrivateKey privateKey, PublicKey publicKey) {
    }

    private volatile ActiveKey active;
    private final Map<Integer, PublicKey> verifyKeys = new ConcurrentHashMap<>();
    private final int minVerifyVersion;
    private final SigningKeyPersister persister;

    /** 空环（纯哈希链降级模式；minVerifyVersion = 0）。 */
    public SigningKeyRing() {
        this(0, List.of(), null);
    }

    /**
     * 从版本化密钥集构建：最高版本为 active 签名钥，其余版本只验不签
     * （私钥即刻丢弃，仅留公钥）。
     */
    public SigningKeyRing(int minVerifyVersion, List<SigningKeyProvider.VersionedSigningKey> keys) {
        this(minVerifyVersion, keys, null);
    }

    /**
     * spec 41 §A / T153 / impl-124：带轮换持久化钩子构造——rotate() 在切换 active 之前先经
     * persister 落盘新钥（写而后切）；持久化失败轮换整体失败、active 不变。persister 可空。
     */
    public SigningKeyRing(int minVerifyVersion, List<SigningKeyProvider.VersionedSigningKey> keys,
            SigningKeyPersister persister) {
        this.persister = persister;
        this.minVerifyVersion = minVerifyVersion;
        List<SigningKeyProvider.VersionedSigningKey> sorted = new ArrayList<>(keys);
        sorted.sort(Comparator.comparingInt(SigningKeyProvider.VersionedSigningKey::version).reversed());
        for (SigningKeyProvider.VersionedSigningKey key : sorted) {
            if (key.version() <= 0) {
                throw new IllegalArgumentException("keyVersion 必须为正整数（收到 " + key.version() + "）");
            }
            PublicKey previous = verifyKeys.put(key.version(), key.publicKey());
            if (previous != null) {
                throw new IllegalArgumentException("keyVersion 重复：" + key.version());
            }
        }
        if (!sorted.isEmpty()) {
            SigningKeyProvider.VersionedSigningKey latest = sorted.getFirst();
            // 非最新版本的私钥不进内存：只验不签的物理保证
            this.active = new ActiveKey(latest.version(), latest.privateKey(), latest.publicKey());
            verifyKeys.entrySet().removeIf(e -> e.getKey() < minVerifyVersion);
        }
    }

    /**
     * 原子轮换：新钥即刻生效为唯一签名钥，旧钥私钥丢弃、公钥保留可验。
     * spec 41 §A / T153：配置了 persister 时「写而后切」——落盘失败则轮换失败、active 不变。
     */
    public synchronized void rotate(int version, KeyPair newKey) {
        if (version <= 0) {
            throw new IllegalArgumentException("keyVersion 必须为正整数（收到 " + version + "）");
        }
        if (verifyKeys.containsKey(version)) {
            throw new IllegalArgumentException("keyVersion 重复：" + version);
        }
        ActiveKey previous = this.active;
        if (previous != null && version <= previous.version()) {
            throw new IllegalArgumentException(
                    "轮换版本必须递增（active=" + previous.version() + "，收到 " + version + "）");
        }
        if (persister != null) {
            persister.persist(version, newKey); // 先落盘，后切换——失败即中止
        }
        this.active = new ActiveKey(version, newKey.getPrivate(), newKey.getPublic());
        verifyKeys.put(version, newKey.getPublic());
    }

    /** 是否有可用签名钥（false = 纯哈希链降级模式）。 */
    public boolean hasSigningKey() {
        return active != null;
    }

    /** 当前签名版本（无签名钥时 0）。 */
    public int activeVersion() {
        ActiveKey current = active;
        return current == null ? UNVERSIONED : current.version();
    }

    PrivateKey activePrivateKey() {
        ActiveKey current = active;
        return current == null ? null : current.privateKey();
    }

    /**
     * 按版本取验证公钥；版本未知或低于 {@code minVerifyVersion} 返回 null（= 拒绝验证）。
     * 无签名记录（keyVersion=0）不走本入口。
     */
    public PublicKey verifyKey(int version) {
        if (version <= 0 || version < minVerifyVersion) {
            return null;
        }
        return verifyKeys.get(version);
    }

    public int minVerifyVersion() {
        return minVerifyVersion;
    }

    /** 已注册的可验版本（运维观测用，升序）。 */
    public List<Integer> registeredVersions() {
        return verifyKeys.keySet().stream().sorted().toList();
    }
}
