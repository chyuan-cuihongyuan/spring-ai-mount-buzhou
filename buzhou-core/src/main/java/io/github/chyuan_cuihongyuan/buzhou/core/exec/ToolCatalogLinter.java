package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * 工具目录 lint（spec 420 / T731，ESLint 构建期 lint 借鉴——只报不改）：
 * 装配期一遍扫全部工具定义，三条有界规则——NAME_CONVENTION
 * （^[a-z][a-z0-9_]{0,63}$，Spring AI/MCP 惯例）/ DESCRIPTION_LENGTH
 * （<10 过短、>4096 过长）/ DUPLICATE_NAME（同名不同实例）。发现 = WARN +
 * 计数（tag rule 3 值有界）+ 会话事件（每会话首装配一次去重）。不拦截
 * 不改写（lint 非门禁——HITL/kill-switch 域）。
 */
public final class ToolCatalogLinter {

    /** 目录体检发现事件（tool/rule/detail；首装配一次）。 */
    public static final String EVENT_LINT = "tool.catalog.lint";

    /** 规则名（计数 tag 有界 3 值）。 */
    public static final String RULE_NAME = "NAME_CONVENTION";
    public static final String RULE_DESC = "DESCRIPTION_LENGTH";
    public static final String RULE_DUP = "DUPLICATE_NAME";

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");
    private static final int DESC_MIN = 10;
    private static final int DESC_MAX = 4096;

    /** 一条发现。 */
    public record Finding(String tool, String rule, String detail) {
    }

    private final Consumer<SessionEvent> emitter;
    private final Set<String> announcedSessions = ConcurrentHashMap.newKeySet();

    public ToolCatalogLinter(Consumer<SessionEvent> emitter) {
        this.emitter = emitter == null ? e -> { } : emitter;
    }

    /** 体检一遍（装配期逐工具调；重复名检测跨调用累积）。 */
    public List<Finding> lint(ToolCallback callback, List<ToolCallback> seenSoFar) {
        List<Finding> findings = new ArrayList<>();
        String name = callback.getToolDefinition().name();
        String description = callback.getToolDefinition().description() == null
                ? "" : callback.getToolDefinition().description();
        if (!NAME_PATTERN.matcher(name).matches()) {
            findings.add(new Finding(name, RULE_NAME,
                    "工具名应匹配 ^[a-z][a-z0-9_]{0,63}$（当前「" + name + "」——模型难引/跨端不一致）"));
        }
        if (description.length() < DESC_MIN) {
            findings.add(new Finding(name, RULE_DESC,
                    "描述过短（" + description.length() + " < " + DESC_MIN + "——模型无信息可选）"));
        } else if (description.length() > DESC_MAX) {
            findings.add(new Finding(name, RULE_DESC,
                    "描述过长（" + description.length() + " > " + DESC_MAX + "——挤占上下文)"));
        }
        for (ToolCallback seen : seenSoFar) {
            if (seen.getToolDefinition().name().equals(name) && seen != callback) {
                findings.add(new Finding(name, RULE_DUP,
                        "同名不同实现（跨源重名——模型引用歧义）"));
                break;
            }
        }
        for (Finding f : findings) {
            BuzhouMetricsHolder.metrics().counter("buzhou.tools.catalog-lint.findings",
                    "rule", f.rule());
        }
        return findings;
    }

    /** 装配完成口径：发现落日志 + 会话事件（每会话首装配一次去重）。 */
    public void announce(String sessionId, List<Finding> findings) {
        System.Logger logger = System.getLogger(ToolCatalogLinter.class.getName());
        for (Finding f : findings) {
            logger.log(System.Logger.Level.WARNING,
                    "工具目录 lint：[{0}] {1}：{2}", f.rule(), f.tool(), f.detail());
        }
        if (!findings.isEmpty() && sessionId != null
                && announcedSessions.add(sessionId)) {
            emitter.accept(SessionEvent.of(EVENT_LINT, Map.of(
                    "findings", findings.size(),
                    "rules", findings.stream().map(Finding::rule).distinct().toList())));
        }
    }
}
