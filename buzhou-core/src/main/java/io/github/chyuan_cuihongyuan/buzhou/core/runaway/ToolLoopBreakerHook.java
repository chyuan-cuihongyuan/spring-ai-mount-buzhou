package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ToolCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具循环断路器（spec 327 / T645，326 重复检测的工具面孪生）：order 245
 * （熔断 240 后——熔断按错误率跳闸，本闸按<b>调用形态</b>断路）。beforeTool
 * 以 toolName + args Map.hashCode（顺序无关）为 key 做相邻 run 计数——同
 * key 连续达窗即 block「[工具循环]」带三选一出路（换参/换工具/收束）。
 * <b>持续干预直到换 key</b>（与 326 闩一次不同：工具面不拦就继续烧真配额）；
 * 默认干预（装配即拦——复读文本只占上下文，循环工具在烧钱）。per-session
 * 超 1024 整体重置（诚实降级同 326）。
 */
public final class ToolLoopBreakerHook implements BuzhouHook {

    public static final int ORDER = 245;
    static final String MARKER = "[工具循环]";
    static final String BROKEN_COUNTER = "buzhou.runaway.tool-loop.broken";
    static final int MAX_SESSIONS = 1024;

    private static final class SessionState {
        String lastKey;
        int run;
    }

    private final int window;
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    /** @param window 同 key 连续多少次开始拦（≥2） */
    public ToolLoopBreakerHook(int window) {
        if (window < 2) {
            throw new IllegalArgumentException("window >= 2（当前 " + window + "）");
        }
        this.window = window;
    }

    @Override
    public String name() {
        return "ToolLoopBreakerHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTool(ToolCallContext ctx) {
        if (ctx == null || ctx.toolName() == null || ctx.sessionId() == null) {
            return HookResult.CONTINUE;
        }
        SessionState state = stateFor(ctx.sessionId());
        String key = ctx.toolName() + "|" + (ctx.arguments() == null
                ? Map.of().hashCode() : ctx.arguments().hashCode());
        synchronized (state) {
            if (key.equals(state.lastKey)) {
                state.run++;
            } else {
                state.lastKey = key;
                state.run = 1; // 换参/换工具——新 run
            }
            if (state.run < window) {
                return HookResult.CONTINUE;
            }
            BuzhouMetricsHolder.metrics().counter(BROKEN_COUNTER);
            return HookResult.block(MARKER + "\n工具：" + ctx.toolName()
                    + "\n原因：同一工具同一参数已连续调用 " + state.run
                    + " 次——参数未变结果不会变，每次调用都在烧配额。"
                    + "请三选一：更换参数重试 / 改用其他工具 / 以当前结果收束任务。");
        }
    }

    @Override
    public HookResult afterTool(ToolCallContext ctx) {
        return HookResult.CONTINUE;
    }

    /** 会话当前 run 长（观测面/测试）。 */
    public int currentRun(String sessionId, String toolName, Map<String, Object> args) {
        SessionState state = sessions.get(sessionId);
        if (state == null) {
            return 0;
        }
        String key = toolName + "|" + (args == null ? Map.of().hashCode() : args.hashCode());
        synchronized (state) {
            return key.equals(state.lastKey) ? state.run : 0;
        }
    }

    private SessionState stateFor(String sessionId) {
        SessionState state = sessions.get(sessionId);
        if (state != null) {
            return state;
        }
        if (sessions.size() >= MAX_SESSIONS) {
            sessions.clear(); // 诚实降级：超上限整体重置（run 重新累计）
        }
        return sessions.computeIfAbsent(sessionId, k -> new SessionState());
    }
}
