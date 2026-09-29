package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.math.BigInteger;

/**
 * Base58 编码（spec 8016 / V8033 / impl 2318）——
 * Bitcoin/IPFS/Flickr 思想：**58 字符无歧义字母表 + 大整数
 * 进制转换**——0OIl 剔除（0/O、l/I 手抄歧义根除）、+/& 不入表
 * （URL/双击选中友好）——Base64 易混字形与符号字符的病解。
 * 前导 0x00 字节 → '1' 字符（Bitcoin 经典约定）；decode '1'
 * 计数还原前导零；非法字符携带位置 fail-fast；null fail-fast；
 * 确定性纯函数（同字节同编码）。
 *
 * <p>与 Base32（既有面）同族不同面：无歧义转录表 vs 大小写
 * 不敏感表。
 */
public final class Base58Codec {

    /** Bitcoin 字母表序（明示非 Flickr 序）。 */
    private static final char[] ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

    private static final int BASE = ALPHABET.length;
    private static final BigInteger BIG_BASE = BigInteger.valueOf(BASE);
    private static final char ZERO_CHAR = '1';

    private Base58Codec() {
    }

    /** 编码（前导 0x00 → '1'；null fail-fast）。 */
    public static String encode(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("数据非空引用");
        }
        int zeros = 0;
        while (zeros < data.length && data[zeros] == 0) {
            zeros++;
        }
        BigInteger value = new BigInteger(1, data);
        StringBuilder sb = new StringBuilder();
        while (value.signum() > 0) {
            BigInteger[] divMod = value.divideAndRemainder(BIG_BASE);
            sb.append(ALPHABET[divMod[1].intValue()]);
            value = divMod[0];
        }
        for (int i = 0; i < zeros; i++) {
            sb.append(ZERO_CHAR);
        }
        return sb.reverse().toString();
    }

    /** 解码（非法字符携带位置 fail-fast；null fail-fast）。 */
    public static byte[] decode(String encoded) {
        if (encoded == null) {
            throw new IllegalArgumentException("编码串非空引用");
        }
        int zeros = 0;
        while (zeros < encoded.length() && encoded.charAt(zeros) == ZERO_CHAR) {
            zeros++;
        }
        BigInteger value = BigInteger.ZERO;
        for (int i = zeros; i < encoded.length(); i++) {
            int digit = indexOf(encoded.charAt(i));
            if (digit < 0) {
                throw new IllegalArgumentException(
                        "非法 Base58 字符 '" + encoded.charAt(i) + "'（位置 " + i + "）");
            }
            value = value.multiply(BIG_BASE).add(BigInteger.valueOf(digit));
        }
        if (value.signum() == 0) {
            return new byte[zeros];
        }
        byte[] raw = value.toByteArray();
        int strip = raw.length > 1 && raw[0] == 0 ? 1 : 0;
        byte[] out = new byte[zeros + raw.length - strip];
        System.arraycopy(raw, strip, out, zeros, raw.length - strip);
        return out;
    }

    private static int indexOf(char c) {
        for (int i = 0; i < ALPHABET.length; i++) {
            if (ALPHABET[i] == c) {
                return i;
            }
        }
        return -1;
    }
}
