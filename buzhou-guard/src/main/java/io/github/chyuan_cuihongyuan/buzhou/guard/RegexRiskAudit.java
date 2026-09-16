package io.github.chyuan_cuihongyuan.buzhou.guard;

import java.util.List;

/**
 * 正则风险审计（spec 1874 / T2949 / impl 1475）——ReDoS 防线启发式
 *（OWASP 正则 DoS / SafeRegex 惯例）：扫描模式串的已知回溯爆炸形态——
 * **嵌套量词**（(a+)+ 类：量词作用域内再套量词，指数回溯）与**重叠
 * 交替**（(a|ab)* 类：分支共享前缀，回溯分支翻倍）。分级三档：SAFE /
 * SUSPECT（单一风险形态）/ DANGEROUS（多形态叠加）。启发式非完备
 *（诚实边界：真完备要正则语法分析/ automata 构造——静态字符级扫描
 * 只拦已知形态）。
 *
 * <p>纯函数零状态；只审计不拦截（禁用决策归宿主）。
 */
public final class RegexRiskAudit {

    private RegexRiskAudit() {
    }

    /** 风险三档：SAFE / SUSPECT 单形态 / DANGEROUS 多形态叠加。 */
    public enum Risk {

        /** 未命中已知风险形态。 */
        SAFE,

        /** 命中单一风险形态——审查。 */
        SUSPECT,

        /** 多形态叠加——高危（回溯爆炸概率叠乘）。 */
        DANGEROUS
    }

    /** 审计结果：风险档 + 命中形态清单（人类可读）。 */
    public record Audit(String pattern, Risk risk, List<String> findings) {
    }

    /**
     * 审计入口。契约：pattern 非 null（fail-fast；空串 SAFE——空模式无
     * 回溯面）。
     */
    public static Audit audit(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern 不能为 null");
        }
        java.util.List<String> findings = new java.util.ArrayList<>();
        if (hasNestedQuantifier(pattern)) {
            findings.add("嵌套量词（量词作用域内再套量词——指数回溯形态）");
        }
        if (hasQuantifiedGroupWithAlternation(pattern)) {
            findings.add("量词组内交替（分支数×回溯路径翻倍形态）");
        }
        if (hasOverlappingAlternation(pattern)) {
            findings.add("重叠交替（分支共享前缀——回溯分支翻倍形态）");
        }
        Risk risk = findings.size() >= 2 ? Risk.DANGEROUS
                : findings.size() == 1 ? Risk.SUSPECT
                : Risk.SAFE;
        return new Audit(pattern, risk, List.copyOf(findings));
    }

    /**
     * 嵌套量词：存在 `+`/`*`/`}` 量词后紧跟 `)` 且该组以量词开括号承接——
     * 简化字符级判定：`+)`/`*)`/`})` 之后又紧跟量词字符。
     */
    private static boolean hasNestedQuantifier(String p) {
        for (int i = 1; i < p.length() - 1; i++) {
            char c = p.charAt(i);
            if (c == ')' && isQuantifier(p.charAt(i + 1))) {
                // 组内以量词结尾（倒数第二字符是量词或 `}` 结尾区间量词）
                char prev = p.charAt(i - 1);
                if (isQuantifier(prev) || prev == '}') {
                    return true;
                }
            }
        }
        return false;
    }

    /** 量词组含交替：(…|…) 后接量词。 */
    private static boolean hasQuantifiedGroupWithAlternation(String p) {
        for (int i = 1; i < p.length() - 1; i++) {
            if (p.charAt(i) == ')' && isQuantifier(p.charAt(i + 1))) {
                int open = p.lastIndexOf('(', i);
                if (open >= 0 && p.substring(open, i).contains("|")) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 重叠交替：同组内某分支是另一分支的前缀（(a|ab) 形态——字符级简化）。 */
    private static boolean hasOverlappingAlternation(String p) {
        int depth = 0;
        int groupStart = -1;
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c == '\\' && i + 1 < p.length()) {
                i++; // 转义跳过
                continue;
            }
            if (c == '(') {
                depth++;
                groupStart = i;
            } else if (c == ')') {
                depth--;
            } else if (c == '|' && depth == 1 && groupStart >= 0) {
                // 组内首个 |：检查两侧分支首字符相同（简化——(a|ab) 类）
                String body = p.substring(groupStart + 1, i);
                String rest = p.substring(i + 1, groupEnd(p, i));
                if (!body.isEmpty() && !rest.isEmpty()
                        && body.charAt(body.length() - 1) == rest.charAt(0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int groupEnd(String p, int from) {
        for (int i = from; i < p.length(); i++) {
            if (p.charAt(i) == ')') {
                return i;
            }
        }
        return p.length();
    }

    private static boolean isQuantifier(char c) {
        return c == '+' || c == '*' || c == '?';
    }
}
