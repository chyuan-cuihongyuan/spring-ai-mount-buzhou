package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-676 / spec 923：扩缩容建议缩容滞回——默认 1 立即回落零回归、滞回 N=2
 * 首个空闲窗保持/连续两窗回落、扩容路径即时、参数校验。
 *
 * <p>拒绝制造：agent 限 1 → acquire 占住唯一许可后，再次 acquire 抛
 * QUOTA_EXCEEDED（recordRejection 计数 +1）。
 */
class ScalingHysteresisTest {

    /** 制造一次拒绝：占住许可后再 acquire（抛 QUOTA_EXCEEDED → 拒绝计数 +1）。 */
    private static void rejectOnce(AgentBulkhead bulkhead, String agent) {
        try (AgentBulkhead.Lease lease = bulkhead.acquire(agent)) { // 占住唯一许可
            try {
                bulkhead.acquire(agent); // 再取 → QUOTA_EXCEEDED（拒绝计数 +1）
            } catch (RuntimeException expected) {
                // 预期拒绝
            }
        } // close 释放许可——下次 rejectOnce 可重复
    }

    @Test
    void defaultStabilizeOneKeepsImmediateFallback() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("agent-a", 1), Duration.ofMillis(1));
        BulkheadScalingAdvisor advisor = new BulkheadScalingAdvisor(bulkhead, 1, 4);
        rejectOnce(bulkhead, "agent-a");
        // 产生拒绝 → 建议 >1
        assertThat(advisor.advise().get("agent-a").suggestedMultiplier()).isGreaterThan(1);
        // 拒绝回零（无新拒绝）→ 立即回落 1（默认滞回=1 既有语义）
        Map<String, BulkheadScalingAdvisor.Advice> down = advisor.advise();
        assertThat(down.get("agent-a").suggestedMultiplier()).isEqualTo(1);
    }

    @Test
    void hysteresisHoldsThenFallsAfterStableWindows() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("agent-a", 1), Duration.ofMillis(1));
        BulkheadScalingAdvisor advisor = new BulkheadScalingAdvisor(bulkhead, 1, 4, 2);
        // 窗 1：拒绝 → 扩容建议 2
        rejectOnce(bulkhead, "agent-a");
        assertThat(advisor.advise().get("agent-a").suggestedMultiplier()).isEqualTo(2);
        // 窗 2：空闲——滞回保持（不立即回落）
        assertThat(advisor.advise().get("agent-a").suggestedMultiplier()).isEqualTo(2);
        // 窗 3：再空闲——连续 2 窗满足 → 回落 1
        assertThat(advisor.advise().get("agent-a").suggestedMultiplier()).isEqualTo(1);
    }

    @Test
    void scaleUpPathStaysImmediate() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("agent-a", 1), Duration.ofMillis(1));
        BulkheadScalingAdvisor advisor = new BulkheadScalingAdvisor(bulkhead, 1, 4, 2);
        // 窗 1：拒绝 → 扩容
        rejectOnce(bulkhead, "agent-a");
        assertThat(advisor.advise().get("agent-a").suggestedMultiplier()).isEqualTo(2);
        // 窗 2：空闲 → 滞回保持 2
        assertThat(advisor.advise().get("agent-a").suggestedMultiplier()).isEqualTo(2);
        // 窗 3：又有拒绝 → 扩容即时响应（3 = 1 + 2 拒绝/阈值 1），不等滞回
        rejectOnce(bulkhead, "agent-a");
        rejectOnce(bulkhead, "agent-a");
        assertThat(advisor.advise().get("agent-a").suggestedMultiplier()).isEqualTo(3);
    }

    @Test
    void stabilizeWindowsValidated() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("agent-a", 1), Duration.ofMillis(1));
        assertThatThrownBy(() -> new BulkheadScalingAdvisor(bulkhead, 1, 4, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
