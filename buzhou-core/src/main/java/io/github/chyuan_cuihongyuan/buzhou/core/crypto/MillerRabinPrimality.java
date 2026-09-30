package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import java.math.BigInteger;

/**
 * Miller–Rabin 素性检测（spec 9030 / W9061 / impl 2383）——
 * Miller 1976/Rabin 1980 思想（JDK BigInteger.isProbablePrime
 * 同源）：**n−1 = d·2^s 分解，见证基 a 检 a^d 与连续平方链——
 * 非平凡平方根存在即合数（强伪素判定）**——Fermat 小定理检测
 * 被 Carmichael 数全骗（561=3·11·17 对任意基费马真）的病解。
 * 12 见证基 {2,3,5,7,11,13,17,19,23,29,31,37}——对全部
 * long 域确定性判定（< 3.3×10²⁴ 界内零误报——确定性而非
 * 概率承诺）；BigInteger 作 128 位乘模载体（算法结构自持）；
 * n≤1 非素、偶数快断；同输入同判定完全确定。
 *
 * <p>与 FastModPow（同包）同域不同面：小模数快速模幂原语 vs
 * long 域素性判定面；与 HammingCode（同包）不同面：纠错信任
 * vs 数论信任。
 */
public final class MillerRabinPrimality {

    private static final long[] WITNESSES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};

    private MillerRabinPrimality() {
    }

    /**
     * 确定性素性判定（long 全域——12 见证基零误报）。
     *
     * @param n 待判整数（n ≤ 1 恒 false）
     */
    public static boolean isPrime(long n) {
        if (n < 2) {
            return false;
        }
        for (long small : WITNESSES) {
            if (n == small) {
                return true;
            }
            if (n % small == 0) {
                return false;
            }
        }
        long d = n - 1;
        int s = 0;
        while ((d & 1) == 0) {
            d >>= 1;
            s++;
        }
        BigInteger bigN = BigInteger.valueOf(n);
        for (long witness : WITNESSES) {
            BigInteger x = BigInteger.valueOf(witness).modPow(BigInteger.valueOf(d), bigN);
            if (x.equals(BigInteger.ONE) || x.equals(bigN.subtract(BigInteger.ONE))) {
                continue;
            }
            boolean composite = true;
            for (int r = 1; r < s; r++) {
                x = x.multiply(x).mod(bigN);
                if (x.equals(bigN.subtract(BigInteger.ONE))) {
                    composite = false;
                    break;
                }
            }
            if (composite) {
                return false;
            }
        }
        return true;
    }
}
