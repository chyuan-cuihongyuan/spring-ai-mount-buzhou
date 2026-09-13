package io.github.chyuan_cuihongyuan.buzhou.core.token;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenEstimateStatsTest {

    private final CharHeuristicTokenEstimator estimator = new CharHeuristicTokenEstimator();

    @BeforeEach
    @AfterEach
    void resetReadout() {
        CharHeuristicTokenEstimator.resetForTest();
    }

    @Test
    void estimateCountsCallAndTokens() {
        int tokens = estimator.estimate("abcd");

        assertThat(tokens).isEqualTo(1);
        CharHeuristicTokenEstimator.TokenEstimateStats stats = CharHeuristicTokenEstimator.stats();
        assertThat(stats.estimateCalls()).isEqualTo(1);
        assertThat(stats.totalEstimatedTokens()).isEqualTo(1);
        assertThat(stats.batchCalls()).isZero();
    }

    @Test
    void cjkTextCountsDoubleChars() {
        estimator.estimate("中文中文");

        CharHeuristicTokenEstimator.TokenEstimateStats stats = CharHeuristicTokenEstimator.stats();
        assertThat(stats.estimateCalls()).isEqualTo(1);
        assertThat(stats.totalEstimatedTokens()).isEqualTo(2);
    }

    @Test
    void nullTextCountsCallWithZeroTokens() {
        estimator.estimate(null);

        CharHeuristicTokenEstimator.TokenEstimateStats stats = CharHeuristicTokenEstimator.stats();
        assertThat(stats.estimateCalls()).isEqualTo(1);
        assertThat(stats.totalEstimatedTokens()).isZero();
    }

    @Test
    void batchCountsCallsPerMessagePlusBatch() {
        Message m1 = new AssistantMessage("abcd");
        Message m2 = new AssistantMessage("abcd");
        int probeTokens = estimator.estimate(m1.getText()); // 预探测计入 estimateCalls

        int total = estimator.estimateMessages(List.of(m1, m2));

        CharHeuristicTokenEstimator.TokenEstimateStats stats = CharHeuristicTokenEstimator.stats();
        assertThat(stats.batchCalls()).isEqualTo(1);
        // 每消息 = 该消息文本估算 + 4 轮内开销；批量内 2 次 + 预探测 1 次
        assertThat(stats.estimateCalls()).isEqualTo(3);
        // totalEstimatedTokens 只累计 estimate() 结果（1 探测 + 批量 2 条）
        assertThat(stats.totalEstimatedTokens()).isEqualTo(3);
    }

    @Test
    void resetForTestZeroesAllCounters() {
        estimator.estimate("abcd");
        CharHeuristicTokenEstimator.resetForTest();

        assertThat(CharHeuristicTokenEstimator.stats())
                .isEqualTo(new CharHeuristicTokenEstimator.TokenEstimateStats(0, 0, 0));
    }
}
