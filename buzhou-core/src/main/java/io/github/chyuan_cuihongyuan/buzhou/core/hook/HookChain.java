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
    private final Set<String> ghostDisabledNames;
    private final ConcurrentHashMap<String, Timing> timings = new ConcurrentHashMap<>();
    /** impl-766 / spec 1013：Replace 载荷应用/丢弃计数（幽灵载荷显形）。 */
    private final AtomicLong replaceApplied = new AtomicLong();
    private final AtomicLong replaceDropped = new AtomicLong();

    public HookChain(Collection<BuzhouHook> hooks, Set<String> disabledHookNames) {
        List<BuzhouHook> resolved = hooks.stream()
                .filter(h -> !disabledHookNames.contains(h.name()))
                .sorted(Comparator.comparingInt(BuzhouHook::order)
                        .thenComparing(BuzhouHook::name))
                .toList();
        this.hooks = resolved;
        Set<String> present = new java.util.HashSet<>();
        Set<String> duplicated = new java.util.HashSet<>();
        for (BuzhouHook hook : hooks) {
            // spec 1524 / T2299：重复 hook 名显形——同名 hook 派发序不稳定（order 平局
            // 时 name 比较无区分）且 stats/禁用配置按名对位歧义；Kong 插件重名诊断思想
            if (!present.add(hook.name())) {
                duplicated.add(hook.name());
            }
        }
        if (!duplicated.isEmpty()) {
            LOGGER.log(System.Logger.Level.WARNING,
                    "重复 hook 名注册（派发序不稳定、stats/禁用对位歧义）：{0}",
                    duplicated);
        }
        Set<String> ghosts = new java.util.HashSet<>();
        for (String disabled : disabledHookNames) {
            if (!present.contains(disabled)) {
                ghosts.add(disabled);
            }
        }
        this.ghostDisabledNames = Set.copyOf(ghosts);
    }

    public static HookChain of(Collection<BuzhouHook> hooks) {
        return new HookChain(hooks, Set.of());
    }

    public List<BuzhouHook> hooks() {
        return hooks;
    }

    /**
     * 链解析快照（spec 1002 / Kong plugin priority 思想）：解析后派发序 + 幽灵禁用集
     * （disabled 配置里未命中任何 hook 的名字——拼错静默蒸发的显形）。只读诊断，
     * 构造期一次计算，零行为变化。
     */
    public ChainComposition composition() {
        List<String> names = new java.util.ArrayList<>(hooks.size());
        for (BuzhouHook hook : hooks) {
            names.add(hook.name());
        }
        return new ChainComposition(names, ghostDisabledNames);
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

    /**
     * spec 1501 / T2253：事件通知面逐 hook 隔离——单个 hook 的 onEvent 抛
     * RuntimeException 记 ERROR 日志（hook 名 + 事件类型 + 栈）后继续其余 hook；
     * 计时 try/finally 仍入账（失败调用的耗时也不丢）。与裁决面（{@link #run}，
     * Block/Replace 语义）分离：裁决面保持 fail-fast 治理语义不动——治理点异常
     * 必须可见，通知面异常不该吞掉其余 hook 的事件消费（spec 1500 的 EventBus
     * SubscriberExceptionHandler 思想在 hook 事件通知域的同源应用）。
     */
    public void fireEvent(SessionEventContext ctx) {
        for (BuzhouHook hook : hooks) {
            long start = System.nanoTime();
            try {
                hook.onEvent(ctx);
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.ERROR,
                        "事件 hook 异常已隔离（不跳过其余 hook 的 onEvent）：hook={0}, event={1}",
                        hook.name(), ctx.event().type());
                LOGGER.log(System.Logger.Level.ERROR, "事件 hook onEvent 异常栈：", e);
            } finally {
                record(hook, "onEvent", System.nanoTime() - start);
            }
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
            do {
                // 每轮重读 maxNanos（RollingMaxCounter.record 同款）——重读若留在循环外，
                // CAS 失败后期望值永不过期刷新，maxNanos 被并发推进即活锁（R50 审计轮实证）
                currentMax = maxNanos;
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
                if (applyReplace(ctx, replace.payload())) {
                    replaceApplied.incrementAndGet();
                } else {
                    replaceDropped.incrementAndGet(); // spec 1013：幽灵载荷——类型不匹配静默跳过，显形
                }
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

    /**
     * 应用 Replace 载荷到上下文。@return 是否真实应用（载荷与上下文类型不匹配
     * 时 false——丢弃显形计入 {@link #replaceDroppedCount()}，行为仍为静默跳过）。
     */
    private boolean applyReplace(HookContext ctx, Object payload) {
        switch (ctx) {
            case ToolCallContext toolCtx -> {
                if (toolCtx.result() == null && payload instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> args = (Map<String, Object>) map;
                    toolCtx.replaceArguments(args);
                } else {
                    toolCtx.replaceResult(payload);
                }
                return true;
            }
            case ModelCallContext modelCtx -> {
                if (payload instanceof org.springframework.ai.chat.client.ChatClientRequest request) {
                    modelCtx.replaceRequest(request);
                    return true;
                } else if (payload instanceof org.springframework.ai.chat.client.ChatClientResponse response) {
                    modelCtx.replaceResponse(response);
                    return true;
                }
                return false;
            }
            case TurnContext turnCtx -> {
                if (turnCtx.response() == null && payload instanceof String input) {
                    turnCtx.replaceInput(input);
                    return true;
                } else if (payload instanceof String response) {
                    turnCtx.replaceResponse(response);
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    /** Replace 载荷应用累计（spec 1013）。 */
    public long replaceAppliedCount() {
        return replaceApplied.get();
    }

    /** Replace 载荷丢弃累计（类型不匹配静默跳过——幽灵载荷显形，spec 1013）。 */
    public long replaceDroppedCount() {
        return replaceDropped.get();
    }
}
