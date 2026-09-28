package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

/**
 * Soundex 语音编码（spec 7018 / U7237 / impl 2270）——
 * 美国 1880 年人口普查检索经典（NARA 规范）：**辅音按
 * 发音部位映射数字、首字母保留、相邻同码折叠（元音断开、
 * H/W 不断开）、补零截断至 1 字母+3 数字**——拼写不同发音
 * 相近的名字（Robert/Rupert）归并为同码——精确拼写匹配
 * 搜不到同音异形变体的病解。非字母丢弃、小写归一、H/W
 * 不重置前码（Ashcraft→A261 经典钉子）。全规则确定性
 * 纯函数。
 *
 * <p>与 KmpSearch（同包）同族不同面：精确子串匹配 vs
 * 发音近似归并。
 */
public final class Soundex {

    private Soundex() {
    }

    /** Soundex 码（1 字母+3 数字；null fail-fast；非字母丢弃）。 */
    public static String soundex(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("名字非空");
        }
        String upper = name.toUpperCase(java.util.Locale.ROOT);
        StringBuilder letters = new StringBuilder();
        for (char c : upper.toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                letters.append(c);
            }
        }
        if (letters.length() == 0) {
            throw new IllegalArgumentException("无字母可编码: " + name);
        }
        StringBuilder out = new StringBuilder();
        out.append(letters.charAt(0));
        int prevDigit = digitOf(letters.charAt(0));
        for (int i = 1; i < letters.length(); i++) {
            char c = letters.charAt(i);
            int digit = digitOf(c);
            if (digit == 0) {
                if (!isHw(c)) {
                    prevDigit = 0;
                }
                continue;
            }
            if (digit != prevDigit) {
                out.append((char) ('0' + digit));
                if (out.length() == 4) {
                    return out.toString();
                }
            }
            prevDigit = digit;
        }
        while (out.length() < 4) {
            out.append('0');
        }
        return out.toString();
    }

    /** 辅音发音部位映射（元音/HW 为 0——断开或跳过面）。 */
    private static int digitOf(char c) {
        switch (c) {
            case 'B': case 'F': case 'P': case 'V':
                return 1;
            case 'C': case 'G': case 'J': case 'K':
            case 'Q': case 'S': case 'X': case 'Z':
                return 2;
            case 'D': case 'T':
                return 3;
            case 'L':
                return 4;
            case 'M': case 'N':
                return 5;
            case 'R':
                return 6;
            default:
                return 0;
        }
    }

    private static boolean isHw(char c) {
        return c == 'H' || c == 'W';
    }
}
