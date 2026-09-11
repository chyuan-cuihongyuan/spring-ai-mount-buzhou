package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.BuzhouHook;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.StreamTextFilter;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 流式回复秘密扫描（spec 536 / T825——400 三缝的第四缝：模型回复出站流；
 * 500 StreamTextFilter SPI 的第二消费者——证明 SPI 可组合）。滑动窗口
 * 缓冲与占位符不拆分钳制同 PiiStreamRedactionHook（528 族同法——算法
 * 同构独立实现，安全域互不依赖）。命中计数复用
 * {@code buzhou.guard.secret.redactions}（type tag）+ SecretHitStats
 * OUTPUT 侧。
 *
 * <p>诚实边界：窗口（默认 128）放不下的超长跨界实体不保；会话历史不
 * 回溯清洗（出站缝每轮拦截）。
 */
public class SecretScanStreamHook implements BuzhouHook {

    public static final int ORDER = 76;
    /** 默认回看窗口（字符）。 */
    public static final int DEFAULT_WINDOW = 128;

    private final SecretScanner scanner;
    private final int window;

    public SecretScanStreamHook() {
        this(null, DEFAULT_WINDOW);
    }

    public SecretScanStreamHook(Set<SecretType> types, int window) {
        if (window < 1) {
            throw new IllegalArgumentException("窗口必须 >= 1（当前 " + window + "）");
        }
        this.scanner = types == null || types.isEmpty()
                ? new SecretScanner() : new SecretScanner(types);
        this.window = window;
    }

    @Override
    public String name() {
        return "SecretScanStreamHook";
    }

    @Override
    public int order() {
        return ORDER;
    }

    /** 每轮新建窗口过滤器（有状态——跨 chunk 缓冲；同轮串行回调契约）。 */
    @Override
    public StreamTextFilter replyStreamFilter() {
        return new WindowFilter();
    }

    private final class WindowFilter implements StreamTextFilter {

        private final StringBuilder pending = new StringBuilder();

        @Override
        public String filter(String chunk) {
            if (chunk == null || chunk.isEmpty()) {
                return "";
            }
            pending.append(chunk);
            return drain(false);
        }

        @Override
        public String flush() {
            return drain(true);
        }

        /** 脱敏在窗完整秘密，发出安全前缀（flush 时全量）；占位符不拆分。 */
        private String drain(boolean finalPass) {
            if (pending.length() == 0) {
                return "";
            }
            String text = pending.toString();
            List<SecretScanner.SecretMatch> hits = scanner.scan(text);
            String redacted = scanner.redact(text);
            if (redacted != text) {
                // 命中计数（approximate：重叠区间按 scan 命中计——SecretScanHook 同口径）
                for (SecretScanner.SecretMatch match : hits) {
                    io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                            .counter("buzhou.guard.secret.redactions",
                                    "type", match.type().name());
                    SecretHitStats.global().record(match.type(),
                            io.github.chyuan_cuihongyuan.buzhou.guard.secret.SecretHitStats.Side.OUTPUT);
                }
            }
            pending.replace(0, pending.length(), redacted);
            int emitLength = finalPass
                    ? pending.length()
                    : Math.max(0, pending.length() - window + 1);
            emitLength = clampToPlaceholderBoundary(emitLength);
            if (emitLength == 0) {
                return "";
            }
            String outbound = pending.substring(0, emitLength);
            pending.delete(0, emitLength);
            return outbound;
        }

        /** emit 边界落在占位符内部时回退到占位符起点——下游所见占位符恒完整。 */
        private int clampToPlaceholderBoundary(int emitLength) {
            if (emitLength <= 0 || emitLength >= pending.length()) {
                return emitLength;
            }
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("\\[SECRET:[A-Z0-9_]{2,32}\\]").matcher(pending);
            while (matcher.find()) {
                if (matcher.start() < emitLength && emitLength < matcher.end()) {
                    return matcher.start();
                }
            }
            return emitLength;
        }
    }
}
