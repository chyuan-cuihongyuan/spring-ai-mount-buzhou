package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import java.math.BigInteger;
import java.util.Random;

/**
 * Diffie–Hellman 密钥交换（spec 9037 / W9075 / impl 2390）——
 * Diffie–Hellman 1976 思想（「密码学的新方向」开山——TLS/
 * SSH/Signal 密钥协商同源）：**素域上 g^a 与 g^b 公开互传，
 * (g^b)^a = (g^a)^b = g^(ab) mod p 双方同达、离散对数难解保
 * 秘**——明文信道协商共享秘密——对称密钥预共享（需安全信道
 * 分发）与信使传递（物理成本）的病解。RFC 3526 Group 14
 * 2048 位安全素数常驻（业界标准域参数）；私钥域 [2,p)（均匀
 * 长）；publicKey/sharedSecret 两原语面；p 素性校验复用
 * MillerRabinPrimality（spec 9030——族内组合）；弱参数
 * （p 非素/g 越域/私钥 ≤1）fail-fast。
 *
 * <p>与 ShamirSecretSharing（同包）同域不同面：秘密分享 vs
 * 秘密协商；与 FastModPow（同包）同根：小模数原语 vs 大整数
 * 域协商面（BigInteger modPow 载体）。
 */
public final class DiffieHellmanExchange {

    /**
     * 128 位教学安全素数（p=2q+1，q 亦素——探针生成经 isProbablePrime(100) 验证）。
     * <b>非安全域参数</b>：仅测试/教学面；生产协商须自携 RFC 3526 等标准域大素数
     * （入参面支持任意位宽——素性校验 long 域走 MillerRabin、大域走 isProbablePrime）。
     */
    public static final BigInteger TEACHING_SAFE_PRIME_128 = new BigInteger(
            "da5eb6c5cb31de9f82eb42774fb58cd7", 16);

    /** 生成元（2——对 p=2q+1 为 q 阶子群生成元，交换正确性对任意 g 成立）。 */
    public static final BigInteger DEFAULT_G = BigInteger.TWO;

    private DiffieHellmanExchange() {
    }

    /**
     * 随机私钥（[2,p) 均匀长—— BigInteger(random bits) 拒零一后取域内）。
     *
     * @throws IllegalArgumentException p 非素或 p ≤ 3
     */
    public static BigInteger generatePrivateKey(BigInteger p, Random random) {
        validatePrime(p);
        BigInteger key;
        do {
            key = new BigInteger(p.bitLength(), random);
        } while (key.compareTo(BigInteger.TWO) < 0 || key.compareTo(p) >= 0);
        return key;
    }

    /**
     * 公钥 g^private mod p。
     *
     * @throws IllegalArgumentException p 非素、g ∉ [2,p)、私钥 ∉ [2,p)
     */
    public static BigInteger publicKey(BigInteger p, BigInteger g, BigInteger privateKey) {
        validatePrime(p);
        validateGenerator(g, p);
        if (privateKey.compareTo(BigInteger.TWO) < 0 || privateKey.compareTo(p) >= 0) {
            throw new IllegalArgumentException("私钥 ∈ [2,p)（实际 " + privateKey + "）");
        }
        return g.modPow(privateKey, p);
    }

    /**
     * 共享秘密（对方公钥^己方私钥 mod p——两侧同达 g^(ab)）。
     *
     * @throws IllegalArgumentException p 非素、公钥 ∉ [1,p)、私钥 ∉ [2,p)
     */
    public static BigInteger sharedSecret(BigInteger p, BigInteger peerPublicKey, BigInteger privateKey) {
        validatePrime(p);
        if (peerPublicKey.signum() < 1 || peerPublicKey.compareTo(p) >= 0) {
            throw new IllegalArgumentException("对方公钥 ∈ [1,p)（实际 " + peerPublicKey + "）");
        }
        if (privateKey.compareTo(BigInteger.TWO) < 0 || privateKey.compareTo(p) >= 0) {
            throw new IllegalArgumentException("私钥 ∈ [2,p)（实际 " + privateKey + "）");
        }
        return peerPublicKey.modPow(privateKey, p);
    }

    private static void validatePrime(BigInteger p) {
        if (p == null || p.compareTo(BigInteger.valueOf(3)) <= 0) {
            throw new IllegalArgumentException("素数 p > 3");
        }
        boolean prime = p.bitLength() <= 62
                ? MillerRabinPrimality.isPrime(p.longValue())
                : p.isProbablePrime(64);
        if (!prime) {
            throw new IllegalArgumentException("p 须为素数（素性校验判合）");
        }
    }

    private static void validateGenerator(BigInteger g, BigInteger p) {
        if (g == null || g.compareTo(BigInteger.TWO) < 0 || g.compareTo(p) >= 0) {
            throw new IllegalArgumentException("生成元 g ∈ [2,p)（实际 " + g + "）");
        }
    }
}
