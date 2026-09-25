package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.Arrays;

/**
 * Gorilla XOR 浮点压缩（spec 6013 / T6227 / impl 2214）——
 * Facebook Gorilla/Prometheus TSDB 思想：**相邻浮点 XOR 流的
 * 三态控制位压缩**——与前值同值只记一位 0；有效位落在前值
 * 前导/尾随零窗内复用窗（'10'+有效位）；否则记新窗（'11'+
 * 5 位前导+6 位有效长+有效位）——时间序列浮点逐值 8 字节
 * 直存（本地性序列压缩率极低）的病解。位级定构（同输入同
 * 位流——可审计）；无损按位保留（NaN/±0/Inf 亦保真）。
 *
 * <p>与 VarintCodec/DeltaFrameOfReference（同包）同族不同面：
 * 整数变长/差分参照 vs 浮点 XOR 有效位窗。
 */
public final class GorillaXor {

    private static final int NO_WINDOW = 255;

    private final long[] words;
    private final int bitLength;
    private final int count;

    private GorillaXor(long[] words, int bitLength, int count) {
        this.words = words;
        this.bitLength = bitLength;
        this.count = count;
    }

    /** 压缩浮点序列（null fail-fast；空序列允许）。 */
    public static GorillaXor compress(double[] values) {
        if (values == null) {
            throw new IllegalArgumentException("序列非空");
        }
        BitWriter writer = new BitWriter(values.length * 8 + 16);
        long prevBits = 0;
        int prevLeading = NO_WINDOW;
        int prevTrailing = 0;
        for (int i = 0; i < values.length; i++) {
            long bits = Double.doubleToRawLongBits(values[i]);
            if (i == 0) {
                writer.write(bits, 64);
            } else {
                long xor = bits ^ prevBits;
                if (xor == 0) {
                    writer.writeBit(0);
                } else {
                    int leading = Long.numberOfLeadingZeros(xor);
                    int trailing = Long.numberOfTrailingZeros(xor);
                    boolean windowReusable = prevLeading != NO_WINDOW
                            && leading >= prevLeading && trailing >= prevTrailing;
                    if (windowReusable) {
                        writer.writeBit(1);
                        writer.writeBit(0);
                        writer.write(xor >>> prevTrailing,
                                64 - prevLeading - prevTrailing);
                    } else {
                        int storedLeading = Math.min(leading, 31);
                        writer.writeBit(1);
                        writer.writeBit(1);
                        writer.write(storedLeading, 5);
                        writer.write(64 - storedLeading - trailing - 1, 6);
                        writer.write(xor >>> trailing, 64 - storedLeading - trailing);
                        prevLeading = storedLeading;
                        prevTrailing = trailing;
                    }
                }
            }
            prevBits = bits;
        }
        return new GorillaXor(writer.words(), writer.bitLength(), values.length);
    }

    /** 解压回浮点序列（无损——位模式全等）。 */
    public double[] decompress() {
        double[] out = new double[count];
        BitReader reader = new BitReader(words);
        long prevBits = 0;
        int prevLeading = NO_WINDOW;
        int prevTrailing = 0;
        for (int i = 0; i < count; i++) {
            long bits;
            if (i == 0) {
                bits = reader.read(64);
            } else {
                long xor;
                if (reader.readBit() == 0) {
                    xor = 0;
                } else if (reader.readBit() == 0) {
                    int meaningful = 64 - prevLeading - prevTrailing;
                    xor = reader.read(meaningful) << prevTrailing;
                } else {
                    int leading = (int) reader.read(5);
                    int meaningful = (int) reader.read(6) + 1;
                    int trailing = 64 - leading - meaningful;
                    xor = reader.read(meaningful) << trailing;
                    prevLeading = leading;
                    prevTrailing = trailing;
                }
                bits = prevBits ^ xor;
            }
            prevBits = bits;
            out[i] = Double.longBitsToDouble(bits);
        }
        return out;
    }

    /** 压缩位长读数（对照 originalBits）。 */
    public int compressedBits() {
        return bitLength;
    }

    /** 原始位长读数（64×n）。 */
    public long originalBits() {
        return 64L * count;
    }

    /** 元素数读数。 */
    public int count() {
        return count;
    }

    /** 仅限测试与审计：位流防御副本。 */
    long[] wordsCopy() {
        return Arrays.copyOf(words, words.length);
    }

    private static final class BitWriter {
        private long[] data;
        private int bitLength;

        BitWriter(int expectedBits) {
            this.data = new long[(expectedBits + 63) >>> 6];
        }

        void writeBit(int bit) {
            ensure(bitLength);
            if (bit != 0) {
                data[bitLength >>> 6] |= 1L << (bitLength & 63);
            }
            bitLength++;
        }

        void write(long value, int width) {
            for (int b = 0; b < width; b++) {
                writeBit((int) (value >>> b & 1));
            }
        }

        private void ensure(int pos) {
            int word = pos >>> 6;
            if (word >= data.length) {
                data = Arrays.copyOf(data, data.length * 2 + 1);
            }
        }

        int bitLength() {
            return bitLength;
        }

        long[] words() {
            return data;
        }
    }

    private static final class BitReader {
        private final long[] data;
        private int pos;

        BitReader(long[] data) {
            this.data = data;
        }

        int readBit() {
            int bit = (int) (data[pos >>> 6] >>> (pos & 63) & 1);
            pos++;
            return bit;
        }

        long read(int width) {
            long out = 0;
            for (int b = 0; b < width; b++) {
                out |= (long) readBit() << b;
            }
            return out;
        }
    }
}
