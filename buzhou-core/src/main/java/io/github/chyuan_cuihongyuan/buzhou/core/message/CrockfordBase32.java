package io.github.chyuan_cuihongyuan.buzhou.core.message;

/**
 * Crockford Base32（spec 4009 / T6019 / impl 2110）——人类可转录
 * 整数编码思想（Douglas Crockford 2001；ULID 底层字母表同源）：
 * 32 符号字母表剔除 I/L/O/U（形近 I↔1、L↔1、O↔0、U 不雅词），
 * 解码侧**宽容归一**（I/L→1、O→0、大小写不敏感、连字符忽略）——
 * 电话/工单口传 ID 的「念错写错」病从字母表根除。
 *
 * <p>可选 mod-37 校验符号（encodeWithCheck/decodeWithCheck）：
 * 37 素数 > 32，单字符检错（转写错一位必被捕获）。纯静态。
 */
public final class CrockfordBase32 {

    /** 主字母表（32 符号——无 I/L/O/U）。 */
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    /** 校验符号表（主 32 + * ~ $ = U——mod 37 余数 32–36）。 */
    private static final char[] CHECK_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ*~$=U".toCharArray();

    private CrockfordBase32() {
    }

    /** 编码（值 ≥0；最小位数——无前导零）。 */
    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value ≥0：" + value);
        }
        if (value == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder(13);
        long v = value;
        while (v > 0) {
            sb.append(ALPHABET[(int) (v & 31)]);
            v >>>= 5;
        }
        return sb.reverse().toString();
    }

    /** 解码（I/L→1、O→0 归一；大小写不敏感；连字符忽略；非法符/溢出 fail-fast）。 */
    public static long decode(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("text 非空");
        }
        long value = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toUpperCase(text.charAt(i));
            if (c == '-') {
                continue;   // 分隔符忽略
            }
            int digit = digitOf(c);
            if (value > (Long.MAX_VALUE - digit) / 32) {
                throw new IllegalArgumentException("溢出：" + text);
            }
            value = value * 32 + digit;
        }
        return value;
    }

    /** 带校验符号编码（尾附 mod-37 检错符）。 */
    public static String encodeWithCheck(long value) {
        return encode(value) + CHECK_ALPHABET[(int) (value % 37)];
    }

    /** 带校验符号解码（尾符不匹配 fail-fast——单字符转写错必被捕获）。 */
    public static long decodeWithCheck(String text) {
        if (text == null || text.length() < 2) {
            throw new IllegalArgumentException("text 至少数值符+校验符两位");
        }
        String body = text.substring(0, text.length() - 1);
        char check = Character.toUpperCase(text.charAt(text.length() - 1));
        long value = decode(body);
        char expected = CHECK_ALPHABET[(int) (value % 37)];
        if (check != expected) {
            throw new IllegalArgumentException("校验符不符：" + check + " 期望 " + expected);
        }
        return value;
    }

    /** 符号→值（O→0、I/L→1 形近归一；U 及校验符外字符非法）。 */
    private static int digitOf(char c) {
        return switch (c) {
            case '0', 'O' -> 0;
            case '1', 'I', 'L' -> 1;
            case '2' -> 2;
            case '3' -> 3;
            case '4' -> 4;
            case '5' -> 5;
            case '6' -> 6;
            case '7' -> 7;
            case '8' -> 8;
            case '9' -> 9;
            case 'A' -> 10;
            case 'B' -> 11;
            case 'C' -> 12;
            case 'D' -> 13;
            case 'E' -> 14;
            case 'F' -> 15;
            case 'G' -> 16;
            case 'H' -> 17;
            case 'J' -> 18;
            case 'K' -> 19;
            case 'M' -> 20;
            case 'N' -> 21;
            case 'P' -> 22;
            case 'Q' -> 23;
            case 'R' -> 24;
            case 'S' -> 25;
            case 'T' -> 26;
            case 'V' -> 27;
            case 'W' -> 28;
            case 'X' -> 29;
            case 'Y' -> 30;
            case 'Z' -> 31;
            default -> throw new IllegalArgumentException("非法符号：" + c);
        };
    }
}
