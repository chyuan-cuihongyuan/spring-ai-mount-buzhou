package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 上下文余量水位 hook（spec 181 / T553，水库低水位预警借鉴）：beforeModel 估算
 * 本轮注入字符量 / 配置窗口字符容量 → gauge buzhou.context.utilization（活读）
 * + chars；利用率 ≥ 低水位线发一次 {@code context.low-watermark} 事件
 * （<b>翻转制</b>——跨线各一次不刷屏）；容量未配（0）= 静默零行为（零配置安全）。
 *
 * <p>字符口径估算（token 精算归装配侧 ContextWindowResolver）；纯观测不裁决
 * （限制/压缩动作归既有机制——分层诚实）。
 */
public final class ContextWatermarkHook implements BuzhouHook {

    public static final String EVENT_LOW_WATERMARK = "context.low-watermark";
    private static final String GAUGE_UTILIZATION = "buzhou.context.utilization";
    private static final String GAUGE_CHARS = "buzhou.context.chars";
    private static final int MAX_SESSIONS = 1024;

    /** 配置：窗口字符容量（0=禁用）/ 低水位线 (0,1)。 */
    public record Config(long windowChars, double lowWatermarkRatio) {
        public Config {
            if (windowChars < 0 || !(lowWatermarkRatio > 0 && lowWatermarkRatio < 1)) {
                throw new IllegalArgumentException(
                        "windowChars>=0、lowWatermarkRatio∈(0,1)");
            }
        }

        public static Config disabled() {
            return new Config(0, 0.8);
        }
    }

    private final Config config;
    private volatile double lastUtilization = -1;
    private volatile long lastChars = -1;
    /** per-session 低水位翻转态（true=当前在低水位区）。 */
    private final LinkedHashMap<String, Boolean> lowWater = new LinkedHashMap<>(16,
            0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
            return size() > MAX_SESSIONS;
        }
    };

    public ContextWatermarkHook() {
        this(Config.disabled());
    }

    public ContextWatermarkHook(Config config) {
        this.config = config == null ? Config.disabled() : config;
    }

    @Override
    public String name() {
        return "ContextWatermarkHook";
    }

    /** 当前处于低水位区的会话数（spec 526 健康桥接读数——翻转态计数）。 */
    public synchronized int lowWaterSessionCount() {
        return (int) lowWater.values().stream().filter(Boolean::booleanValue).count();
    }

    /** 窗口容量配置是否启用（false = 全程静默零行为）。 */
    public boolean isEnabled() {
        return config.windowChars() > 0;
    }

    @Override
    public HookResult beforeModel(ModelCallContext ctx) {
        if (ctx == null || ctx.sessionId() == null || config.windowChars() <= 0
                || ctx.request() == null || ctx.request().prompt() == null) {
            return HookResult.CONTINUE; // 未配容量/缺请求面——静默零行为
        }
        long chars = ctx.request().prompt().getInstructions().stream()
                .map(m -> m.getText() == null ? "" : m.getText())
                .mapToLong(String::length)
                .sum();
        double utilization = Math.min(1.0, (double) chars / config.windowChars());
        lastUtilization = utilization;
        lastChars = chars;
        BuzhouMetricsHolder.metrics().gauge(GAUGE_UTILIZATION, () -> lastUtilization);
        BuzhouMetricsHolder.metrics().gauge(GAUGE_CHARS, () -> lastChars);

        boolean low = utilization >= config.lowWatermarkRatio();
        boolean flipped;
        synchronized (lowWater) {
            Boolean previous = lowWater.get(ctx.sessionId());
            flipped = previous == null || previous != low;
            lowWater.put(ctx.sessionId(), low);
        }
        if (flipped && low) {
            ctx.emitEvent(new SessionEvent(EVENT_LOW_WATERMARK,
                    Map.of("sessionId", ctx.sessionId(),
                            "utilization", Math.round(utilization * 1000) / 1000.0,
                            "chars", chars,
                            "windowChars", config.windowChars(),
                            "remainingChars", Math.max(0, config.windowChars() - chars)),
                    Instant.now()));
        }
        return HookResult.CONTINUE;
    }

    /** 最近利用率（观测/测试；未估算过 = -1）。 */
    public double lastUtilization() {
        return lastUtilization;
    }

    /** 最近估算字符量（观测/测试）。 */
    public long lastChars() {
        return lastChars;
    }
}
