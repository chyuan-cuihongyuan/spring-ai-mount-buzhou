package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2008 / T3118：重试主机排除合同——失败者让位、冷却到期回归、
 * 全排除回退全量、多失败累积、序保持、畸形 fail-fast。
 */
class RetryHostExclusionTest {

    @Test
    void failedHostShouldYieldInCooldown() {
        RetryHostExclusion exclusion = new RetryHostExclusion(30_000L);
        exclusion.recordFailure("model-a", 1_000L);
        assertThat(exclusion.filterCandidates(List.of("model-a", "model-b"), 2_000L))
                .containsExactly("model-b"); // 失败者让位
        assertThat(exclusion.excludedCount(List.of("model-a", "model-b"), 2_000L)).isEqualTo(1);
    }

    @Test
    void hostShouldReturnAfterCooldownExpires() {
        RetryHostExclusion exclusion = new RetryHostExclusion(1_000L);
        exclusion.recordFailure("model-a", 0);
        exclusion.recordFailure("model-b", 0);
        // 冷却内：双候选全排除 → 回退全量（可用性优先——排除是偏好不是硬门）
        assertThat(exclusion.filterCandidates(List.of("model-a", "model-b"), 999L))
                .containsExactly("model-a", "model-b");
        assertThat(exclusion.excludedCount(List.of("model-a", "model-b"), 999L)).isEqualTo(2);
        // 到期回归（恰好等于冷却窗即回归——界内才排除）
        assertThat(exclusion.filterCandidates(List.of("model-a"), 1_000L))
                .containsExactly("model-a");
    }

    @Test
    void allExcludedShouldFallBackToFullList() {
        RetryHostExclusion exclusion = new RetryHostExclusion(30_000L);
        exclusion.recordFailure("a", 0);
        exclusion.recordFailure("b", 0);
        // 全排除 → 回退全量（可用性优先，不制造空候选）
        assertThat(exclusion.filterCandidates(List.of("a", "b"), 100L))
                .containsExactly("a", "b");
        assertThat(exclusion.excludedCount(List.of("a", "b"), 100L)).isEqualTo(2);
    }

    @Test
    void multipleFailuresShouldAccumulate() {
        RetryHostExclusion exclusion = new RetryHostExclusion(10_000L);
        exclusion.recordFailure("a", 0);
        exclusion.recordFailure("b", 5_000L);
        assertThat(exclusion.filterCandidates(List.of("a", "b", "c", "d"), 6_000L))
                .containsExactly("c", "d");
        // a 冷却窗 10s：t=9_999 仍排除，t=10_000 回归——只剩 b 排除
        assertThat(exclusion.filterCandidates(List.of("a", "b", "c"), 10_000L))
                .containsExactly("a", "c");
    }

    @Test
    void candidateOrderShouldBePreserved() {
        RetryHostExclusion exclusion = new RetryHostExclusion(30_000L);
        exclusion.recordFailure("c", 0);
        assertThat(exclusion.filterCandidates(List.of("a", "b", "c", "d", "e"), 1L))
                .containsExactly("a", "b", "d", "e"); // 序保持（路由权重序不动）
    }

    @Test
    void reFailureShouldExtendCooldown() {
        RetryHostExclusion exclusion = new RetryHostExclusion(1_000L);
        exclusion.recordFailure("a", 0);
        exclusion.recordFailure("a", 5_000L); // 再失败——冷却自 5s 重算
        // 双候选：a 冷却内排除，b 正常（不触发全排除回退）
        assertThat(exclusion.filterCandidates(List.of("a", "b"), 5_500L))
                .containsExactly("b");
        assertThat(exclusion.filterCandidates(List.of("a", "b"), 6_000L))
                .containsExactly("a", "b");
    }

    @Test
    void malformedInputsShouldFailFast() {
        RetryHostExclusion exclusion = new RetryHostExclusion();
        assertThatThrownBy(() -> new RetryHostExclusion(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryHostExclusion(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exclusion.recordFailure(null, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exclusion.recordFailure("a", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> exclusion.filterCandidates(null, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
