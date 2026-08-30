package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 轮次时延计时 hook（spec 191 / T563）：beforeTurn 记 nanoTime 起点、
 * afterTurn 计端到端时长喂 {@code buzhou.turn.duration} timer（tag=agent）+
 * per-session 滚动 64 样本读数（count/avg/max/last）。重入 beforeTurn 覆盖
 * 重计（异常路径缺 afterTurn 不错配）；LRU 1024 会话。
 */
public final class TurnTimingHook implements BuzhouHook {

    static final String TIMER = "buzhou.turn.duration";
    private static final int WINDOW = 64;
    private static final int MAX_SESSIONS = 1024;

    /** 单会话滚动读数。 */
    public record TurnStats(long count, double avgMillis, long maxMillis, long lastMillis) {
    }

    private static final class SessionTiming {
        long startNanos = Long.MIN_VALUE;
        final Deque<Long> samplesMillis = new ArrayDeque<>();
    }

    private final Map<String, SessionTiming> sessions = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SessionTiming> eldest) {
            return size() > MAX_SESSIONS;
        }
    };

    @Override
    public String name() {
        return "TurnTimingHook";
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        if (ctx == null || ctx.sessionId() == null) {
            return HookResult.CONTINUE;
        }
        SessionTiming timing;
        synchronized (sessions) {
            timing = sessions.computeIfAbsent(ctx.sessionId(), k -> new SessionTiming());
        }
        timing.startNanos = System.nanoTime(); // 重入覆盖——异常缺 afterTurn 不错配
        return HookResult.CONTINUE;
    }

    @Override
    public HookResult afterTurn(TurnContext ctx) {
        if (ctx == null || ctx.sessionId() == null) {
            return HookResult.CONTINUE;
        }
        SessionTiming timing;
        synchronized (sessions) {
            timing = sessions.get(ctx.sessionId());
        }
        if (timing == null || timing.startNanos == Long.MIN_VALUE) {
            return HookResult.CONTINUE; // 无起点的 after——安全跳过
        }
        long elapsedMillis = (System.nanoTime() - timing.startNanos) / 1_000_000;
        timing.startNanos = Long.MIN_VALUE;
        synchronized (timing.samplesMillis) {
            timing.samplesMillis.addLast(elapsedMillis);
            while (timing.samplesMillis.size() > WINDOW) {
                timing.samplesMillis.removeFirst();
            }
        }
        BuzhouMetricsHolder.metrics().timer(TIMER,
                java.time.Duration.ofMillis(elapsedMillis), "agent", ctx.agentName());
        return HookResult.CONTINUE;
    }

    /** 单会话滚动读数（无样本 = 零值行）。 */
    public TurnStats stats(String sessionId) {
        SessionTiming timing;
        synchronized (sessions) {
            timing = sessions.get(sessionId);
        }
        if (timing == null) {
            return new TurnStats(0, 0, 0, 0);
        }
        synchronized (timing.samplesMillis) {
            if (timing.samplesMillis.isEmpty()) {
                return new TurnStats(0, 0, 0, 0);
            }
            long count = timing.samplesMillis.size();
            long sum = 0;
            long max = 0;
            long last = timing.samplesMillis.getLast();
            for (long sample : timing.samplesMillis) {
                sum += sample;
                max = Math.max(max, sample);
            }
            return new TurnStats(count, (double) sum / count, max, last);
        }
    }
}
