package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 目录 lint 严重度分级（spec 821 / T1143，rust-clippy 分级体系借鉴——
 * correctness/suspicious/style 三档语义）：对 {@link ToolCatalogLinter}
 * 发现做严重度归类与排序——DUPLICATE_NAME = DENY（同目录冲突直接破坏分发）、
 * NAME_CONVENTION = WARN（可解析但反直觉）、DESCRIPTION_LENGTH = HINT
 * （提示质量）。默认映射可被定制覆盖（新规则随轮登记）。
 *
 * <p>纯函数：分级只读不改 lint 行为（阻断与否归调用方——按 denyCount 决策）。
 */
public final class LintSeverityGrader {

    /** 严重度（rank 越小越严重）。 */
    public enum Severity {
        /** 破坏性——应阻断（clippy correctness）。 */
        DENY(0),
        /** 可疑——应关注（clippy suspicious）。 */
        WARN(1),
        /** 提示——可忽略（clippy style）。 */
        HINT(2);

        private final int rank;

        Severity(int rank) {
            this.rank = rank;
        }

        int rank() {
            return rank;
        }
    }

    /** 单条分级发现。 */
    public record GradedFinding(ToolCatalogLinter.Finding finding, Severity severity) {
    }

    /** 分级报告（严重→轻排序 + 三档计数）。 */
    public record GradeReport(List<GradedFinding> graded, int denyCount, int warnCount, int hintCount) {
    }

    private final Map<String, Severity> rules;

    /** 默认分级（三内置规则）。 */
    public LintSeverityGrader() {
        Map<String, Severity> m = new HashMap<>();
        m.put(ToolCatalogLinter.RULE_DUP, Severity.DENY);
        m.put(ToolCatalogLinter.RULE_NAME, Severity.WARN);
        m.put(ToolCatalogLinter.RULE_DESC, Severity.HINT);
        this.rules = Map.copyOf(m);
    }

    private LintSeverityGrader(Map<String, Severity> rules) {
        this.rules = Map.copyOf(rules);
    }

    /** 定制规则分级（返回新实例——不可变风格）。 */
    public LintSeverityGrader withRule(String rule, Severity severity) {
        if (rule == null || rule.isBlank() || severity == null) {
            return this;
        }
        Map<String, Severity> next = new HashMap<>(rules);
        next.put(rule, severity);
        return new LintSeverityGrader(next);
    }

    /** 分级：未知规则归 HINT（不猜测严重性）；同严重度按 (tool, rule) 典序。 */
    public GradeReport grade(List<ToolCatalogLinter.Finding> findings) {
        List<GradedFinding> graded = new ArrayList<>();
        int deny = 0;
        int warn = 0;
        int hint = 0;
        for (ToolCatalogLinter.Finding f : findings) {
            if (f == null) {
                continue;
            }
            Severity severity = rules.getOrDefault(f.rule(), Severity.HINT);
            graded.add(new GradedFinding(f, severity));
            switch (severity) {
                case DENY -> deny++;
                case WARN -> warn++;
                default -> hint++;
            }
        }
        graded.sort(Comparator.comparingInt((GradedFinding g) -> g.severity().rank())
                .thenComparing(g -> g.finding().tool() == null ? "" : g.finding().tool())
                .thenComparing(g -> g.finding().rule() == null ? "" : g.finding().rule()));
        return new GradeReport(List.copyOf(graded), deny, warn, hint);
    }
}
