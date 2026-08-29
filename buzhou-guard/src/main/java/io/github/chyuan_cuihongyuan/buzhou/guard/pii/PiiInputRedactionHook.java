package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.EnumSet;
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

    public PiiInputRedactionHook() {
        this(EnumSet.allOf(PiiType.class));
    }

    public PiiInputRedactionHook(Set<PiiType> enabledTypes) {
        this.detector = new PiiDetector();
        this.enabledTypes = EnumSet.copyOf(enabledTypes == null || enabledTypes.isEmpty()
                ? EnumSet.allOf(PiiType.class) : enabledTypes);
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
        if (redacted == input) {
            return HookResult.CONTINUE; // 无命中零改写（引用等）
        }
        for (PiiDetector.PiiMatch m : detector.scan(input)) {
            if (enabledTypes.contains(m.type())) {
                BuzhouMetricsHolder.metrics().counter("buzhou.guard.pii.input-redactions",
                        "type", m.type().name());
            }
        }
        ctx.replaceInput(redacted);
        return HookResult.CONTINUE;
    }
}
