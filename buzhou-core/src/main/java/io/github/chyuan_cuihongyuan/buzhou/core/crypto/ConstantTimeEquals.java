package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import java.util.Arrays;

/**
 * 常数时间比较（spec 8037 / V8075 / impl 2339）——
 * timing attack 防御思想（DJB/OAuth 同源）：**diff 全长度
 * XOR 累积、无短路**——比较耗时只随长度不随内容——短路
 * ==（泄漏前缀匹配长度、时序侧信道逐字节爆破）的病解。
 * 数组长度差返回 false（长度非秘密——OAuth 惯例明示）；
 * null fail-fast；确定性。
 *
 * <p>与 SipHash24（spec 8015）同族不同面：比较面 vs 哈希面。
 */
public final class ConstantTimeEquals {

    private ConstantTimeEquals() {
    }

    /** 常数时间字节比较（null fail-fast；长度不同 false——长度面明示不保护）。 */
    public static boolean equals(byte[] first, byte[] second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("比较串非空引用");
        }
        int diff = first.length ^ second.length;
        if (first.length > 0 && second.length > 0) {
            int length = Math.max(first.length, second.length);
            for (int i = 0; i < length; i++) {
                diff |= first[i % first.length] ^ second[i % second.length];
            }
        }
        return diff == 0;
    }

    /** 十六进制字符串常数时间比较（大小写归一）。 */
    public static boolean equalsHex(String first, String second) {
        if (first == null || second == null) {
            throw new IllegalArgumentException("比较串非空引用");
        }
        return equals(hexBytes(first), hexBytes(second));
    }

    private static byte[] hexBytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i + 1 < hex.length(); i += 2) {
            out[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return out;
    }

    /** 语义对照面（非常数时间——仅测试对照用）。 */
    static boolean referenceEquals(byte[] first, byte[] second) {
        return Arrays.equals(first, second);
    }
}
