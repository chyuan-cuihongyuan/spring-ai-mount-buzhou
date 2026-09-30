package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Arrays;

/**
 * 二分内插编码（spec 10001 / X10003 / impl 2404）——Moffat–Stuiver 2000
 * 思想（「递归中值定宽」——Lucene block postings/倒排索引同源）：
 * **有序整数列按中点递归二分——中点元素以上下文区间 [low, high] 的
 * 定宽二进制写入（width=bits(high−low)），再递归两半**——上下文
 * 收紧使位宽随递归坍缩，单调表压缩远胜逐元素定宽——Elias–Fano
 * （core/message 已占）的姊妹面：分桶位流 vs 上下文递归。非负整数、
 * 允许重复（非严格单调契约）；null/空/负值/乱序 fail-fast；同输入
 * 同码流完全确定。
 */
public final class InterpolativeCoding {

    /** 位流容器单字节数值域上界（byte 无符号 255）。 */
    private static final int BYTE_MASK = 0xFF;

    private InterpolativeCoding() {
    }

    /**
     * 编码结果（bytes=码流；count=元素个数——解码侧重构导向）。
     *
     * @param bytes 码流字节
     * @param count 编码元素个数
     */
    public record Encoded(byte[] bytes, int count) {
    }

    /**
     * 编码非负有序（非严格）整数列。
     *
     * @throws IllegalArgumentException null/空/负值/乱序
     */
    public static Encoded encode(int[] sortedAscending) {
        if (sortedAscending == null || sortedAscending.length == 0) {
            throw new IllegalArgumentException("输入非空且非 null（实际 "
                    + (sortedAscending == null ? "null" : "空") + "）");
        }
        for (int i = 0; i < sortedAscending.length; i++) {
            if (sortedAscending[i] < 0) {
                throw new IllegalArgumentException("值非负（第 " + i + " 个实际 "
                        + sortedAscending[i] + "）");
            }
            if (i > 0 && sortedAscending[i] < sortedAscending[i - 1]) {
                throw new IllegalArgumentException("输入须非严格递增（第 " + i
                        + " 个 " + sortedAscending[i] + " < 前值 "
                        + sortedAscending[i - 1] + "）");
            }
        }
        BitWriter writer = new BitWriter();
        // 首写末元素全宽 32 位——解码侧以同值播种上界，递归位宽两侧一致
        writer.writeBits(sortedAscending[sortedAscending.length - 1], Integer.SIZE);
        writeRecursive(writer, sortedAscending, 0, sortedAscending.length - 1,
                0, sortedAscending[sortedAscending.length - 1]);
        return new Encoded(writer.toBytes(), sortedAscending.length);
    }

    /**
     * 解码重构（count 取自编码结果）。
     *
     * @throws IllegalArgumentException null 码流
     */
    public static int[] decode(Encoded encoded) {
        if (encoded == null || encoded.bytes == null) {
            throw new IllegalArgumentException("码流非 null");
        }
        int[] out = new int[encoded.count];
        BitReader reader = new BitReader(encoded.bytes);
        if (encoded.count > 0) {
            int last = reader.readBits(Integer.SIZE);
            readRecursive(reader, out, 0, encoded.count - 1, 0, last);
        }
        return out;
    }

    private static void writeRecursive(BitWriter writer, int[] values, int lo, int hi,
                                       int lowBound, int highBound) {
        if (lo > hi) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        writer.writeBits(values[mid] - lowBound, bitsNeeded(highBound - lowBound));
        writeRecursive(writer, values, lo, mid - 1, lowBound, values[mid]);
        writeRecursive(writer, values, mid + 1, hi, values[mid], highBound);
    }

    private static void readRecursive(BitReader reader, int[] out, int lo, int hi,
                                      int lowBound, int highBound) {
        if (lo > hi) {
            return;
        }
        int mid = (lo + hi) >>> 1;
        out[mid] = lowBound + (int) reader.readBits(bitsNeeded(highBound - lowBound));
        readRecursive(reader, out, lo, mid - 1, lowBound, out[mid]);
        readRecursive(reader, out, mid + 1, hi, out[mid], highBound);
    }

    /** x≥1 时为其位宽、x=0 时为 0（负域在上界钳制下不可达）。 */
    private static int bitsNeeded(int x) {
        return 32 - Integer.numberOfLeadingZeros(x);
    }

    /** 低位先出位写入器（grow-by-double，末字节零填充到字节界）。 */
    private static final class BitWriter {

        private byte[] buf = new byte[16];
        private int length;
        private int bitPosition;

        void writeBits(int value, int width) {
            for (int i = 0; i < width; i++) {
                if ((bitPosition >> 3) == buf.length) {
                    buf = Arrays.copyOf(buf, buf.length * 2);
                }
                if ((value >>> i & 1) == 1) {
                    buf[bitPosition >> 3] |= (byte) (1 << (bitPosition & 7));
                }
                bitPosition++;
            }
            length = (bitPosition + 7) >> 3;
        }

        byte[] toBytes() {
            return Arrays.copyOf(buf, length);
        }
    }

    /** 低位先出位读取器（与 BitWriter 对偶）。 */
    private static final class BitReader {

        private final byte[] buf;
        private int bitPosition;

        BitReader(byte[] buf) {
            this.buf = buf;
        }

        int readBits(int width) {
            int value = 0;
            for (int i = 0; i < width; i++) {
                value |= (buf[bitPosition >> 3] >> (bitPosition & 7) & 1) << i;
                bitPosition++;
            }
            return value;
        }
    }
}
