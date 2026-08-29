package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.TurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 同输入泛洪防护 hook（spec 167 / T531，API 网关 idempotency-key 风暴防护借鉴）：
 * beforeTurn 按 (sessionId, SHA-256(strip(input))) 记 <b>TTL 滚动窗</b>内重复次数
 * ——超阈值 block（可读理由：疑似循环/重放）；窗口滑出自动复位；<b>只拦完全相同</b>
 * 输入（改写重试不误伤——模糊相似归语义面，诚实边界）。per-session LRU 1024。
 */
public final class InputFloodGuardHook implements BuzhouHook {

    public static final int ORDER = 220;
    static final String BLOCKED_COUNTER = "buzhou.input-flood.blocked";
    private static final int MAX_SESSIONS = 1024;

    /** 配置：窗口内允许的最大相同次数（超过即 block）/ 滚动窗 TTL。 */
    public record Config(int maxRepeats, Duration window) {
        public Config {
            if (maxRepeats < 1 || window == null || window.isZero() || window.isNegative()) {
                throw new IllegalArgumentException("maxRepeats>=1、window 为正");
            }
        }

        public static Config defaults() {
            return new Config(5, Duration.ofSeconds(60));
        }
    }

    private record RepeatWindow(int count, Instant windowEnd) {
    }

    private final Config config;
    private final Clock clock;
    private final Map<String, RepeatWindow> windows = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, RepeatWindow> eldest) {
            return size() > MAX_SESSIONS;
        }
    };

    public InputFloodGuardHook() {
        this(Config.defaults(), Clock.systemUTC());
    }

    public InputFloodGuardHook(Config config, Clock clock) {
        this.config = config == null ? Config.defaults() : config;
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    @Override
    public String name() {
        return "InputFloodGuardHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public HookResult beforeTurn(TurnContext ctx) {
        if (ctx == null || ctx.sessionId() == null || ctx.input() == null) {
            return HookResult.CONTINUE;
        }
        String key = ctx.sessionId() + ":" + sha256(ctx.input().strip());
        RepeatWindow verdict;
        synchronized (windows) {
            Instant now = clock.instant();
            RepeatWindow current = windows.get(key);
            if (current == null || !now.isBefore(current.windowEnd())) {
                verdict = new RepeatWindow(1, now.plus(config.window()));
            } else {
                verdict = new RepeatWindow(current.count() + 1, current.windowEnd());
            }
            windows.put(key, verdict);
        }
        if (verdict.count() > config.maxRepeats()) {
            BuzhouMetricsHolder.metrics().counter(BLOCKED_COUNTER, 1);
            return HookResult.block("相同输入在 " + config.window().toSeconds() + "s 窗口内已出现 "
                    + verdict.count() + " 次（疑似循环/重放）——请变化输入或稍后再试");
        }
        return HookResult.CONTINUE;
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "unhashed:" + text.hashCode();
        }
    }
}
