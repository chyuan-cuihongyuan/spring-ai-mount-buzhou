package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.Arrays;

/**
 * Simple8b 位打包（spec 6015 / T6229 / impl 2215）——
 * Simple8b 思想（InfluxDB/时序列存同源）：**60 位载荷按
 * 16 档选择子变长打包**——4 位选择子定（值数×位宽）档位，
 * 多个小整数共居一个 64 位字（全零档单字容纳 240 值）——
 * 小基数整数列逐值 8 字节直存（存储放大数十倍）的病解。
 * 贪心选档（首个能容纳的档位——同输入同字流，确定性）。
 *
 * <p>与 VarintCodec（同包）同族不同面：流式逐字节变长 vs
 * 字对齐并行定宽；与 BitPacking 互补：混合宽度自适应 vs
 * 单一固定宽。值域 [0, 2^60)。
 */
public final class Simple8b {

    private static final int[] VALUES_PER_WORD = {240, 120, 60, 30, 20, 15, 12, 10, 8, 7, 6, 5, 4, 3, 2, 1};
    private static final int[] BITS_PER_VALUE = {0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 15, 20, 30, 60};
    private static final long VALUE_LIMIT = 1L << 60;

    private final long[] words;
    private final int valueCount;

    private Simple8b(long[] words, int valueCount) {
        this.words = words;
        this.valueCount = valueCount;
    }

    /** 编码（null/负值/值≥2^60 fail-fast；空数组允许）。 */
    public static Simple8b encode(long[] values) {
        if (values == null) {
            throw new IllegalArgumentException("序列非空");
        }
        for (long v : values) {
            if (v < 0) {
                throw new IllegalArgumentException("值须非负: " + v);
            }
            if (v >= VALUE_LIMIT) {
                throw new IllegalArgumentException("值超出 2^60 域: " + v);
            }
        }
        long[] words = new long[values.length];
        int wordCount = 0;
        int pos = 0;
        while (pos < values.length) {
            int remaining = values.length - pos;
            int selector = -1;
            int take = 0;
            for (int s = 0; s < 16; s++) {
                int candidate = Math.min(VALUES_PER_WORD[s], remaining);
                if (candidate == 0) {
                    continue;
                }
                int bits = BITS_PER_VALUE[s];
                boolean fits = true;
                for (int j = 0; j < candidate; j++) {
                    long v = values[pos + j];
                    if (s == 0 ? v != 0 : (v >>> bits) != 0) {
                        fits = false;
                        break;
                    }
                }
                if (fits) {
                    selector = s;
                    take = candidate;
                    break;
                }
            }
            long word = (long) selector << 60;
            for (int j = 0; j < take; j++) {
                word |= values[pos + j] << (j * BITS_PER_VALUE[selector]);
            }
            words[wordCount++] = word;
            pos += take;
        }
        return new Simple8b(Arrays.copyOf(words, wordCount), values.length);
    }

    /** 解码全序列（尾部截断到原值数）。 */
    public long[] decode() {
        long[] out = new long[valueCount];
        int idx = 0;
        for (long word : words) {
            int selector = (int) (word >>> 60);
            int bits = BITS_PER_VALUE[selector];
            for (int j = 0; j < VALUES_PER_WORD[selector] && idx < valueCount; j++) {
                out[idx++] = bits == 0 ? 0 : (word >>> (j * bits)) & ((1L << bits) - 1);
            }
        }
        return out;
    }

    /** 单字解码（静态工具——字→VALUES_PER_WORD[档] 个值）。 */
    public static long[] decodeWord(long word) {
        int selector = (int) (word >>> 60);
        if (selector < 0 || selector > 15) {
            throw new IllegalArgumentException("选择子非法: " + selector);
        }
        int bits = BITS_PER_VALUE[selector];
        long[] out = new long[VALUES_PER_WORD[selector]];
        for (int j = 0; j < out.length; j++) {
            out[j] = bits == 0 ? 0 : (word >>> (j * bits)) & ((1L << bits) - 1);
        }
        return out;
    }

    /** 字数读数。 */
    public int wordCount() {
        return words.length;
    }

    /** 原值数读数。 */
    public int valueCount() {
        return valueCount;
    }

    /** 仅限审计：字流防御副本。 */
    public long[] wordsCopy() {
        return words.clone();
    }
}
