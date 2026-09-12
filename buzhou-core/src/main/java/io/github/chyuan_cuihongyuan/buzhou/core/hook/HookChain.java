package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiFunction;

/**
 * hook 链（spec 13 §core-2）：按 order（同序按名稳定）逐 hook 派发八个回调面。
 *
 * <p>spec 646 / T942–T943：内嵌 <b>per-hook 耗时观测</b>——每回调面包裹
 * {@link System#nanoTime()}，按 hook 名累计 count / totalNanos / maxNanos
 * （{@link #stats()} 快照面）；单次超 {@link #SLOW_HOOK_WARN_NANOS} 输出
 * WARN（原子去重：每 hook 首慢只告一次，此后只累计不刷屏）。纯观测零行为
 * 变化——不注入超时不截断（hook 护栏语义敏感，跳过即失效）。
 */
public class HookChain {

    /** 慢 hook 告警阈值（100ms——Turn 主链路内联面，超此量级即值得被看见）。 */
    public static final long SLOW_HOOK_WARN_NANOS = 100_000_000L;

    private static final System.Logger LOGGER = System.getLogger(HookChain.class.getName());

    private final List<BuzhouHook> hooks;
    private final ConcurrentHashMap<String, Timing> timings = new ConcurrentHashMap<>();

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
        return run(ctx, "beforeTurn", (hook, c) -> hook.beforeTurn(c));
    }

    public HookResult afterTurn(TurnContext ctx) {
        return run(ctx, "afterTurn", (hook, c) -> hook.afterTurn(c));
    }

    public HookResult beforeModel(ModelCallContext ctx) {
        return run(ctx, "beforeModel", (hook, c) -> hook.beforeModel(c));
    }

    public HookResult afterModel(ModelCallContext ctx) {
        return run(ctx, "afterModel", (hook, c) -> hook.afterModel(c));
    }

    /**
     * 终态失败后派发 {@code onModelError}。复用 {@link #run}：{@code Replace(ChatClientResponse)} 经
     * {@code applyReplace} 回填响应、{@code Block(reason)} 提前返回；全 {@code CONTINUE} 时返回放行。
     */
    public HookResult onModelError(ModelCallContext ctx) {
        return run(ctx, "onModelError", (hook, c) -> hook.onModelError(c));
    }

    public HookResult beforeTool(ToolCallContext ctx) {
        return run(ctx, "beforeTool", (hook, c) -> hook.beforeTool(c));
    }

    public HookResult afterTool(ToolCallContext ctx) {
        return run(ctx, "afterTool", (hook, c) -> hook.afterTool(c));
    }

    public void fireEvent(SessionEventContext ctx) {
        for (BuzhouHook hook : hooks) {
            long start = System.nanoTime();
            hook.onEvent(ctx);
            record(hook, "onEvent", System.nanoTime() - start);
        }
    }

    /**
     * 每轮新建回复流出站过滤器集（spec 500 / T751）——按 hook 序（已排序）收集
     * {@link BuzhouHook#replyStreamFilter()} 非 null 实例。空表 = 无过滤器（调用方
     * 短路零包装零开销）。
     */
    public java.util.List<StreamTextFilter> newReplyFilters() {
        java.util.List<StreamTextFilter> filters = new java.util.ArrayList<>(1);
        for (BuzhouHook hook : hooks) {
            StreamTextFilter filter = hook.replyStreamFilter();
            if (filter != null) {
                filters.add(filter);
            }
        }
        return java.util.List.copyOf(filters);
    }

    /** 单 hook 耗时快照（spec 646）：count 累计 / totalNanos 合计 / maxNanos 单次峰值。 */
    public record HookTiming(String hookName, long count, long totalNanos, long maxNanos) {

        /** 均值纳秒（零调用诚实 0）。 */
        public double avgNanos() {
            return count == 0 ? 0.0 : (double) totalNanos / count;
        }
    }

    /** per-hook 计时累计（LongAdder 无锁累计 + volatile max）。 */
    private static final class Timing {
        final LongAdder count = new LongAdder();
        final LongAdder totalNanos = new LongAdder();
        volatile long maxNanos;
        /** 慢 hook WARN 去重（0=未告过，1=已告过）。 */
        final AtomicLong slowWarned = new AtomicLong();

        void record(long nanos) {
            count.increment();
            totalNanos.add(nanos);
            long currentMax;
            long observed = maxNanos;
            do {
                currentMax = observed;
                if (nanos <= currentMax) {
                    break;
                }
            } while (!MAX_UPDATER.compareAndSet(this, currentMax, nanos));
        }

        HookTiming snapshot(String hookName) {
            return new HookTiming(hookName, count.sum(), totalNanos.sum(), maxNanos);
        }

        private static final AtomicLongFieldUpdater<Timing> MAX_UPDATER =
                AtomicLongFieldUpdater.newUpdater(Timing.class, "maxNanos");
    }

    /** 耗时快照（hook 名 → 计时；不可变）。 */
    public Map<String, HookTiming> stats() {
        Map<String, HookTiming> out = new java.util.LinkedHashMap<>();
        timings.forEach((name, t) -> out.put(name, t.snapshot(name)));
        return Map.copyOf(out);
    }

    private <C extends HookContext> HookResult run(C ctx, String callback,
            BiFunction<BuzhouHook, C, HookResult> call) {
        for (BuzhouHook hook : hooks) {
            long start = System.nanoTime();
            HookResult result = call.apply(hook, ctx);
            record(hook, callback, System.nanoTime() - start);
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

    private void record(BuzhouHook hook, String callback, long nanos) {
        Timing timing = timings.computeIfAbsent(hook.name(), k -> new Timing());
        timing.record(nanos);
        // spec 647：进程级聚合镜像（未装配 = null 跳过——链内私有口径不变）
        HookTimingAggregator aggregator = HookTimingAggregator.Holder.current();
        if (aggregator != null) {
            aggregator.record(hook.name(), nanos);
        }
        if (nanos > SLOW_HOOK_WARN_NANOS && timing.slowWarned.compareAndSet(0, 1)) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "慢 hook（首次告警，此后只累计）：{0}.{1} 单次 {2}ms——Turn 主链路内联面，持续偏慢请自查该 hook",
                    hook.name(), callback, nanos / 1_000_000);
        }
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
