package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

/**
 * 快速模幂（spec 7037 / U7275 / impl 2289）——平方-乘
 * 二进制快速幂思想（RSA/Diffie-Hellman 原语面）：**指数
 * 二进制位扫描，平方累积**——O(log exponent)——朴素连乘
 * O(exponent)（指数放大不可承受）的病解。long 域（模数
 * ≤约 3·10⁹ 防中间乘积溢出——明示安全域 fail-fast）。
 * 完全确定纯函数。同参数同结果。
 *
 * <p>与 ShamirSecretSharing（同包）同族不同面：门限分散
 * vs 域算术原语（modInverse 同用费马小定理）。
 */
public final class FastModPow {

    /** 模乘法安全上限（中间乘积 < 2⁶³）。 */
    public static final long MOD_LIMIT = 3_037_000_499L;

    private FastModPow() {
    }

    /** base^exponent mod mod（指数≥0；mod≥1；越安全域 fail-fast）。 */
    public static long modPow(long base, long exponent, long mod) {
        if (mod < 1) {
            throw new IllegalArgumentException("模数须 ≥1: " + mod);
        }
        if (exponent < 0) {
            throw new IllegalArgumentException("指数须非负: " + exponent);
        }
        if (mod > MOD_LIMIT) {
            throw new IllegalArgumentException("模数越安全域 ≤" + MOD_LIMIT + ": " + mod);
        }
        long result = 1;
        long baseMod = base % mod;
        long exp = exponent;
        while (exp > 0) {
            if ((exp & 1) == 1) {
                result = result * baseMod % mod;
            }
            baseMod = baseMod * baseMod % mod;
            exp >>= 1;
        }
        return result;
    }
}
