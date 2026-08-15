package io.github.chyuan_cuihongyuan.buzhou.core.internal.token;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.TokenEstimator;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

public class CharHeuristicTokenEstimator implements TokenEstimator {

    private static final int CHARS_PER_TOKEN_LATIN = 4;
    private static final int CHARS_PER_TOKEN_CJK = 2;
    private static final double JSON_UPLIFT = 1.15;

    @Override
    public int estimate(String text) {
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
        return (int) Math.ceil(tokens);
    }

    @Override
    public int estimateMessages(List<Message> messages) {
        return messages.stream()
                .mapToInt(m -> estimate(m.getText()) + 4 + mediaCharge(m))
                .sum();
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
