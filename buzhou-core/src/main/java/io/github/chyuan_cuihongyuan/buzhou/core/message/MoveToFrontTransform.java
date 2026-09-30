package io.github.chyuan_cuihongyuan.buzhou.core.message;

/**
 * Move-to-Front 变换（spec 9014 / W9029 / impl 2367）——MTF 思想
 * （Bentley 1986——bzip2 BWT 后半同源）：**256 表初始序，每字符
 * 出其当前位次并移至表首**——BWT 聚簇后的末列被译成「大量 0/1
 * 的小整数偏斜分布」（熵编码陡增效）——直接对聚簇列做 Huffman
 * 吃不到局部性红利的病解。可逆：decode 按同一规则同构重建；
 * 确定性（同输入同输出完全确定）；null fail-fast。
 *
 * <p>与 BurrowsWheelerTransform（spec 9013）组成 bzip2 前半管线
 * （BWT 聚簇 → MTF 偏斜化）；与 RunLengthCodec（同包）接力
 * （MTF 后的 0 游程是 RLE 的最优输入）。
 */
public final class MoveToFrontTransform {

    private MoveToFrontTransform() {
    }

    /**
     * 正变换（输出 = 各字符在当时的表内位次 0..255）。
     *
     * @throws IllegalArgumentException null 输入
     */
    public static byte[] encode(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        int[] table = new int[256];
        int[] position = new int[256];
        for (int b = 0; b < 256; b++) {
            table[b] = b;
            position[b] = b;
        }
        byte[] ranks = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            int symbol = data[i] & 0xFF;
            int rank = position[symbol];
            ranks[i] = (byte) rank;
            if (rank > 0) {
                // 位次 > 当前端点者整体后移一位；symbol 上表首
                for (int r = rank; r > 0; r--) {
                    int moved = table[r - 1];
                    table[r] = moved;
                    position[moved] = r;
                }
                table[0] = symbol;
                position[symbol] = 0;
            }
        }
        return ranks;
    }

    /**
     * 逆变换（encode 的精确逆——同构重建表）。
     *
     * @throws IllegalArgumentException null 输入
     */
    public static byte[] decode(byte[] ranks) {
        if (ranks == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        int[] table = new int[256];
        for (int b = 0; b < 256; b++) {
            table[b] = b;
        }
        byte[] data = new byte[ranks.length];
        for (int i = 0; i < ranks.length; i++) {
            int rank = ranks[i] & 0xFF;
            int symbol = table[rank];
            data[i] = (byte) symbol;
            if (rank > 0) {
                System.arraycopy(table, 0, table, 1, rank);
                table[0] = symbol;
            }
        }
        return data;
    }
}
