package io.github.chyuan_cuihongyuan.buzhou.core.cache;

/**
 * SCAN 游标编解码（spec 1880 / T2961 / impl 1481）——Redis SCAN 的
 * 反向递增游标：游标不是偏移量，而是「位反转空间」里的计数器——
 * 每步 {@code v' = rev(rev(v) + 1)}。表尺寸固定时全周游不重不漏、
 * 绕满一圈精确归 0；表扩缩容时桶序与 rehash 迁移序天然对齐（先扫
 * 低位原桶、后扫高位新桶的逆反序）。
 *
 * <p>纯计算零状态；白盒面（存储层用 Lettuce 客户端黑盒扫描）。
 */
public final class ScanCursorCodec {

    private ScanCursorCodec() {
    }

    /**
     * 下一个游标：位反转空间 +1 一步（rev_bits(rev_bits(cursor)+1)）。
     * 周游完成精确回 0。契约：cursor ≥ 0、tableBits ∈ [1,63]
     * （fail-fast）。
     */
    public static long nextCursor(long cursor, int tableBits) {
        validate(cursor, tableBits);
        long reversed = Long.reverse(cursor) >>> (64 - tableBits);
        long mask = (1L << tableBits) - 1;
        reversed = (reversed + 1) & mask;
        return Long.reverse(reversed) >>> (64 - tableBits);
    }

    /**
     * 迭代完成判定：游标 0 即全周游结束（或尚未开始的首态——由调用方
     * 以「先取数再推进」的循环语义区分）。
     */
    public static boolean isComplete(long cursor) {
        if (cursor < 0) {
            throw new IllegalArgumentException("游标不能为负：" + cursor);
        }
        return cursor == 0;
    }

    /**
     * 同表尺寸全周游桶数：2^tableBits。契约：tableBits ∈ [1,62]
     * （63 平方溢出 long 由调用方自担，fail-fast）。
     */
    public static long cycleLength(int tableBits) {
        if (tableBits < 1 || tableBits > 62) {
            throw new IllegalArgumentException(
                    "tableBits 须在 [1,62]：" + tableBits);
        }
        return 1L << tableBits;
    }

    private static void validate(long cursor, int tableBits) {
        if (cursor < 0) {
            throw new IllegalArgumentException("游标不能为负：" + cursor);
        }
        if (tableBits < 1 || tableBits > 63) {
            throw new IllegalArgumentException(
                    "tableBits 须在 [1,63]：" + tableBits);
        }
    }
}
