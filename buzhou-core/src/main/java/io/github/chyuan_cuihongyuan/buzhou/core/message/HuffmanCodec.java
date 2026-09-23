package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Huffman 前缀码（spec 4006 / T6013 / impl 2107）——频率最优前缀
 * 编码思想（Huffman 1952；zlib/deflate 熵编码级）：高频符号短码、
 * 低频符号长码，码字无歧义前缀（解码自同步）；本件落 **规范
 * Huffman（canonical）**——码本只存每符号码长，码字按 (码长, 符号)
 * 字典序推导（deflate 同款），码表随码长走不随树走。
 *
 * <p>定长 8 位/字节的等概率浪费病（工具签名/文本骨架等倾斜流）的
 * 最优前缀解；输出位打包（MSB 先）。与 EliasGamma（幂律整数、无
 * 表）/Varint（字节界自界定）成编码三档按符号分布选型。
 */
public final class HuffmanCodec {

    /** 编码输出（位打包字节 + 总位长——解码定界用）。 */
    public record Encoded(byte[] bytes, long bitCount) {
    }

    private static final int SYMBOLS = 256;
    private static final int MAX_LEN = 57;   // 规范码 long 位宽安全上限

    private final int[] lengths = new int[SYMBOLS];
    private final long[] codes = new long[SYMBOLS];
    private final long[] firstCode = new long[MAX_LEN + 1];
    private final int[] symbolsByLength = new int[SYMBOLS];
    private final int[] firstIndex = new int[MAX_LEN + 2];

    /** 自 256 桶频率定构（全零 fail-fast；单符号退化 1 位码；树深 >57 fail-fast）。 */
    public HuffmanCodec(long[] frequencies) {
        if (frequencies == null || frequencies.length != SYMBOLS) {
            throw new IllegalArgumentException("frequencies 须 256 桶");
        }
        long total = 0;
        int distinct = 0;
        int lastSymbol = -1;
        for (int s = 0; s < SYMBOLS; s++) {
            if (frequencies[s] < 0) {
                throw new IllegalArgumentException("频率非负：" + s);
            }
            if (frequencies[s] > 0) {
                distinct++;
                lastSymbol = s;
                total += frequencies[s];
            }
        }
        if (total == 0) {
            throw new IllegalArgumentException("至少一个非零频率");
        }
        if (distinct == 1) {
            lengths[lastSymbol] = 1;   // 单符号退化：1 位码
        } else {
            buildLengths(frequencies);
        }
        for (int len : lengths) {
            if (len > MAX_LEN) {
                throw new IllegalArgumentException("频率分布病态深（>57 级）");
            }
        }
        assignCanonicalCodes();
        buildDecodeTable();
    }

    /** 编码（逐符号查码表位打包，MSB 先）。 */
    public Encoded encode(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data 非 null");
        }
        long bitCount = 0;
        for (byte b : data) {
            if (lengths[b & 0xff] == 0) {
                throw new IllegalArgumentException("符号未入码表：" + (b & 0xff));
            }
            bitCount += lengths[b & 0xff];
        }
        byte[] out = new byte[(int) ((bitCount + 7) / 8)];
        long bitPos = 0;
        for (byte b : data) {
            int symbol = b & 0xff;
            long code = codes[symbol];
            for (int i = lengths[symbol] - 1; i >= 0; i--) {
                if (((code >>> i) & 1) == 1) {
                    out[(int) (bitPos >> 3)] |= (byte) (0x80 >>> (bitPos & 7));
                }
                bitPos++;
            }
        }
        return new Encoded(out, bitCount);
    }

    /** 解码（规范码逐位对表：码长内首码偏移命中即出符号；截断/坏码 fail-fast）。 */
    public byte[] decode(byte[] encoded, long bitCount) {
        if (encoded == null || bitCount < 0) {
            throw new IllegalArgumentException("encoded 非 null / bitCount ≥0");
        }
        if ((bitCount + 7) / 8 > encoded.length) {
            throw new IllegalArgumentException("位长超出字节面：" + bitCount);
        }
        int minLen = Integer.MAX_VALUE;
        for (int len : lengths) {
            if (len > 0) {
                minLen = Math.min(minLen, len);
            }
        }
        byte[] out = new byte[(int) (bitCount / minLen)];
        long bitPos = 0;
        int outPos = 0;
        while (bitPos < bitCount) {
            long code = 0;
            boolean matched = false;
            for (int len = 1; len <= MAX_LEN && bitPos < bitCount; len++) {
                int bit = (encoded[(int) (bitPos >> 3)] >> (7 - (bitPos & 7))) & 1;
                bitPos++;
                code = (code << 1) | bit;
                int offset = (int) (code - firstCode[len]);
                if (offset >= 0 && offset < firstIndex[len + 1] - firstIndex[len]) {
                    out[outPos++] = (byte) symbolsByLength[firstIndex[len] + offset];
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new IllegalArgumentException("位流无法解码（截断/坏码）");
            }
        }
        return Arrays.copyOf(out, outPos);
    }

    /** 符号码长读数（未入码表 0）。 */
    public int codeLength(byte symbol) {
        return lengths[symbol & 0xff];
    }

    /** 码本尺寸读数（入表符号数）。 */
    public int distinctSymbols() {
        int n = 0;
        for (int len : lengths) {
            if (len > 0) {
                n++;
            }
        }
        return n;
    }

    /** 建树节点（freq 并列按 symbol 序——合并确定性）。 */
    private record Node(long freq, int symbol, Node left, Node right) {
    }

    /** 频率建树取码长（堆合并——freq 并列按符号序确定性）。 */
    private void buildLengths(long[] frequencies) {
        PriorityQueue<Node> heap = new PriorityQueue<>((a, b) -> {
            int byFreq = Long.compare(a.freq(), b.freq());
            return byFreq != 0 ? byFreq : Integer.compare(a.symbol(), b.symbol());
        });
        for (int s = 0; s < SYMBOLS; s++) {
            if (frequencies[s] > 0) {
                heap.add(new Node(frequencies[s], s, null, null));
            }
        }
        Node root = heap.poll();
        while (!heap.isEmpty()) {
            Node right = heap.poll();
            Node merged = new Node(root.freq() + right.freq(),
                    Math.min(root.symbol(), right.symbol()), root, right);
            heap.add(merged);
            root = heap.poll();
        }
        assignLengths(root, 0);
    }

    private void assignLengths(Node node, int depth) {
        if (node.left() == null && node.right() == null) {
            lengths[node.symbol()] = Math.max(1, depth);
            return;
        }
        assignLengths(node.left(), depth + 1);
        assignLengths(node.right(), depth + 1);
    }

    /** 规范码赋值：按 (码长, 符号) 字典序连续编号——firstCode 逐长左移推进。 */
    private void assignCanonicalCodes() {
        int[] count = new int[MAX_LEN + 1];
        for (int len : lengths) {
            if (len > 0) {
                count[len]++;
            }
        }
        long code = 0;
        for (int len = 1; len <= MAX_LEN; len++) {
            firstCode[len] = code;
            code = (code + count[len]) << 1;
        }
        long[] next = firstCode.clone();
        for (int s = 0; s < SYMBOLS; s++) {
            if (lengths[s] > 0) {
                codes[s] = next[lengths[s]]++;
            }
        }
    }

    /** 解码表：symbolsByLength 按 (码长, 符号) 排列表 + 每长首下标。 */
    private void buildDecodeTable() {
        int idx = 0;
        for (int len = 1; len <= MAX_LEN; len++) {
            firstIndex[len] = idx;
            for (int s = 0; s < SYMBOLS; s++) {
                if (lengths[s] == len) {
                    symbolsByLength[idx++] = s;
                }
            }
        }
        firstIndex[MAX_LEN + 1] = idx;
    }
}
