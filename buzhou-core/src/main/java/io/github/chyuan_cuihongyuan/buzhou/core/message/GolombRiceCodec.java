package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.ArrayList;
import java.util.List;

/**
 * Golomb/Rice 编码（spec 7020 / U7241 / impl 2272）——
 * Golomb 1966 / Rice 1971 变长码思想（FLIF/WebP lossless
 * 同源）：**商 unary + 余数定宽 k 位**——几何分布数据
 * （小值高频）逼近熵下界，参数 k 自适应面（2^k 为除数）
 * ——定宽编码对偏斜值域放大、Huffman 码表开销（短流
 * 不划算）两种病的折中解。MSB-first 位流；负值 fail-fast
 * （非负域——明示不做 zigzag）。同值同码完全确定。
 *
 * <p>与 Simple8b（6015）同族不同面：几何自适应变长 vs
 * 同域混合档位定宽；与 EliasGammaCodec（同包）不同面：
 * 参数化余数直存 vs 全 unary。
 */
public final class GolombRiceCodec {

    private GolombRiceCodec() {
    }

    /** 编码结果（位流 + 有效位长——尾部补零不入有效域）。 */
    public static final class BitStream {
        final byte[] bytes;
        final int bitLength;

        BitStream(byte[] bytes, int bitLength) {
            this.bytes = bytes;
            this.bitLength = bitLength;
        }

        public byte[] bytes() {
            return bytes;
        }

        public int bitLength() {
            return bitLength;
        }
    }

    /** 编码（k∈[0,30]；负值/null fail-fast）。 */
    public static BitStream encode(long[] values, int k) {
        if (values == null) {
            throw new IllegalArgumentException("值列非空");
        }
        requireParam(k);
        List<Boolean> bits = new ArrayList<>();
        for (long value : values) {
            if (value < 0) {
                throw new IllegalArgumentException("非负域: " + value);
            }
            long quotient = value >> k;
            long remainder = value - (quotient << k);
            for (long i = 0; i < quotient; i++) {
                bits.add(Boolean.TRUE);
            }
            bits.add(Boolean.FALSE);
            for (int i = k - 1; i >= 0; i--) {
                bits.add(((remainder >> i) & 1L) != 0);
            }
        }
        byte[] bytes = new byte[(bits.size() + 7) / 8];
        int cursor = 0;
        for (boolean bit : bits) {
            if (bit) {
                bytes[cursor >> 3] |= (byte) (1 << (7 - (cursor & 7)));
            }
            cursor++;
        }
        return new BitStream(bytes, bits.size());
    }

    /** 解码（count 条；越流 fail-fast）。 */
    public static long[] decode(BitStream stream, int k, int count) {
        if (stream == null || count < 0) {
            throw new IllegalArgumentException("流与条数合法");
        }
        requireParam(k);
        long[] out = new long[count];
        int cursor = 0;
        for (int n = 0; n < count; n++) {
            long quotient = 0;
            while (readBit(stream, cursor)) {
                quotient++;
                cursor++;
            }
            cursor++;
            long remainder = 0;
            for (int i = 0; i < k; i++) {
                remainder = (remainder << 1) | (readBit(stream, cursor) ? 1L : 0L);
                cursor++;
            }
            out[n] = (quotient << k) + remainder;
        }
        return out;
    }

    private static boolean readBit(BitStream stream, int index) {
        if (index >= stream.bitLength) {
            throw new IllegalArgumentException("位流越界（位 " + index + "）");
        }
        return (stream.bytes[index >> 3] & (1 << (7 - (index & 7)))) != 0;
    }

    private static void requireParam(int k) {
        if (k < 0 || k > 30) {
            throw new IllegalArgumentException("参数 k 须在 [0,30]: " + k);
        }
    }
}
