package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class HookChain {

    private final List<BuzhouHook> hooks;

    public HookChain(Collection<BuzhouHook> hooks, Set<String> disabledHookNames) {
        this.hooks = hooks.stream()
                .filter(h -> !disabledHookNames.contains(h.name()))
                .sorted(Comparator.comparingInt(BuzhouHook::order)
                        .thenComparing(BuzhouHook::name))
                .toList();
    }

    public static HookChain of(Collection<BuzhouHook> hooks) {
        return new HookChain(hooks, Set.of());
    }

    public List<BuzhouHook> hooks() {
        return hooks;
    }

    public HookResult beforeTurn(TurnContext ctx) {
        return run(ctx, (hook, c) -> hook.beforeTurn(c));
    }

    public HookResult afterTurn(TurnContext ctx) {
        return run(ctx, (hook, c) -> hook.afterTurn(c));
    }

    public HookResult beforeModel(ModelCallContext ctx) {
        return run(ctx, (hook, c) -> hook.beforeModel(c));
    }

    public HookResult afterModel(ModelCallContext ctx) {
        return run(ctx, (hook, c) -> hook.afterModel(c));
    }

    /**
     * 终态失败后派发 {@code onModelError}。复用 {@link #run}：{@code Replace(ChatClientResponse)} 经
     * {@code applyReplace} 回填响应、{@code Block(reason)} 提前返回；全 {@code CONTINUE} 时返回放行。
     */
    public HookResult onModelError(ModelCallContext ctx) {
        return run(ctx, (hook, c) -> hook.onModelError(c));
    }

    public HookResult beforeTool(ToolCallContext ctx) {
        return run(ctx, (hook, c) -> hook.beforeTool(c));
    }

    public HookResult afterTool(ToolCallContext ctx) {
        return run(ctx, (hook, c) -> hook.afterTool(c));
    }

    public void fireEvent(SessionEventContext ctx) {
        hooks.forEach(hook -> hook.onEvent(ctx));
    }

    private <C extends HookContext> HookResult run(C ctx, BiFunction<BuzhouHook, C, HookResult> call) {
        for (BuzhouHook hook : hooks) {
            HookResult result = call.apply(hook, ctx);
            if (result instanceof HookResult.Replace replace) {
                applyReplace(ctx, replace.payload());
                continue;
            }
            if (result instanceof HookResult.Block block) {
                ctx.emitEvent(new SessionEvent("hook.blocked",
                        Map.of("hook", hook.name(), "reason", block.reason()), Instant.now()));
                return block;
            }
        }
        return HookResult.CONTINUE;
    }

    private void applyReplace(HookContext ctx, Object payload) {
        switch (ctx) {
            case ToolCallContext toolCtx -> {
                if (toolCtx.result() == null && payload instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> args = (Map<String, Object>) map;
                    toolCtx.replaceArguments(args);
                } else {
                    toolCtx.replaceResult(payload);
                }
            }
            case ModelCallContext modelCtx -> {
                if (payload instanceof org.springframework.ai.chat.client.ChatClientRequest request) {
                    modelCtx.replaceRequest(request);
                } else if (payload instanceof org.springframework.ai.chat.client.ChatClientResponse response) {
                    modelCtx.replaceResponse(response);
                }
            }
            case TurnContext turnCtx -> {
                if (turnCtx.response() == null && payload instanceof String input) {
                    turnCtx.replaceInput(input);
                } else if (payload instanceof String response) {
                    turnCtx.replaceResponse(response);
                }
            }
            default -> {
            }
        }
    }
}
