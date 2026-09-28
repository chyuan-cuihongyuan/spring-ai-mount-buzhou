package io.github.chyuan_cuihongyuan.buzhou.core.message;

/**
 * 行程编码（spec 7004 / U7209 / impl 2256）——RLE（run-length
 * encoding）思想（PNG/传真 G3 同源）：**连续重复段折叠为
 * (count, value) 对**，count∈[1,255]（超长行程切段）——重复
 * 率高的列/位图逐值直存（存储随行程数放大）的病解。解码
 * 奇长度/零计数 fail-fast（对偶不完整与零长行程非法）；
 * pairCount 读数显形压缩比面。全重复列压至 n/255 对，全
 * 相异列诚实膨胀 2×（对账读数可感——不隐瞒反面）。
 *
 * <p>与 DictionaryEncoding（spec 6017）同族不同面：局部重复
 * 折叠 vs 全列基数收缩；与 Simple8b（6015）不同面：行程对
 * vs 同域变长位打包。
 */
public final class RunLengthCodec {

    private static final int MAX_RUN = 255;

    private RunLengthCodec() {
    }

    /** 编码：连续重复段折叠 (count,value) 对（超 255 切段；null fail-fast）。 */
    public static byte[] encode(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("数据非空");
        }
        if (data.length == 0) {
            return new byte[0];
        }
        byte[] out = new byte[2 * data.length];
        int outCursor = 0;
        int runStart = 0;
        for (int i = 1; i <= data.length; i++) {
            boolean runBroken = i == data.length
                    || data[i] != data[runStart]
                    || i - runStart == MAX_RUN;
            if (runBroken) {
                out[outCursor++] = (byte) (i - runStart);
                out[outCursor++] = data[runStart];
                runStart = i;
            }
        }
        byte[] trimmed = new byte[outCursor];
        System.arraycopy(out, 0, trimmed, 0, outCursor);
        return trimmed;
    }

    /** 解码（奇长度/零计数 fail-fast——对偶不完整与零长行程非法）。 */
    public static byte[] decode(byte[] encoded) {
        if (encoded == null) {
            throw new IllegalArgumentException("编码列非空");
        }
        if (encoded.length % 2 != 0) {
            throw new IllegalArgumentException("编码列须为 (count,value) 偶长对偶: " + encoded.length);
        }
        int total = 0;
        for (int i = 0; i < encoded.length; i += 2) {
            int count = encoded[i] & 0xFF;
            if (count == 0) {
                throw new IllegalArgumentException("零长行程非法（位 " + i + "）");
            }
            total += count;
        }
        byte[] out = new byte[total];
        int cursor = 0;
        for (int i = 0; i < encoded.length; i += 2) {
            int count = encoded[i] & 0xFF;
            byte value = encoded[i + 1];
            for (int j = 0; j < count; j++) {
                out[cursor++] = value;
            }
        }
        return out;
    }

    /** 对偶数读数（压缩比面：对偶数×2 vs 原长）。 */
    public static int pairCount(byte[] encoded) {
        if (encoded == null || encoded.length % 2 != 0) {
            throw new IllegalArgumentException("编码列非空且须偶长");
        }
        return encoded.length / 2;
    }
}
