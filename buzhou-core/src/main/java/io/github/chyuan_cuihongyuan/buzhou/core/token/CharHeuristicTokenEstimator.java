package io.github.chyuan_cuihongyuan.buzhou.core.token;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 字符启发式 token 估算器（拉丁 ≈4 字符/token、汉字 ≈2 字符/token、JSON ×1.15、
 * 媒体固定计数）——无 tokenizer 依赖的零配置基线。
 *
 * <p>spec 707 / T965：自 {@code core.internal.token} 迁出——跨模块复用类不入
 * internal（边界守卫 ModuleBoundaryGuardTest 口径）。
 */
public class CharHeuristicTokenEstimator implements TokenEstimator {

    private static final int CHARS_PER_TOKEN_LATIN = 4;
    private static final int CHARS_PER_TOKEN_CJK = 2;
    private static final double JSON_UPLIFT = 1.15;

    /** impl-786 / spec 1033：估算调用量与总量进程级读面（预算面可观测性）。 */
    private static final java.util.concurrent.atomic.AtomicLong estimateCalls =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong batchCalls =
            new java.util.concurrent.atomic.AtomicLong();
    private static final java.util.concurrent.atomic.AtomicLong totalEstimatedTokens =
            new java.util.concurrent.atomic.AtomicLong();

    @Override
    public int estimate(String text) {
        estimateCalls.incrementAndGet(); // spec 1033：调用量显形
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0;
        for (int i = 0; i < text.length(); i++) {
            if (Character.UnicodeScript.of(text.charAt(i)) == Character.UnicodeScript.HAN) {
                cjk++;
            }
        }
        double tokens = (double) (text.length() - cjk) / CHARS_PER_TOKEN_LATIN
                + (double) cjk / CHARS_PER_TOKEN_CJK;
        if (looksLikeJson(text)) {
            tokens *= JSON_UPLIFT;
        }
        int result = (int) Math.ceil(tokens);
        totalEstimatedTokens.addAndGet(result);
        return result;
    }

    @Override
    public int estimateMessages(List<Message> messages) {
        batchCalls.incrementAndGet(); // spec 1033：批量估算调用量
        int total = messages.stream()
                .mapToInt(m -> estimate(m.getText()) + 4 + mediaCharge(m))
                .sum();
        return total;
    }

    /** 估算调用量与总量只读快照（静态进程级——调用点内联构造先例）。 */
    public static TokenEstimateStats stats() {
        return new TokenEstimateStats(estimateCalls.get(), batchCalls.get(),
                totalEstimatedTokens.get());
    }

    /** 进程态清零（测试隔离注入点——生产勿调）。 */
    public static void resetForTest() {
        estimateCalls.set(0);
        batchCalls.set(0);
        totalEstimatedTokens.set(0);
    }

    /** 估算计数行（不可变）。 */
    public record TokenEstimateStats(long estimateCalls, long batchCalls,
                                     long totalEstimatedTokens) {
    }

    /**
     * spec 27 / T106：媒体固定计数（每媒体 {@link io.github.chyuan_cuihongyuan.buzhou.core.session.MediaRef#TOKENS_PER_MEDIA}
     * ——尺寸未知按中位档位估，预算闸按此累计）。
     */
    private int mediaCharge(Message message) {
        if (message instanceof org.springframework.ai.chat.messages.UserMessage userMessage) {
            return userMessage.getMedia().size()
                    * io.github.chyuan_cuihongyuan.buzhou.core.session.MediaRef.TOKENS_PER_MEDIA;
        }
        return 0;
    }

    @Override
    public String name() {
        return "char-heuristic";
    }

    private boolean looksLikeJson(String text) {
        String trimmed = text.strip();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }
}
