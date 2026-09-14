package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 指标命名校验器（spec 1431 / T2165 / impl 1085）——Prometheus metric
 * naming 规范思想（命名规则机器化，命名漂移在写入时显形而非看板对不上）：
 * 与 MetricNamingGuardTest 的 starter 门规则同源（^[a-z][a-z0-9-]*(\.[a-z][a-z0-9-]*)+$，
 * buzhou 前缀族），把规则抽成公共纯函数供机制作者自查与运行期 debug。
 *
 * <p>纯函数零状态：validate(name) → 违规原因清单（空 = 合规）。违规类别
 * 闭集：EMPTY/WHITESPACE/PREFIX/SEGMENT_EMPTY/SEGMENT_CASE/SEGMENT_CHARS——
 * 逐条检测（首违不短路，一次看全所有问题）。安全前缀
 * {@link #SAFE_PREFIX}="buzhou" 之外的命名判 PREFIX 违规（仓内指标族纪律）。
 */
public final class MetricNameAudit {

    /** 命名安全前缀（仓内指标族前缀）。 */
    public static final String SAFE_PREFIX = "buzhou";

    /** 与 MetricNamingGuardTest 同源的段规则：小写字母开头，段内小写/数字/连字符。 */
    static final Pattern SEGMENT_RULE = Pattern.compile("^[a-z][a-z0-9-]*$");

    private MetricNameAudit() {
    }

    /**
     * @param name     被检指标名
     * @param violations 违规原因清单（空 = 合规；首违不短路——一次看全）
     */
    public record NameVerdict(String name, List<String> violations) {

        public boolean compliant() {
            return violations.isEmpty();
        }
    }

    /** 校验入口：单指标名全量违规检测。 */
    public static NameVerdict validate(String name) {
        List<String> violations = new ArrayList<>();
        if (name == null || name.isBlank()) {
            violations.add("EMPTY");
            return new NameVerdict(name, List.copyOf(violations));
        }
        if (!name.equals(name.trim())) {
            violations.add("WHITESPACE");
        }
        if (!name.startsWith(SAFE_PREFIX + ".")) {
            violations.add("PREFIX");
        }
        for (String segment : name.split("\\.")) {
            if (segment.isEmpty()) {
                violations.add("SEGMENT_EMPTY");
                continue;
            }
            if (!SEGMENT_RULE.matcher(segment).matches()) {
                if (Character.isUpperCase(segment.charAt(0))) {
                    violations.add("SEGMENT_CASE:" + segment);
                } else {
                    violations.add("SEGMENT_CHARS:" + segment);
                }
            }
        }
        return new NameVerdict(name, List.copyOf(violations));
    }
}
