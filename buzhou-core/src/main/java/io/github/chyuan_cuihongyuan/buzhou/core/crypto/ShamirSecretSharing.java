package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Shamir 门限秘密共享（spec 7024 / U7249 / impl 2276）——
 * Shamir 1979 思想：**秘密为 GF(257) 上 t−1 次多项式的
 * 常数项，n 份 (x,y) 分发，任意 t 份拉格朗日插值还原**
 * ——单点保存（丢失/被窃即全损）的病解（门限下信息论
 * 安全：少于 t 份零信息泄露）。素域 257（字节域 0..255
 * 无损嵌入，模运算免 GF(256) 乘法表）；种子化 Random
 * （同种子同份额——确定性可回放）。重复 x/份数越界
 * fail-fast。
 *
 * <p>与 EnvelopeCipher（同包）同族不同面：对称加密（密钥
 * 集中保存）vs 门限分散（无单点信任）。
 */
public final class ShamirSecretSharing {

    /** 素域模数（257 素数——字节域无损嵌入）。 */
    public static final int FIELD = 257;

    /** 份额（x∈[1,255]，y∈[0,256]）。 */
    public static final class Share {
        final int x;
        final int y;

        Share(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int x() {
            return x;
        }

        public int y() {
            return y;
        }
    }

    private ShamirSecretSharing() {
    }

    /** 拆分（secret∈[0,255]；threshold≥2；shareCount≥threshold 且 ≤255；越域 fail-fast）。 */
    public static List<Share> split(int secret, int threshold, int shareCount, Random rng) {
        if (secret < 0 || secret > 255) {
            throw new IllegalArgumentException("秘密须为字节域 [0,255]: " + secret);
        }
        if (threshold < 2) {
            throw new IllegalArgumentException("门限须 ≥2: " + threshold);
        }
        if (shareCount < threshold || shareCount > 255) {
            throw new IllegalArgumentException("份数须在 [门限,255]: " + shareCount);
        }
        int[] coefficients = new int[threshold - 1];
        for (int i = 0; i < coefficients.length; i++) {
            coefficients[i] = rng.nextInt(FIELD);
        }
        List<Share> shares = new ArrayList<>();
        for (int x = 1; x <= shareCount; x++) {
            int y = secret;
            long power = 1;
            for (int coefficient : coefficients) {
                power = (power * x) % FIELD;
                y = (int) ((y + coefficient * power) % FIELD);
            }
            shares.add(new Share(x, y));
        }
        return shares;
    }

    /** 合成（≥门限份；重复 x/不足门限 fail-fast；返回字节域秘密）。 */
    public static int combine(List<Share> shares, int threshold) {
        if (shares == null || shares.size() < threshold) {
            throw new IllegalArgumentException("份额不足门限: "
                    + (shares == null ? 0 : shares.size()) + "/" + threshold);
        }
        Set<Integer> seen = new HashSet<>();
        for (Share share : shares) {
            if (!seen.add(share.x)) {
                throw new IllegalArgumentException("重复 x 份额: " + share.x);
            }
        }
        long secret = 0;
        for (int i = 0; i < threshold; i++) {
            Share share = shares.get(i);
            long numerator = 1;
            long denominator = 1;
            for (int j = 0; j < threshold; j++) {
                if (j == i) {
                    continue;
                }
                Share other = shares.get(j);
                numerator = (numerator * (0 - other.x + FIELD)) % FIELD;
                denominator = (denominator * (share.x - other.x + FIELD)) % FIELD;
            }
            long lagrange = numerator * modInverse((int) denominator) % FIELD;
            secret = (secret + share.y * lagrange) % FIELD;
        }
        int value = (int) ((secret + FIELD) % FIELD);
        if (value > 255) {
            throw new IllegalStateException("合成结果越字节域（份额不一致）: " + value);
        }
        return value;
    }

    /** 模逆（费马小定理——257 素数）。 */
    private static int modInverse(int value) {
        int inverse = 1;
        int exponent = FIELD - 2;
        long base = value % FIELD;
        while (exponent > 0) {
            if ((exponent & 1) == 1) {
                inverse = (int) (inverse * base % FIELD);
            }
            base = base * base % FIELD;
            exponent >>= 1;
        }
        return inverse;
    }
}
