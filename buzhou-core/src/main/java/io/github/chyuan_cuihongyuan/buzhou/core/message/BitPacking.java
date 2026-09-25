package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.Arrays;

/**
 * Bit Packing 固定位宽打包（spec 6016 / T6231 / impl 2216）——
 * Parquet/ORC 列存 bit-packing 思想：**统一位宽无缝串接**——
 * 每值占定宽 w 位、首尾相接跨字排布（64 位字边界不浪费），
 * 压缩率 = w/64 精确可控——全量 long 直存（值域窄时放大）
 * 的病解。跨字拼接取值 O(1)（双字移位合并）；位级定构
 * （同输入同打包流——可审计）。
 *
 * <p>与 Simple8b（spec 6015）互补：单一固定宽 vs 混合自适应
 * 档位；与 VarintCodec 不同面：字节流变长 vs 位域定宽。
 * 值域 [0, 2^w)。
 */
public final class BitPacking {

    private final long[] packed;
    private final int bitWidth;
    private final int count;

    private BitPacking(long[] packed, int bitWidth, int count) {
        this.packed = packed;
        this.bitWidth = bitWidth;
        this.count = count;
    }

    /** 打包（width∈[0,64]；负值/溢出 fail-fast）。 */
    public static BitPacking pack(long[] values, int bitWidth) {
        if (values == null) {
            throw new IllegalArgumentException("序列非空");
        }
        if (bitWidth < 0 || bitWidth > 64) {
            throw new IllegalArgumentException("位宽须在 [0,64]: " + bitWidth);
        }
        if (bitWidth < 64) {
            for (long v : values) {
                if (v < 0) {
                    throw new IllegalArgumentException("值须非负: " + v);
                }
            }
            if (bitWidth < 63) {
                long limit = 1L << bitWidth;
                for (long v : values) {
                    if (v >= limit) {
                        throw new IllegalArgumentException("值超出 " + bitWidth + " 位域: " + v);
                    }
                }
            }
        } else {
            for (long v : values) {
                if (v < 0) {
                    throw new IllegalArgumentException("64 位域仅容非负值: " + v);
                }
            }
        }
        long totalBits = values.length * (long) bitWidth;
        long[] packed = new long[(int) ((totalBits + 63) >>> 6)];
        for (int i = 0; i < values.length && packed.length > 0; i++) {
            long bitPos = i * (long) bitWidth;
            int word = (int) (bitPos >>> 6);
            int offset = (int) (bitPos & 63);
            packed[word] |= (values[i] << offset);
            if (offset + bitWidth > 64) {
                packed[word + 1] = values[i] >>> (64 - offset) | packed[word + 1];
            }
        }
        return new BitPacking(packed, bitWidth, values.length);
    }

    /** 取第 i 个值（跨字双字移位合并，O(1)）。 */
    public long get(int index) {
        if (index < 0 || index >= count) {
            throw new IllegalArgumentException("下标越界: " + index);
        }
        if (bitWidth == 0) {
            return 0;
        }
        long bitPos = index * (long) bitWidth;
        int word = (int) (bitPos >>> 6);
        int offset = (int) (bitPos & 63);
        long low = packed[word] >>> offset;
        if (bitWidth == 64) {
            long high = offset == 0 ? 0 : packed[word + 1] << (64 - offset);
            return low | high;
        }
        long combined = low;
        if (offset + bitWidth > 64) {
            combined |= packed[word + 1] << (64 - offset);
        }
        return combined & ((1L << bitWidth) - 1);
    }

    /** 解包全序列。 */
    public long[] unpack() {
        long[] out = new long[count];
        for (int i = 0; i < count; i++) {
            out[i] = get(i);
        }
        return out;
    }

    /** 位宽读数。 */
    public int bitWidth() {
        return bitWidth;
    }

    /** 值数读数。 */
    public int count() {
        return count;
    }

    /** 打包字数读数（⌈n·w/64⌉）。 */
    public int wordCount() {
        return packed.length;
    }

    /** 仅限审计：打包流防御副本。 */
    public long[] packedCopy() {
        return Arrays.copyOf(packed, packed.length);
    }
}
