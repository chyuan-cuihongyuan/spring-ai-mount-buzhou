package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 自定义 PII 规则（spec 118 §A / T417，spec 86 fog「自定义 recognizer」最小版；
 * Presidio PatternRecognizer 对应物）：宿主声明命名正则（如订单号/工号/内部
 * token），命中替换 {@code [PII:<NAME>]}——与内置五型同占位符形态。
 *
 * <p>纪律：NAME 须 {@code [A-Z0-9_]{2,32}}（占位符形态稳定、有界——不校验则
 * fail-fast IllegalArgumentException）；正则由宿主自负（ReDoS 风险归声明方——
 * javadoc 显性告知）。
 */
public final class CustomPiiRules {

    /** 单条自定义规则（NAME 大写下划线/数字；pattern 命中即整段替换）。 */
    public record Rule(String name, Pattern pattern) {

        public Rule {
            if (name == null || !name.matches("[A-Z0-9_]{2,32}")) {
                throw new IllegalArgumentException(
                        "自定义 PII 规则名非法（须 [A-Z0-9_]{2,32}）：" + name);
            }
            if (pattern == null) {
                throw new IllegalArgumentException("自定义 PII 规则正则为空：" + name);
            }
        }

        /** 便捷工厂（pattern 字符串形态）。 */
        public static Rule of(String name, String regex) {
            return new Rule(name, Pattern.compile(regex));
        }
    }

    private final List<Rule> rules;

    public CustomPiiRules(List<Rule> rules) {
        this.rules = List.copyOf(rules == null ? List.of() : rules);
    }

    public boolean isEmpty() {
        return rules.isEmpty();
    }

    /** 规则清单只读视图（spec 743 / T1086：命中计数归因用）。 */
    public List<Rule> rules() {
        return rules;
    }

    /** 应用全部自定义规则（不做占位符短路——叠加场景：内置结果之上继续脱自定义； 幂等由规则特异性保证）。 */
    public String redact(String text) {
        if (text == null || text.isEmpty() || rules.isEmpty()) {
            return text;
        }
        String out = text;
        for (Rule rule : rules) {
            out = rule.pattern().matcher(out)
                    .replaceAll("[PII:" + rule.name() + "]");
        }
        return out;
    }
}
