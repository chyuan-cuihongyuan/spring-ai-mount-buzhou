package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 密钥扫描 hook（spec 400 / T692，gitleaks 借鉴——在内容移动的缝上扫描）：
 * 三缝 MASK，同检测器同配置。
 * <ul>
 *   <li>beforeTurn（59，先于 PII 输入侧 60）：用户粘贴凭据不进 prompt 与观测。</li>
 *   <li>beforeTool（40）：出站工具参数值脱敏——外发前拦截（gitleaks pre-commit
 *       同位）。诚实边界：宿主自管凭据应经 env/工具配置注入，不走模型可见
 *       上下文——开启即假设该纪律成立。</li>
 *   <li>afterTool（69，先于 PII 输出侧 70）：工具结果回灌上下文前脱敏。</li>
 * </ul>
 * 计数器 {@code buzhou.guard.secret.redactions}（tag type——7 值有界枚举）。
 * opt-in 默认关（GuardModule 装配面控制）。
 */
public class SecretScanHook implements BuzhouHook {

    /** 三缝统一站位（BuzhouHook 单 order——三缝同 hook 必同位）：40，均先于
     * PII 两侧（输入 60/输出 70）且先于 HITL 指纹（300）。 */
    public static final int ORDER = 40;

    private final SecretScanner scanner;

    public SecretScanHook() {
        this(null);
    }

    public SecretScanHook(Set<SecretType> enabledTypes) {
        this.scanner = new SecretScanner(enabledTypes);
    }

    @Override
    public String name() {
        return "SecretScanHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        String input = ctx.input();
        if (input == null || input.isEmpty()) {
            return HookResult.CONTINUE;
        }
        String redacted = scanner.redact(input);
        if (redacted == input) {
            return HookResult.CONTINUE; // 无命中零改写
        }
        recordHits(scanner.scan(input));
        ctx.replaceInput(redacted);
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        Map<String, Object> args = ctx.arguments();
        if (args == null || args.isEmpty()) {
            return HookResult.CONTINUE;
        }
        Map<String, Object> rewritten = null;
        for (Map.Entry<String, Object> e : args.entrySet()) {
            if (e.getValue() instanceof String s && !s.isEmpty()) {
                String redacted = scanner.redact(s);
                if (redacted != s) {
                    if (rewritten == null) {
                        rewritten = new LinkedHashMap<>(args);
                    }
                    rewritten.put(e.getKey(), redacted);
                    recordHits(scanner.scan(s));
                }
            }
        }
        if (rewritten == null) {
            return HookResult.CONTINUE;
        }
        ctx.replaceArguments(rewritten);
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        if (ctx.error() != null || ctx.result() == null) {
            return HookResult.CONTINUE;
        }
        String content = String.valueOf(ctx.result());
        String redacted = scanner.redact(content);
        if (redacted == content) {
            return HookResult.CONTINUE;
        }
        recordHits(scanner.scan(content));
        ctx.replaceResult(redacted);
        return HookResult.CONTINUE;
    }

    private void recordHits(List<SecretScanner.SecretMatch> matches) {
        for (SecretScanner.SecretMatch m : matches) {
            BuzhouMetricsHolder.metrics().counter("buzhou.guard.secret.redactions",
                    "type", m.type().name());
        }
    }
}
