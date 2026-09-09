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

    private final Set<SecretType> enabledTypes;

    public SecretScanner() {
        this(EnumSet.allOf(SecretType.class));
    }

    public SecretScanner(Set<SecretType> enabledTypes) {
        this.enabledTypes = EnumSet.copyOf(enabledTypes == null || enabledTypes.isEmpty()
                ? EnumSet.allOf(SecretType.class) : enabledTypes);
    }

    /** 扫描命中（类型在启用集内的）。 */
    public List<SecretMatch> scan(String text) {
        List<SecretMatch> matches = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return matches;
        }
        for (SecretType type : SecretType.values()) {
            if (!enabledTypes.contains(type)) {
                continue;
            }
            Matcher m = type.pattern().matcher(text);
            while (m.find()) {
                matches.add(new SecretMatch(type, m.start(), m.end()));
            }
        }
        matches.sort((a, b) -> Integer.compare(a.start(), b.start()));
        return matches;
    }

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
