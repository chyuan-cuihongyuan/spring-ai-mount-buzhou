package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.io.ByteArrayOutputStream;

import io.github.chyuan_cuihongyuan.buzhou.core.message.VarintCodec.Decoded;

/**
 * 增量+基准帧编码（spec 4008 / T6017 / impl 2109）——列存排序块
 * 压缩思想（Parquet DELTA_BINARY_PACKED / ORC 思想）：排序整数列
 * 的相邻差远小于绝对值——每帧记**基准值（frame of reference）**
 * 原样，帧内余值只记与前一值的**增量（delta）**，增量再走
 * zigzag varint 紧凑——时间戳/序号/偏移等单调（近单调）流的
 * 「绝对值大、差值小」病解。
 *
 * <p>帧化（frameSize）：帧界即基准重置点——大幅跳变（分块边界/
 * 重启点）不被长程增量拖累；帧尾不足整帧以计数收官。与
 * VarintCodec（单值自界定）成对复用、与 EliasGamma/Huffman
 * （符号流）成编码四档。
 */
public final class DeltaFrameOfReference {

    /** 编码输出（字节 + 值计数——解码定界用）。 */
    public record Encoded(byte[] bytes, int count) {
    }

    private final int frameSize;

    /** 定构（frameSize ≥1 否则 fail-fast；1 = 全基准无增量退化档）。 */
    public DeltaFrameOfReference(int frameSize) {
        if (frameSize < 1) {
            throw new IllegalArgumentException("frameSize ≥1：" + frameSize);
        }
        this.frameSize = frameSize;
    }

    /** 编码：逐帧（基准原样 + 帧内增量）全部 zigzag varint。 */
    public Encoded encode(long[] values) {
        if (values == null) {
            throw new IllegalArgumentException("values 非 null");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int frameStart = 0; frameStart < values.length; frameStart += frameSize) {
            int frameEnd = Math.min(frameStart + frameSize, values.length);
            long previous = values[frameStart];
            append(out, previous);   // 基准
            for (int i = frameStart + 1; i < frameEnd; i++) {
                append(out, values[i] - previous);   // 增量
                previous = values[i];
            }
        }
        return new Encoded(out.toByteArray(), values.length);
    }

    /** 解码（count 驱动逐帧推进；流截断 fail-fast）。 */
    public long[] decode(byte[] bytes, int count) {
        if (bytes == null || count < 0) {
            throw new IllegalArgumentException("bytes 非 null / count ≥0");
        }
        long[] out = new long[count];
        int pos = 0;
        int cursor = 0;
        while (pos < count) {
            Decoded base = VarintCodec.decodeAt(bytes, cursor);
            cursor += base.bytesRead();
            out[pos++] = base.value();
            for (int i = 1; i < frameSize && pos < count; i++) {
                Decoded delta = VarintCodec.decodeAt(bytes, cursor);
                cursor += delta.bytesRead();
                out[pos] = out[pos - 1] + delta.value();
                pos++;
            }
        }
        if (cursor != bytes.length) {
            throw new IllegalArgumentException("残留字节：" + cursor + " / " + bytes.length);
        }
        return out;
    }

    /** 帧长读数。 */
    public int frameSize() {
        return frameSize;
    }

    private static void append(ByteArrayOutputStream out, long value) {
        byte[] encoded = VarintCodec.encode(value);
        out.write(encoded, 0, encoded.length);
    }
}
