package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Store 一致性校验报告（spec 29 / T108 / impl-83）：按检测项聚合 findings
 * （sessionId + 详情，样例上限 20/项），严重级 info（可解释残留）与 warn（疑似泄漏）。
 *
 * <p>检测项（v1）:{@value #ORPHAN_SUMMARY} / {@value #STATE_RESIDUE} /
 * {@value #DANGLING_LEASE} / {@value #DANGLING_OBSERVABILITY}——语义见
 * {@link StoreFsck}。
 */
public final class StoreIntegrityReport {

    /** 摘要存在但消息为空（孤儿摘要）。 */
    public static final String ORPHAN_SUMMARY = "orphan-summary";
    /** 三槽皆空但残留 state 键（残留状态）。 */
    public static final String STATE_RESIDUE = "state-residue";
    /** 租约存在但会话无消息（泄漏租约——占用 spawn 容量语义）。 */
    public static final String DANGLING_LEASE = "dangling-lease";
    /** 观测记录存在但会话数据全空（悬挂观测——只报不清，审计保留价值）。 */
    public static final String DANGLING_OBSERVABILITY = "dangling-observability";

    /** 单条发现。 */
    public record Finding(String sessionId, String check, Severity severity, String detail) {
    }

    public enum Severity {INFO, WARN}

    private final List<Finding> findings;

    StoreIntegrityReport(List<Finding> findings) {
        this.findings = List.copyOf(findings);
    }

    public List<Finding> findings() {
        return findings;
    }

    public boolean clean() {
        return findings.isEmpty();
    }

    /** 指定检测项的发现数。 */
    public long count(String check) {
        return findings.stream().filter(f -> f.check().equals(check)).count();
    }

    /** 按检测项计数总览。 */
    public Map<String, Long> countsByCheck() {
        return findings.stream().collect(Collectors.groupingBy(Finding::check, Collectors.counting()));
    }

    /** 指定检测项的样例（≤20）。 */
    public List<Finding> samples(String check) {
        return findings.stream().filter(f -> f.check().equals(check)).limit(20).toList();
    }

    /** 人读渲染（runbook 引用；计数 + 各项样例）。 */
    public String renderText() {
        StringBuilder sb = new StringBuilder("Store integrity report: ");
        if (findings.isEmpty()) {
            return sb.append("CLEAN（无发现）").toString();
        }
        sb.append(findings.size()).append(" finding(s)\n");
        countsByCheck().forEach((check, count) -> {
            sb.append("  ").append(check).append(": ").append(count).append('\n');
            samples(check).forEach(f -> sb.append("    - ").append(f.sessionId())
                    .append("（").append(f.severity()).append("）")
                    .append(f.detail() == null ? "" : " " + f.detail()).append('\n'));
        });
        return sb.toString();
    }
}
