package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.Arrays;

/**
 * Burrows–Wheeler 变换（spec 9013 / W9027 / impl 2366）——BWT
 * 思想（Burrows–Weller 1994——bzip2 块预处理同源）：**全部循环
 * 旋转字典序排序，取末列 + 原串行位**——同上下文字符在末列聚
 * 簇（后续 MTF/RLE 压缩率陡升）的可逆重排，本身不压缩——
 * 直接熵编码吃不到上下文冗余的病解。无哨兵变体（primaryIndex
 * 随行）；排序确定性（同输入同输出完全确定）；null fail-fast。
 *
 * <p>与 MoveToFrontTransform（spec 9014）组成 bzip2 前半管线
 * （BWT 聚簇 → MTF 偏斜化）；与 SuffixArray（metrics 域）同根：
 * 循环旋转序 vs 后缀序。
 */
public final class BurrowsWheelerTransform {

    private BurrowsWheelerTransform() {
    }

    /** BWT 结果（末列 + 原串行位——逆变换双钥匙）。 */
    public record BwtResult(byte[] lastColumn, int primaryIndex) {
    }

    /**
     * 正变换（旋转排序 O(n² log n) 朴素面——块尺寸明示）。
     *
     * @throws IllegalArgumentException null 输入
     */
    public static BwtResult transform(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        int n = data.length;
        Integer[] rotations = new Integer[n];
        for (int i = 0; i < n; i++) {
            rotations[i] = i;
        }
        final byte[] d = data;
        Arrays.sort(rotations, (a, b) -> {
            for (int k = 0; k < n; k++) {
                int ca = d[(a + k) % n] & 0xFF;
                int cb = d[(b + k) % n] & 0xFF;
                if (ca != cb) {
                    return Integer.compare(ca, cb);
                }
            }
            return Integer.compare(a, b);
        }
        );
        byte[] last = new byte[n];
        int primary = -1;
        for (int rank = 0; rank < n; rank++) {
            int rotation = rotations[rank];
            last[rank] = data[(rotation + n - 1) % n];
            if (rotation == 0) {
                primary = rank;
            }
        }
        return new BwtResult(last, primary);
    }

    /**
     * 逆变换（LF 映射重建——primaryIndex 行回推）。
     *
     * @throws IllegalArgumentException null/行位越域
     */
    public static byte[] inverse(byte[] lastColumn, int primaryIndex) {
        if (lastColumn == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        int n = lastColumn.length;
        if (primaryIndex < 0 || primaryIndex >= n) {
            throw new IllegalArgumentException("行位越域（" + primaryIndex + "/" + n + "）");
        }
        if (n == 0) {
            return new byte[0];
        }
        // LF 映射：同字符按出现序稳定配对（首列序 = 末列同字符稳定序）
        int[] counts = new int[256];
        for (byte b : lastColumn) {
            counts[b & 0xFF]++;
        }
        int[] starts = new int[256];
        int sum = 0;
        for (int c = 0; c < 256; c++) {
            starts[c] = sum;
            sum += counts[c];
        }
        int[] occurrence = new int[256];
        int[] lf = new int[n];
        for (int i = 0; i < n; i++) {
            int c = lastColumn[i] & 0xFF;
            lf[i] = starts[c] + occurrence[c]++;
        }
        // 从 primaryIndex 行末字符起沿 LF 回推（逆序重建）
        byte[] result = new byte[n];
        int index = primaryIndex;
        for (int k = n - 1; k >= 0; k--) {
            result[k] = lastColumn[index];
            index = lf[index];
        }
        return result;
    }
}
