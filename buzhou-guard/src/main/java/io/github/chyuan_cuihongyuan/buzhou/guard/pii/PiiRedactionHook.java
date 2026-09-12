package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 工具输出 PII 脱敏 hook（spec 86 §A / T329，Presidio 借鉴）：外部数据（工具/RAG
 * 返回）回灌上下文前，把命中类型替换为 {@code [PII:TYPE]} 占位符——PII 不进 prompt、
 * 不进观测、不进日志。order 70（先于 Spotlight 80——先脱敏原文再包裹标记段，纵深
 * 两层互不依赖）。幂等：已含占位符前缀的内容不再处理（占位符本身无 PII）。
 *
 * <p>计数器 {@code buzhou.guard.pii.redactions}（tag type——5 值有界枚举）。
 */
public class PiiRedactionHook implements BuzhouHook {

    public static final int ORDER = 70;
    static final String PLACEHOLDER_PREFIX = "[PII:";

    private final PiiDetector detector;
    private final Set<PiiType> enabledTypes;
    /** spec 118 §A / T417：自定义规则（内置五型之外——宿主命名正则；null = 无）。 */
    private final CustomPiiRules customRules;

    public PiiRedactionHook() {
        this(EnumSet.allOf(PiiType.class));
    }

    public PiiRedactionHook(Set<PiiType> enabledTypes) {
        this(enabledTypes, null);
    }

    /** spec 118 §A / T417：带自定义规则的构造（内置类型 + 自定义规则叠加脱敏）。 */
    public PiiRedactionHook(Set<PiiType> enabledTypes, CustomPiiRules customRules) {
        this(enabledTypes, customRules, false);
    }

    /** spec 731 / T1013：+格式保持模式（true = redact 分派 pseudonymize）。 */
    public PiiRedactionHook(Set<PiiType> enabledTypes, CustomPiiRules customRules,
                            boolean formatPreserving) {
        this.detector = new PiiDetector(formatPreserving);
        this.enabledTypes = EnumSet.copyOf(enabledTypes == null || enabledTypes.isEmpty()
                ? EnumSet.allOf(PiiType.class) : enabledTypes);
        this.customRules = customRules == null ? new CustomPiiRules(List.of()) : customRules;
    }

    @Override
    public String name() {
        return "PiiRedactionHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        if (ctx.error() != null || ctx.result() == null) {
            return HookResult.CONTINUE;
        }
        String content = String.valueOf(ctx.result());
        if (content.contains(PLACEHOLDER_PREFIX)) {
            return HookResult.CONTINUE; // 幂等：占位符已是脱敏产物（readback 纵深）
        }
        String redacted = detector.redact(content, enabledTypes);
        if (customRules != null && !customRules.isEmpty()) {
            // spec 118 §A / T417：自定义规则叠加（内置结果之上——占位符幂等短路天然防重复）
            redacted = customRules.redact(redacted);
        }
        if (redacted == content) { // 引用等——无命中零改写
            return HookResult.CONTINUE;
        }
        for (PiiDetector.PiiMatch m : detector.scan(content)) {
            if (enabledTypes.contains(m.type())) {
                BuzhouMetricsHolder.metrics().counter("buzhou.guard.pii.redactions",
                        "type", m.type().name());
                PiiHitStats.global().record(m.type(), PiiHitStats.Side.OUTPUT); // spec 144/313：输出侧命中统计
            }
        }
        if (customRules != null && !customRules.isEmpty()) {
            for (String ruleName : customRuleHits(redacted)) {
                PiiHitStats.global().recordCustom(ruleName, PiiHitStats.Side.OUTPUT);
            }
        }
        ctx.replaceResult(redacted);
        return HookResult.CONTINUE;
    }
    /** 从脱敏产物提取自定义规则命中名（占位符 [PII:NAME]；内置类型名剔除）。 */
    private static List<String> customRuleHits(String redacted) {
        List<String> names = new ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\[PII:([A-Z0-9_]{2,32})\\]").matcher(redacted);
        while (m.find()) {
            String name = m.group(1);
            boolean builtIn = false;
            for (PiiType type : PiiType.values()) {
                if (type.name().equals(name)) {
                    builtIn = true;
                    break;
                }
            }
            if (!builtIn) {
                names.add(name);
            }
        }
        return names;
    }
}