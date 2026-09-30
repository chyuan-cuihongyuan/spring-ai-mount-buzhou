package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.HashMap;
import java.util.Map;

/**
 * 莫尔斯编解码（spec 9022 / W9045 / impl 2375）——International
 * Morse 1838/ITU-R M.1677 思想（电报时代定长字符→变长点划信令
 * ——SOS 求救信令同源）：**A–Z/0–9 → 点划序列（字母单空格分隔、
 * 单词 " / " 分隔）**——可读性优先的信号编码（与位级压缩族互补
 * 的「人眼可校」形态）。大小写不敏感编码、大写输出；未知字符/
 * 非法点划 token/连续分隔符 fail-fast；同文同码完全确定。
 *
 * <p>与 Base58Codec（spec 8017）同包不同面：可见字母表编码 vs
 * 点划信号；与 HuffmanCodec（同包）不同面：频率最优前缀码 vs
 * 固定码表信令。
 */
public final class MorseCodec {

    private static final Map<Character, String> CODES = new HashMap<>();
    private static final Map<String, Character> DECODE = new HashMap<>();

    static {
        String[] letters = {
                ".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....", "..", ".---",
                "-.-", ".-..", "--", "-.", "---", ".--.", "--.-", ".-.", "...", "-",
                "..-", "...-", ".--", "-..-", "-.--", "--.."
        };
        for (int i = 0; i < letters.length; i++) {
            register((char) ('A' + i), letters[i]);
        }
        String[] digits = {
                "-----", ".----", "..---", "...--", "....-", ".....",
                "-....", "--...", "---..", "----."
        };
        for (int i = 0; i < digits.length; i++) {
            register((char) ('0' + i), digits[i]);
        }
    }

    private static void register(char symbol, String code) {
        CODES.put(symbol, code);
        DECODE.put(code, symbol);
    }

    private MorseCodec() {
    }

    /**
     * 编码（大小写不敏感；字母单空格分隔、单词 " / " 分隔）。
     *
     * @throws IllegalArgumentException null/空文本、未知字符
     */
    public static String encode(String text) {
        if (text == null || text.isEmpty()) {
            throw new IllegalArgumentException("文本非空（编码面）");
        }
        StringBuilder out = new StringBuilder();
        boolean inWord = false;
        for (int i = 0; i < text.length(); i++) {
            char raw = text.charAt(i);
            if (raw == ' ') {
                if (!inWord) {
                    throw new IllegalArgumentException("连续分隔符（位 " + i + "）");
                }
                out.append(" /");
                inWord = false;
                continue;
            }
            char c = Character.toUpperCase(raw);
            String code = CODES.get(c);
            if (code == null) {
                throw new IllegalArgumentException("未知字符（'" + raw + "' 位 " + i + "）");
            }
            out.append(inWord ? ' ' : ' ').append(code);
            inWord = true;
        }
        if (!inWord) {
            throw new IllegalArgumentException("尾随分隔符（解码歧义拒绝）");
        }
        return out.substring(1);
    }

    /**
     * 解码（encode 的精确逆；非法 token fail-fast）。
     *
     * @throws IllegalArgumentException null/空码、非法点划、连续分隔
     */
    public static String decode(String morse) {
        if (morse == null || morse.isEmpty()) {
            throw new IllegalArgumentException("码非空（解码面）");
        }
        StringBuilder out = new StringBuilder();
        boolean pendingWordBreak = false;
        for (String token : morse.split(" ", -1)) {
            if (token.isEmpty()) {
                throw new IllegalArgumentException("连续分隔符（编码面不会产出）");
            }
            if (token.equals("/")) {
                if (pendingWordBreak) {
                    throw new IllegalArgumentException("连续词分隔（编码面不会产出）");
                }
                pendingWordBreak = true;
                continue;
            }
            Character symbol = DECODE.get(token);
            if (symbol == null) {
                throw new IllegalArgumentException("非法点划（\"" + token + "\"）");
            }
            if (pendingWordBreak) {
                out.append(' ');
                pendingWordBreak = false;
            }
            out.append(symbol);
        }
        if (pendingWordBreak) {
            throw new IllegalArgumentException("尾随词分隔（编码面不会产出）");
        }
        return out.toString();
    }
}
