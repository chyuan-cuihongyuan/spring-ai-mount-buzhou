package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

/**
 * 中国剩余定理（spec 8041 / V8081 / impl 2342）——
 * 孙子算经「物不知数」/CRT 思想：**两两互质模数下，扩展
 * 欧几里得逆元加权合并余数**——x=Σ rᵢ·Mᵢ·inv(Mᵢ,mᵢ) mod M
 * O(k·log M)——逐枚举模积域 M 爆炸（大模不可承受）的病解。
 * 两两互质校验（gcd≠1 携下标 fail-fast）；数组不齐/null
 * fail-fast；确定性纯函数。
 *
 * <p>与 FastModPow（spec 7037）同族不同面：单模幂 vs 多模
 * 方程组合并。
 */
public final class ChineseRemainder {

    private ChineseRemainder() {
    }

    /** 最小非负解 x ≡ remainders[i] (mod moduli[i])（互质校验内含）。 */
    public static long crt(long[] remainders, long[] moduli) {
        if (remainders == null || moduli == null || remainders.length == 0
                || remainders.length != moduli.length) {
            throw new IllegalArgumentException("双表同长非空");
        }
        long modulusProduct = 1;
        for (int i = 0; i < moduli.length; i++) {
            if (moduli[i] < 1) {
                throw new IllegalArgumentException("模数为正（下标 " + i + " 实际 " + moduli[i] + "）");
            }
            for (int j = 0; j < i; j++) {
                long gcd = gcd(moduli[i], moduli[j]);
                if (gcd != 1) {
                    throw new IllegalArgumentException("模数两两互质（" + j + "/" + i
                            + " gcd=" + gcd + "）");
                }
            }
            modulusProduct *= moduli[i];
        }
        long solution = 0;
        for (int i = 0; i < moduli.length; i++) {
            long partial = modulusProduct / moduli[i];
            long inverse = modularInverse(partial % moduli[i], moduli[i]);
            long term = Math.multiplyExact(remainders[i] % moduli[i], partial % modulusProduct)
                    % modulusProduct;
            long contribution = Math.multiplyExact(term, inverse) % modulusProduct;
            solution = (solution + contribution) % modulusProduct;
        }
        return (solution % modulusProduct + modulusProduct) % modulusProduct;
    }

    /** 扩展欧几里得模逆（gcd≠1 抛——调用方已校验，防御面）。 */
    private static long modularInverse(long value, long modulus) {
        long oldR = value % modulus;
        long r = modulus;
        long oldS = 1;
        long s = 0;
        while (r != 0) {
            long quotient = oldR / r;
            long tmpR = oldR - quotient * r;
            oldR = r;
            r = tmpR;
            long tmpS = oldS - quotient * s;
            oldS = s;
            s = tmpS;
        }
        if (oldR != 1) {
            throw new IllegalArgumentException("模逆不存在（gcd=" + oldR + "）");
        }
        return ((oldS % modulus) + modulus) % modulus;
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long tmp = a % b;
            a = b;
            b = tmp;
        }
        return a;
    }
}
