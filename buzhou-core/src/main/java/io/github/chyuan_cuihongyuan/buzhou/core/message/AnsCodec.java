package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.ArrayList;
import java.util.List;

/**
 * rANS 熵编码（spec 9016 / W9033 / impl 2369）——ANS 思想
 * （Duda 2009/Jarząbczyk 2014——Zstd/LZFSE 嫡系同源）：**非对称
 * 数系：编码态 x 单个大整数，每符号 x←(x/f)·M+cum+x%f（商槽
 * 携频次信息），字节重整化出入——算术编码的压缩率 + Huffman
 * 的表速查**——静态熵表需按整块重排（Huffman 整符号）或逐位
 * 区间缩放（算术编码慢）的中间形态。静态 256 字母表随行
 * （512B 头）+M=2¹² 归一化频率+L=2¹⁶ 编码态下界；编码逆序
 * 处理/解码正序回放（栈语义）；long 态域 [L,2³²)；归一化取
 * max(1,·) 差额归最大频率项（确定）；空输入空流；null/表
 * 损坏 fail-fast。
 *
 * <p>与 HuffmanCodec（同包）同域不同面：整符号前缀码 vs 非对
 * 称数系（更贴熵界）；与 LzwCodec/Lz77Codec（9012/9015）互补：
 * 建模层 vs 熵层。
 */
public final class AnsCodec {

    private static final int M_BITS = 12;
    private static final int M = 1 << M_BITS;
    private static final long Q = 1L << 16;
    private static final long STATE_FLOOR = Q * M;

    private AnsCodec() {
    }

    /**
     * 编码（自包含流：长度 + 512B 频率表 + 4B 终态 + 重整字节）。
     *
     * @throws IllegalArgumentException null 输入
     */
    public static byte[] encode(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        int n = data.length;
        if (n == 0) {
            return new byte[0];
        }
        int[] freq = normalizedFrequencies(data);
        int[] cum = cumulative(freq);
        long x = STATE_FLOOR;
        List<Byte> renorm = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            int s = data[i] & 0xFF;
            // 重整阈 = f·256·Q：态落入 [f·Q, 256f·Q)——与解码侧 [Q·M 态域) 字节配对恰等
            long threshold = (long) freq[s] * 256 * Q;
            while (x >= threshold) {
                renorm.add((byte) (x & 0xFF));
                x >>= 8;
            }
            x = (x / freq[s]) * M + cum[s] + (x % freq[s]);
        }
        byte[] out = new byte[4 + 512 + 8 + renorm.size()];
        int p = 0;
        out[p++] = (byte) (n >>> 24);
        out[p++] = (byte) (n >>> 16);
        out[p++] = (byte) (n >>> 8);
        out[p++] = (byte) n;
        for (int s = 0; s < 256; s++) {
            out[p++] = (byte) (freq[s] >>> 8);
            out[p++] = (byte) freq[s];
        }
        for (int shift = 56; shift >= 0; shift -= 8) {
            out[p++] = (byte) (x >>> shift);
        }
        for (Byte b : renorm) {
            out[p++] = b;
        }
        return out;
    }

    /**
     * 解码（encode 的精确逆；短流/表损坏 fail-fast）。
     *
     * @throws IllegalArgumentException null/短于头/频率表非归一
     */
    public static byte[] decode(byte[] stream) {
        if (stream == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        if (stream.length == 0) {
            return new byte[0];
        }
        if (stream.length < 4 + 512 + 8) {
            throw new IllegalArgumentException("短于头（" + stream.length + "）");
        }
        int n = ((stream[0] & 0xFF) << 24) | ((stream[1] & 0xFF) << 16)
                | ((stream[2] & 0xFF) << 8) | (stream[3] & 0xFF);
        int[] freq = new int[256];
        int[] cum = new int[256];
        int sum = 0;
        for (int s = 0; s < 256; s++) {
            freq[s] = ((stream[4 + s * 2] & 0xFF) << 8) | (stream[5 + s * 2] & 0xFF);
            cum[s] = sum;
            sum += freq[s];
        }
        if (sum != M) {
            throw new IllegalArgumentException("频率表非归一（" + sum + "≠" + M + "）");
        }
        long x = 0;
        for (int k = 0; k < 8; k++) {
            x = (x << 8) | (stream[516 + k] & 0xFFL);
        }
        int cursor = stream.length;
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) {
            int slot = (int) (x & (M - 1));
            int s = symbolFor(cum, freq, slot);
            out[i] = (byte) s;
            x = (long) freq[s] * (x >> M_BITS) + slot - cum[s];
            while (x < STATE_FLOOR) {
                cursor--;
                if (cursor < 524) {
                    throw new IllegalArgumentException("重整字节不足（第 " + i + " 符号）");
                }
                x = (x << 8) | (stream[cursor] & 0xFFL);
            }
        }
        return out;
    }

    private static int symbolFor(int[] cum, int[] freq, int slot) {
        for (int s = 0; s < 256; s++) {
            if (freq[s] > 0 && slot >= cum[s] && slot < cum[s] + freq[s]) {
                return s;
            }
        }
        throw new IllegalArgumentException("槽位无主（" + slot + "）——频率表损坏");
    }

    /** 归一化：max(1, c·M/n) 起步，逐次 argmax（并列取最低编号）增减逼近恰 M——永不下穿 1。 */
    private static int[] normalizedFrequencies(byte[] data) {
        int n = data.length;
        int[] counts = new int[256];
        for (byte b : data) {
            counts[b & 0xFF]++;
        }
        int[] freq = new int[256];
        long sum = 0;
        for (int s = 0; s < 256; s++) {
            freq[s] = Math.max(1, (int) ((long) counts[s] * M / n));
            sum += freq[s];
        }
        // 截断损失+缺席填充的差额可能超出任意单项——逐次 argmax 增减（每步恰 1，确定收敛）
        while (sum != M) {
            int pivot = 0;
            for (int s = 1; s < 256; s++) {
                if (freq[s] > freq[pivot]) {
                    pivot = s;
                }
            }
            if (sum < M) {
                freq[pivot]++;
                sum++;
            } else if (freq[pivot] > 1) {
                freq[pivot]--;
                sum--;
            } else {
                // 理论不可达（Σf>M ⟹ 存在 f>1），防御性失败优于负频率静默
                throw new IllegalStateException("归一化不可达（sum=" + sum + "）");
            }
        }
        return freq;
    }

    private static int[] cumulative(int[] freq) {
        int[] cum = new int[256];
        int sum = 0;
        for (int s = 0; s < 256; s++) {
            cum[s] = sum;
            sum += freq[s];
        }
        return cum;
    }
}
