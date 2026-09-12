package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 用户输入 PII 脱敏 hook（spec 106 §A / T389，spec 86 fog 项收口）：beforeTurn 把
 * 输入中的命中类型替换 {@code [PII:TYPE]} 占位符再进模型——用户误贴的身份证/卡号
 * 不进 prompt 与观测。<b>与输出侧（PiiRedactionHook）正交</b>：输入是用户主动给
 * 的（改写输入语义可接受——占位符保留可读性），输出是外部数据（同款改写）；两
 * 开关独立 opt-in。幂等：已含占位符前缀不再处理。计数器
 * {@code buzhou.guard.pii.input-redactions}（tag type 有界）。
 */
public class PiiInputRedactionHook implements BuzhouHook {

    public static final int ORDER = 60;
    static final String PLACEHOLDER_PREFIX = "[PII:";

    private final PiiDetector detector;
    private final Set<PiiType> enabledTypes;
    private final CustomPiiRules customRules;

    public PiiInputRedactionHook() {
        this(EnumSet.allOf(PiiType.class));
    }

    public PiiInputRedactionHook(Set<PiiType> enabledTypes) {
        this(enabledTypes, null);
    }

    /** spec 129 / T475：输入侧叠加自定义规则（镜像输出侧三参构造器；null = 无叠加）。 */
    public PiiInputRedactionHook(Set<PiiType> enabledTypes, CustomPiiRules customRules) {
        this(enabledTypes, customRules, false);
    }

    /** spec 731 / T1013：+格式保持模式（true = redact 分派 pseudonymize）。 */
    public PiiInputRedactionHook(Set<PiiType> enabledTypes, CustomPiiRules customRules,
                                 boolean formatPreserving) {
        this.detector = new PiiDetector(formatPreserving);
        this.enabledTypes = EnumSet.copyOf(enabledTypes == null || enabledTypes.isEmpty()
                ? EnumSet.allOf(PiiType.class) : enabledTypes);
        this.customRules = customRules == null ? new CustomPiiRules(List.of()) : customRules;
    }

    @Override
    public String name() {
        return "PiiInputRedactionHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        String input = ctx.input();
        if (input == null || input.isEmpty() || input.contains(PLACEHOLDER_PREFIX)) {
            return HookResult.CONTINUE; // 幂等：占位符已是脱敏产物
        }
        String redacted = detector.redact(input, enabledTypes);
        // spec 129 / T475：自定义规则叠加（内置结果之上继续脱领域格式——幂等由规则特异性保证）
        if (!customRules.isEmpty()) {
            redacted = customRules.redact(redacted);
        }
        if (redacted == input) {
            return HookResult.CONTINUE; // 无命中零改写（引用等）
        }
        for (PiiDetector.PiiMatch m : detector.scan(input)) {
            if (enabledTypes.contains(m.type())) {
                BuzhouMetricsHolder.metrics().counter("buzhou.guard.pii.input-redactions",
                        "type", m.type().name());
                PiiHitStats.global().record(m.type(), PiiHitStats.Side.INPUT); // spec 164/313：输入侧命中统计
            }
        }
        if (!customRules.isEmpty()) {
            for (String ruleName : PiiHitStats.extractCustomRuleNames(redacted)) {
                PiiHitStats.global().recordCustom(ruleName, PiiHitStats.Side.INPUT);
            }
        }
        ctx.replaceInput(redacted);
        return HookResult.CONTINUE;
    }
}
