package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LZW 字典压缩（spec 9012 / W9025 / impl 2365）——LZW 思想
 * （Ziv–Lempel 1978/Welch 1984——GIF/UNIX compress/PDF 同源）：
 * **自适应字典：256 单字节初始化，扫描中「最长已知前缀出码、
 * 前缀+下一字符入典」——字典与数据同构生长，码流自带词典无
 * 需随行**——逐字节熵编码（Huffman 需随行码表）与逐对引用
 * （LZ77 需回窗距离）的中间形态。encode 输出码字序列
 * （int 基 256 起）、decode 同构重建；纯确定性（同输入同码流
 * 完全确定）；null fail-fast；空输入空码流。
 *
 * <p>与 VarintCodec/HuffmanCodec（同包）不同面：定长数/熵前缀
 * vs 自适应字典；与 Base58Codec（spec 8017）不同面：可逆压缩
 * vs 可见编码。
 */
public final class LzwCodec {

    private LzwCodec() {
    }

    /**
     * 编码（码字序列；首码 256 起——0..255 保留单字节）。
     *
     * @throws IllegalArgumentException null 输入
     */
    public static List<Integer> encode(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        Map<List<Byte>, Integer> dictionary = new HashMap<>();
        for (int b = 0; b < 256; b++) {
            List<Byte> single = List.of((byte) b);
            dictionary.put(single, b);
        }
        int nextCode = 256;
        List<Integer> codes = new ArrayList<>();
        int i = 0;
        while (i < data.length) {
            int length = 1;
            Integer code = dictionary.get(List.of(data[i]));
            while (i + length < data.length) {
                List<Byte> candidate = new ArrayList<>(length + 1);
                for (int k = i; k <= i + length; k++) {
                    candidate.add(data[k]);
                }
                Integer extended = dictionary.get(candidate);
                if (extended == null) {
                    dictionary.put(candidate, nextCode++);
                    break;
                }
                code = extended;
                length++;
            }
            codes.add(code);
            i += length;
        }
        return codes;
    }

    /**
     * 解码（encode 的精确逆；码字 0..初始典界外或构造序列缺口 fail-fast）。
     *
     * @throws IllegalArgumentException null/非法码流
     */
    public static byte[] decode(List<Integer> codes) {
        if (codes == null) {
            throw new IllegalArgumentException("输入非空引用");
        }
        if (codes.isEmpty()) {
            return new byte[0];
        }
        Map<Integer, byte[]> dictionary = new HashMap<>();
        for (int b = 0; b < 256; b++) {
            dictionary.put(b, new byte[]{(byte) b});
        }
        int nextCode = 256;
        byte[] previous = dictionary.get(codes.get(0));
        if (previous == null) {
            throw new IllegalArgumentException("首码非法（" + codes.get(0) + "）");
        }
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.writeBytes(previous);
        for (int i = 1; i < codes.size(); i++) {
            int code = codes.get(i);
            byte[] entry;
            if (code == nextCode) {
                // KwKwK 特例：码=即将入典序列（前缀+前缀首字节）
                entry = concat(previous, new byte[]{previous[0]});
            } else {
                entry = dictionary.get(code);
                if (entry == null) {
                    throw new IllegalArgumentException("码字缺口（" + code + " > " + (nextCode - 1) + "）");
                }
            }
            out.writeBytes(entry);
            dictionary.put(nextCode++, concat(previous, new byte[]{entry[0]}));
            previous = entry;
        }
        return out.toByteArray();
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}
