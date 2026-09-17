package io.github.chyuan_cuihongyuan.buzhou.core.message;

/**
 * varint 编解码（spec 3039 / T5079 / impl 2039）——Protobuf
 * LEB128 + zigzag 思想：有符号 long 先 zigzag 映射无符号（负数
 * 不再 10 字节顶格——−1 编 1 字节），再 7 位/字节小端变长（小值
 * 1 字节起步，值域每升 7 位加一字节）——键值对/计数器/时间戳
 * 等小值占多数的字节流紧凑地基件。
 *
 * <p>纯函数静态件；decodeAt 游标流式推进；截断串 fail-fast。
 */
public final class VarintCodec {

    /** 解码结果（值 + 消费字节数——流式推进面）。 */
    public record Decoded(long value, int bytesRead) {
    }

    private VarintCodec() {
    }

    /** zigzag：有符号 → 无符号交错映射（0→0/−1→1/1→2/−2→3）。 */
    public static long zigzagEncode(long value) {
        return (value << 1) ^ (value >> 63);
    }

    /** zigzag 逆映射。 */
    public static long zigzagDecode(long encoded) {
        return (encoded >>> 1) ^ -(encoded & 1);
    }

    /** 编码：zigzag → LEB128（每字节低 7 位，最高位续位标记）。 */
    public static byte[] encode(long value) {
        long v = zigzagEncode(value);
        int length = encodedLength(v);
        byte[] out = new byte[length];
        int index = 0;
        do {
            byte b = (byte) (v & 0x7F);
            v >>>= 7;
            if (v != 0) {
                b |= (byte) 0x80;
            }
            out[index++] = b;
        } while (v != 0);
        return out;
    }

    /** 游标解码（截断 fail-fast——末字节不得带续位）。 */
    public static Decoded decodeAt(byte[] bytes, int offset) {
        if (bytes == null || offset < 0 || offset >= bytes.length) {
            throw new IllegalArgumentException("游标越界：" + (bytes == null ? "null" : offset));
        }
        long value = 0;
        int shift = 0;
        int index = offset;
        while (true) {
            if (index >= bytes.length) {
                throw new IllegalArgumentException("varint 截断（末字节带续位无后续）");
            }
            byte b = bytes[index++];
            value |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return new Decoded(zigzagDecode(value), index - offset);
            }
            shift += 7;
            if (shift >= 64) {
                throw new IllegalArgumentException("varint 超 long 位宽");
            }
        }
    }

    /** 整串恰一 varint 解码。 */
    public static long decode(byte[] bytes) {
        Decoded decoded = decodeAt(bytes, 0);
        if (decoded.bytesRead() != bytes.length) {
            throw new IllegalArgumentException("残留字节（恰一 varint 口径）："
                    + decoded.bytesRead() + "/" + bytes.length);
        }
        return decoded.value();
    }

    /** LEB128 字节数（无符号值 v——7 位/字节）。 */
    private static int encodedLength(long v) {
        int length = 1;
        while ((v >>>= 7) != 0) {
            length++;
        }
        return length;
    }
}
