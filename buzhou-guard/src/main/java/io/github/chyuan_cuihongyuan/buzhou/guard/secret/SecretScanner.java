package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * 密钥扫描器（spec 400 / T691，gitleaks 借鉴）：对文本扫 7 型凭据签名，
 * 命中替换 {@code [SECRET:TYPE]} 占位符。幂等：已含占位符前缀的内容不再
 * 处理（占位符本身无密钥）；无命中零改写（引用等——与 PiiDetector 同口径）。
 */
public final class SecretScanner {

    /** 命中（类型 + 原文区间）。 */
    public record SecretMatch(SecretType type, int start, int end) {
    }

    static final String PLACEHOLDER_PREFIX = "[SECRET:";

    /** 默认熵阈值（bits/char）——真随机键 4.5+，AWS 文档示例 ≈3.7 恰被滤（spec 714）。 */
    public static final double DEFAULT_MIN_ENTROPY = 4.0;

    private final Set<SecretType> enabledTypes;
    private final Double minEntropy;

    public SecretScanner() {
        this(EnumSet.allOf(SecretType.class));
    }

    public SecretScanner(Set<SecretType> enabledTypes) {
        this(enabledTypes, null);
    }

    /**
     * spec 714 / T979（truffleHog entropy 借鉴）：+minEntropy——命中文本 Shannon
     * 熵（bits/char）低于阈值的命中丢弃（占位/示例非真密钥）；null/≤0 = 关
     * （默认零变化）。PRIVATE_KEY_BLOCK 豁免（BEGIN 行字面签名非熵信号）。
     */
    public SecretScanner(Set<SecretType> enabledTypes, Double minEntropy) {
        this.enabledTypes = EnumSet.copyOf(enabledTypes == null || enabledTypes.isEmpty()
                ? EnumSet.allOf(SecretType.class) : enabledTypes);
        this.minEntropy = minEntropy;
    }

    /** 扫描命中（类型在启用集内的；熵过滤开启时低熵命中丢弃）。 */
    public List<SecretMatch> scan(String text) {
        List<SecretMatch> matches = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return matches;
        }
        boolean entropyGate = minEntropy != null && minEntropy > 0;
        for (SecretType type : SecretType.values()) {
            if (!enabledTypes.contains(type)) {
                continue;
            }
            Matcher m = type.pattern().matcher(text);
            while (m.find()) {
                if (entropyGate && type != SecretType.PRIVATE_KEY_BLOCK
                        && shannonEntropy(m.group()) < minEntropy) {
                    continue; // 低熵——疑似占位/示例
                }
                matches.add(new SecretMatch(type, m.start(), m.end()));
            }
        }
        matches.sort((a, b) -> Integer.compare(a.start(), b.start()));
        return matches;
    }

    /** Shannon 熵（字符频率，bits/char——空串 0）。 */
    static double shannonEntropy(String s) {
        if (s == null || s.isEmpty()) {
            return 0.0;
        }
        int[] freq = new int[Character.MAX_VALUE + 1];
        for (int i = 0; i < s.length(); i++) {
            freq[s.charAt(i)]++;
        }
        double h = 0.0;
        for (int f : freq) {
            if (f > 0) {
                double p = (double) f / s.length();
                h -= p * (Math.log(p) / LOG2);
            }
        }
        return h;
    }

    private static final double LOG2 = Math.log(2);

    /** 命中替换占位符；无命中返回原引用。PRIVATE_KEY_BLOCK 只替换 BEGIN 行，
     * 块体本就非可打印密文形状（Base64 私钥体无签名——BEGIN 行标记即泄漏事实）。 */
    public String redact(String text) {
        if (text == null || text.isEmpty() || text.contains(PLACEHOLDER_PREFIX)) {
            return text; // 幂等：占位符已是脱敏产物
        }
        List<SecretMatch> matches = scan(text);
        if (matches.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        int cursor = 0;
        for (SecretMatch m : matches) {
            if (m.start() < cursor) {
                continue; // 区间重叠（同型多 Matcher 竞争时保先到）
            }
            sb.append(text, cursor, m.start());
            sb.append(PLACEHOLDER_PREFIX).append(m.type().name()).append(']');
            cursor = m.end();
        }
        sb.append(text.substring(cursor));
        return sb.toString();
    }
}
