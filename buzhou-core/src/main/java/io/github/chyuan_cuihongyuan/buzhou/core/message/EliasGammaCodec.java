package io.github.chyuan_cuihongyuan.buzhou.core.message;

/**
 * Elias gamma 编解码（spec 3036 / T5073 / impl 2037）——Elias 1975
 * 通用整数前缀码思想：正整数 N=2^k+r 编为 **k 个零 + 边界 1 + k 位
 * 余数**——小数极短（1→"1"、2→"010"）、无解码表（零计数自定界）；
 * 幂律分布（事件计数/频次/差分增量）的紧凑传输地基件。
 *
 * <p>纯函数静态件；串行流解码用 {@link #decodeAt} 游标推进。
 */
public final class EliasGammaCodec {

    /** 解码结果（值 + 下一游标位——流式推进面）。 */
    public record Decoded(int value, int nextOffset) {
    }

    private EliasGammaCodec() {
    }

    /** 编码（n ≥ 1 → [0]^k + 1 + k 位余数）。 */
    public static String encode(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n ≥ 1：" + n);
        }
        int k = 31 - Integer.numberOfLeadingZeros(n);
        int remainder = n - (1 << k);
        StringBuilder bits = new StringBuilder(2 * k + 1);
        bits.append("0".repeat(k));
        bits.append('1');
        for (int bit = k - 1; bit >= 0; bit--) {
            bits.append((remainder >>> bit) & 1);
        }
        return bits.toString();
    }

    /** 码长（位——2k+1）。 */
    public static int bitLength(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n ≥ 1：" + n);
        }
        int k = 31 - Integer.numberOfLeadingZeros(n);
        return 2 * k + 1;
    }

    /** 整串恰一码字解码（残留位/空串/坏前缀 fail-fast）。 */
    public static int decode(String bits) {
        if (bits == null || bits.isEmpty()) {
            throw new IllegalArgumentException("bits 非空");
        }
        Decoded decoded = decodeAt(bits, 0);
        if (decoded.nextOffset() != bits.length()) {
            throw new IllegalArgumentException("残留位（恰一码字口径）：offset "
                    + decoded.nextOffset() + " / " + bits.length());
        }
        return decoded.value();
    }

    /** 游标解码（零计数定界——无表自同步推进）。 */
    public static Decoded decodeAt(String bits, int from) {
        if (bits == null || from < 0 || from >= bits.length()) {
            throw new IllegalArgumentException("游标越界：" + from);
        }
        int zero = 0;
        int cursor = from;
        while (cursor < bits.length() && bits.charAt(cursor) == '0') {
            zero++;
            cursor++;
        }
        if (cursor >= bits.length()) {
            throw new IllegalArgumentException("零串无边界 1");
        }
        cursor++;   // 边界 1
        int remainderBits = zero;
        if (cursor + remainderBits > bits.length()) {
            throw new IllegalArgumentException("余数位不足");
        }
        int value = 1;
        for (int bit = 0; bit < remainderBits; bit++) {
            value = (value << 1) | (bits.charAt(cursor + bit) - '0');
        }
        return new Decoded(value, cursor + remainderBits);
    }
}
