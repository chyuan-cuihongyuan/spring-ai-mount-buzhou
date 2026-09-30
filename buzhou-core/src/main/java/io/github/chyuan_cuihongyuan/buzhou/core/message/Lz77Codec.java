package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.ArrayList;
import java.util.List;

/**
 * LZ77 滑窗引用压缩（spec 9015 / W9031 / impl 2368）——Ziv–Lempel
 * 1977 思想（gzip/zlib 祖型）：**滑窗内贪心最长回引 (offset,
 * length) 替代重复段——历史即字典，无需随行码表**——逐字节独立
 * 编码（Huffman 需码表）与字典生长（LZW/LZ78 需建典）的另一半。
 * LZSS token 面：literal（length=0）或 match（offset>0、
 * length≥3）——无比特打包（位流归邻居）；贪心取最长（并列取
 * 最小 offset——同输入同 token 流完全确定）；窗口与输入同量级
 * 时退化为纯字面（诚实边界）；null/窗口非正/decode 非法 token
 * fail-fast。
 *
 * <p>与 LzwCodec（spec 9012）同族不同面：回窗引用 vs 字典生长；
 * 与 BWT/MTF（9013/9014）互补：上下文聚族预处理 vs 重复段引用。
 */
public final class Lz77Codec {

    private static final int MIN_MATCH = 3;

    /** token：literal（length=0 带 byte）或 match（offset>0、length≥3、byte 恒 0）。 */
    public record Token(int offset, int length, byte literal) {
    }

    /**
     * 编码（windowSize = 回引最大距离）。
     *
     * @throws IllegalArgumentException null 输入、窗口非正
     */
    public static List<Token> encode(byte[] data, int windowSize) {
        if (data == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        if (windowSize < 1) {
            throw new IllegalArgumentException("窗口为正（实际 " + windowSize + "）");
        }
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        while (i < data.length) {
            int windowStart = Math.max(0, i - windowSize);
            int bestLength = 0;
            int bestOffset = 0;
            for (int j = windowStart; j < i; j++) {
                int length = 0;
                while (i + length < data.length
                        && data[j + length] == data[i + length]
                        && length < windowSize) {
                    length++;
                }
                if (length > bestLength) {
                    bestLength = length;
                    bestOffset = i - j;
                }
            }
            if (bestLength >= MIN_MATCH) {
                tokens.add(new Token(bestOffset, bestLength, (byte) 0));
                i += bestLength;
            } else {
                tokens.add(new Token(0, 0, data[i]));
                i++;
            }
        }
        return tokens;
    }

    /**
     * 解码（encode 的精确逆）。
     *
     * @throws IllegalArgumentException null、非法 token（offset/length 越义）
     */
    public static byte[] decode(List<Token> tokens) {
        if (tokens == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        byte[] buffer = new byte[tokens.size() * 8];
        int size = 0;
        for (Token token : tokens) {
            if (token.length() == 0) {
                if (token.offset() != 0) {
                    throw new IllegalArgumentException("字面 token offset 必 0（实际 " + token.offset() + "）");
                }
                if (size == buffer.length) {
                    buffer = java.util.Arrays.copyOf(buffer, buffer.length * 2 + 8);
                }
                buffer[size++] = token.literal();
            } else {
                if (token.offset() < 1 || token.length() < MIN_MATCH) {
                    throw new IllegalArgumentException("匹配 token 非法（offset=" + token.offset()
                            + " length=" + token.length() + "）");
                }
                if (token.offset() > size) {
                    throw new IllegalArgumentException("回引越历史（offset=" + token.offset()
                            + " > 已译 " + size + "）");
                }
                int start = size - token.offset();
                if (size + token.length() > buffer.length) {
                    buffer = java.util.Arrays.copyOf(buffer, Math.max(buffer.length * 2, size + token.length()));
                }
                // 重叠引用（offset<length）逐字节回读——历史即字典的运行期语义
                for (int k = 0; k < token.length(); k++) {
                    buffer[size + k] = buffer[start + k];
                }
                size += token.length();
            }
        }
        return java.util.Arrays.copyOf(buffer, size);
    }
}
