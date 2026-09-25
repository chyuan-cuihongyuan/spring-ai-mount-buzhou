package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.Arrays;

/**
 * Elias-Fano 单调序列编码（spec 6013 / T6225 / impl 2213）——
 * Elias-Fano 思想（Lucene/倒排索引同源）：**单调不减 long 序列
 * 的近信息论下界压缩**——按 lowerWidth = max(0, ⌈log₂(U/n)⌉)
 * 拆高低位：低位定宽直存，高位用「值+下标」联合位图（每值一个
 * 置位——间隔即增量），随机访问 O(1)（位图 select + 低位切片）
 * ——全量 long 数组直存（8 字节/元素）与差分链表（随机访问
 * O(n)）两种病的同解。
 *
 * <p>与 DeltaFrameOfReference（同包）同族不同面：分块差分+
 * 参照系 vs 单调序列高低位联合位图；与 EliasGammaCodec 不同
 * 面：逐值前缀码 vs 序列级拆分编码。静态定构（同输入同编码）。
 */
public final class EliasFano {

    private final long[] upperBits;
    private final long[] lowerBits;
    private final int lowerWidth;
    private final int size;
    private final long[] source;

    private EliasFano(long[] sorted) {
        this.source = sorted;
        this.size = sorted.length;
        long universe = sorted[size - 1];
        this.lowerWidth = universe == 0 ? 0 : ceilLog2(ceilDiv(universe, size));
        int upperSlots = (int) (universe >>> lowerWidth) + size + 1;
        this.upperBits = new long[(upperSlots + 63) >>> 6];
        this.lowerBits = new long[lowerWidth == 0 ? 0 : (int) ((size * (long) lowerWidth + 63) >>> 6)];
        for (int i = 0; i < size; i++) {
            long value = sorted[i];
            long upper = value >>> lowerWidth;
            setBit(upperBits, (int) upper + i);
            if (lowerWidth > 0) {
                writeBits(lowerBits, i * (long) lowerWidth, lowerWidth, value);
            }
        }
    }

    private static long ceilDiv(long a, long b) {
        return (a + b - 1) / b;
    }

    private static int ceilLog2(long x) {
        int bitLength = 64 - Long.numberOfLeadingZeros(x);
        return Long.bitCount(x) == 1 ? bitLength - 1 : bitLength;
    }

    /** 编码单调不减序列（null/空/倒序 fail-fast）。 */
    public static EliasFano encode(long[] sorted) {
        if (sorted == null || sorted.length == 0) {
            throw new IllegalArgumentException("序列非空");
        }
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] < sorted[i - 1]) {
                throw new IllegalArgumentException("序列须单调不减: [" + (i - 1) + "]="
                        + sorted[i - 1] + " > [" + i + "]=" + sorted[i]);
            }
        }
        return new EliasFano(sorted.clone());
    }

    /** 随机访问第 i 个值（O(1)：高位 select + 低位切片）。 */
    public long get(int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("下标越界: " + index);
        }
        int upper = selectOne(upperBits, index) - index;
        long value = (long) upper << lowerWidth;
        if (lowerWidth > 0) {
            value |= readBits(lowerBits, index * (long) lowerWidth, lowerWidth);
        }
        return value;
    }

    /** 解码全序列。 */
    public long[] decode() {
        long[] out = new long[size];
        for (int i = 0; i < size; i++) {
            out[i] = get(i);
        }
        return out;
    }

    /** 元素数读数。 */
    public int size() {
        return size;
    }

    /** 低位宽读数（≈⌈log₂(U/n)⌉——密度决定压缩率）。 */
    public int lowerWidth() {
        return lowerWidth;
    }

    /** 高低位总存储位数（对照 64×n 直存）。 */
    public long storedBits() {
        return (upperBits.length + lowerBits.length) * 64L;
    }

    private static void setBit(long[] bits, int pos) {
        bits[pos >>> 6] |= 1L << (pos & 63);
    }

    private static int selectOne(long[] bits, int oneIndex) {
        int seen = 0;
        for (int w = 0; w < bits.length; w++) {
            long word = bits[w];
            int ones = Long.bitCount(word);
            if (seen + ones > oneIndex) {
                for (int b = 0; b < 64; b++) {
                    if ((word >>> b & 1) == 1) {
                        if (seen == oneIndex) {
                            return w * 64 + b;
                        }
                        seen++;
                    }
                }
            }
            seen += ones;
        }
        throw new IllegalArgumentException("select 越界: " + oneIndex);
    }

    private static void writeBits(long[] target, long bitPos, int width, long value) {
        for (int b = 0; b < width; b++) {
            if ((value >>> b & 1) == 1) {
                setBit(target, (int) (bitPos + b));
            }
        }
    }

    private static long readBits(long[] sourceBits, long bitPos, int width) {
        long out = 0;
        for (int b = 0; b < width; b++) {
            long pos = bitPos + b;
            if ((sourceBits[(int) (pos >>> 6)] >>> (pos & 63) & 1) == 1) {
                out |= 1L << b;
            }
        }
        return out;
    }

    /** 防御性只读视图（审计用）。 */
    public long[] sourceCopy() {
        return Arrays.copyOf(source, source.length);
    }
}
