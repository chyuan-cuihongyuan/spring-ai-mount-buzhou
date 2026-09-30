package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import java.math.BigInteger;

/**
 * Karatsuba 大数乘法（spec 9031 / W9063 / impl 2384）——Karatsuba
 * 1960 思想（Kolmogorov 猜想 O(n²) 下界被 23 岁学生一学期推翻
 * 的经典——GMP/OpenSSL 大数运算同源）：**对半分裂 x=x_H·B+x_L，
 * 三次子乘（x_H·y_H、x_L·y_L、(x_H+x_L)(y_H+y_L)−前两者）替代
 * 四次**——T(n)=3T(n/2)+O(n) 得 O(n^1.585)——教科书竖式逐位
 * 乘 O(n²) 的病解。BigInteger 作数载体（自持递归结构）；阈值
 * 1024 bit 以下落 BigInteger 原生（其内部同款 Karatsuba）——
 * 结构教学面与工程底座互证；符号经 abs 归一重挂；null
 * fail-fast；同输入同积完全确定。
 *
 * <p>与 FastModPow（同包）同域不同面：模幂原语 vs 大数乘法
 * 结构；与 ChineseRemainder（同包）互补：乘法结构 vs 模系
 * 合并。
 */
public final class KaratsubaMultiplication {

    private static final int DIRECT_BITS = 1024;

    private KaratsubaMultiplication() {
    }

    /**
     * 大数乘（任意符号；结果与 BigInteger 乘法恒等）。
     *
     * @throws IllegalArgumentException null 输入
     */
    public static BigInteger multiply(BigInteger a, BigInteger b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        if (a.signum() == 0 || b.signum() == 0) {
            return BigInteger.ZERO;
        }
        boolean negative = a.signum() * b.signum() < 0;
        BigInteger product = karatsuba(a.abs(), b.abs());
        return negative ? product.negate() : product;
    }

    private static BigInteger karatsuba(BigInteger x, BigInteger y) {
        if (x.bitLength() <= DIRECT_BITS || y.bitLength() <= DIRECT_BITS) {
            return x.multiply(y);
        }
        int half = Math.min(x.bitLength(), y.bitLength()) / 2;
        BigInteger xHigh = x.shiftRight(half);
        BigInteger xLow = x.subtract(xHigh.shiftLeft(half));
        BigInteger yHigh = y.shiftRight(half);
        BigInteger yLow = y.subtract(yHigh.shiftLeft(half));
        BigInteger high = karatsuba(xHigh, yHigh);
        BigInteger low = karatsuba(xLow, yLow);
        BigInteger middle = karatsuba(xHigh.add(xLow), yHigh.add(yLow))
                .subtract(high).subtract(low);
        return high.shiftLeft(half * 2).add(middle.shiftLeft(half)).add(low);
    }
}
