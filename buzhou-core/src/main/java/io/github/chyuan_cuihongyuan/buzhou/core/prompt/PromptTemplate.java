package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词模板严格渲染（spec 512 / T775，Jinja2 StrictUndefined 借鉴——
 * 401 注册表扩散）：{{name}} 占位符抽取 + 严格渲染（缺失变量一次列全
 * ——杜绝 {@code {{name}}} 原样漏进 prompt 的静默失败）+ 预检面。
 *
 * <p>诚实边界：只做单层变量替换（不做过滤器/循环/条件——提示词不是
 * 通用模板引擎）；非占位符文本原样保留。与 {@link PromptRegistry} 组合：
 * {@code render(registry.resolve(name).orElseThrow().body(), vars)}。
 */
public final class PromptTemplate {

    /** 占位符形态：{{ name }}（容忍两侧空白；name = 字母/下划线开头标识符）。 */
    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*}}");
    /** 未闭合占位符（{{ 后无 }}——语法错检测）。 */
    private static final Pattern UNCLOSED = Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*)?$");

    private PromptTemplate() {
    }

    /** 抽取模板变量（去重保序——首次出现序）。 */
    public static List<String> variables(String template) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER.matcher(requireTemplate(template));
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return List.copyOf(names);
    }

    /**
     * 严格渲染（StrictUndefined 语义）：任一变量缺失 → IllegalStateException
     * <b>一次列全</b>缺失名单；多余变量忽略；未闭合占位符 → 语法错带位置。
     */
    public static String render(String template, Map<String, ?> variables) {
        requireTemplate(template);
        List<String> missing = validate(template,
                variables == null ? Set.of() : variables.keySet()).missing();
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "提示词模板变量缺失（StrictUndefined）：" + missing + "——请补全变量"
                            + "或修正模板占位符");
        }
        if (hasUnclosed(template) != -1) {
            throw syntaxError(template, hasUnclosed(template));
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            Object value = variables == null ? null : variables.get(name);
            matcher.appendReplacement(out,
                    java.util.regex.Matcher.quoteReplacement(String.valueOf(value)));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** 预检（装配期/评测期零成本体检）：missing = 模板需要而 provided 未含的变量。 */
    public static ValidationResult validate(String template, java.util.Collection<String> provided) {
        requireTemplate(template);
        java.util.Collection<String> have = provided == null ? List.of() : provided;
        List<String> missing = variables(template).stream()
                .filter(name -> !have.contains(name)).toList();
        return new ValidationResult(missing.isEmpty(), missing);
    }

    /** 预检结果（ok + 缺失名单）。 */
    public record ValidationResult(boolean ok, List<String> missing) {
    }

    private static String requireTemplate(String template) {
        if (template == null) {
            throw new IllegalArgumentException("模板正文非空");
        }
        return template;
    }

    /** 未闭合占位符位置（{{ 无配对 }} 返回其下标；无则 -1）。 */
    private static int hasUnclosed(String template) {
        String stripped = PLACEHOLDER.matcher(template).replaceAll("");
        int idx = stripped.indexOf("{{");
        if (idx < 0) {
            return -1;
        }
        return stripped.indexOf("}}", idx) < 0 ? idx : -1;
    }

    private static IllegalStateException syntaxError(String template, int position) {
        return new IllegalStateException("提示词模板占位符未闭合（{{ 无匹配 }}）——位置 "
                + position + " 附近");
    }
}
