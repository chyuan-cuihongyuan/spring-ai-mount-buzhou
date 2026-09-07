package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 319 / impl-342：舱压伸缩建议回归——驱动真实舱制造拒绝（不 mock 计数）：
 * 拒绝升倍/回零回落/钳制/多 agent 各自建议/无拒绝历史空建议/构造校验。
 */
class BulkheadScalingAdvisorTest {

    /** 占满名额制造 count 次真拒绝（QUOTA_EXCEEDED fail-fast）。 */
    private static void forceRejections(AgentBulkhead bulkhead, String agent, int count) {
        for (int i = 0; i < count; i++) {
            assertThatThrownBy(() -> bulkhead.acquire(agent))
                    .isInstanceOf(BuzhouException.class);
        }
    }

    @Test
    void suggestsMultiplierFromWindowRejections() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("a", 1), Duration.ZERO);
        try (AgentBulkhead.Lease lease = bulkhead.acquire("a")) {
            forceRejections(bulkhead, "a", 3);
        }
        BulkheadScalingAdvisor advisor = new BulkheadScalingAdvisor(bulkhead, 2, 3);
        Map<String, BulkheadScalingAdvisor.Advice> advice = advisor.advise();
        assertThat(advice.get("a").windowRejections()).isEqualTo(3);
        assertThat(advice.get("a").suggestedMultiplier())
                .as("1 + 3/2 = 2（HPA 阈值比整数简化）").isEqualTo(2);
        assertThat(advisor.scaleUpAdviceCount()).isEqualTo(1);
        assertThat(advisor.scaleDownAdviceCount()).isZero();
    }

    @Test
    void fallsBackToOneWhenWindowClean() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("a", 1), Duration.ZERO);
        try (AgentBulkhead.Lease lease = bulkhead.acquire("a")) {
            forceRejections(bulkhead, "a", 2);
        }
        BulkheadScalingAdvisor advisor = new BulkheadScalingAdvisor(bulkhead, 1, 3);
        assertThat(advisor.advise().get("a").suggestedMultiplier()).isEqualTo(3); // 1 + 2
        Map<String, BulkheadScalingAdvisor.Advice> second = advisor.advise();
        assertThat(second.get("a").windowRejections()).isZero();
        assertThat(second.get("a").suggestedMultiplier())
                .as("窗口拒绝回零——回落 1（HPA minReplicas 语义，不为 0）").isEqualTo(1);
        assertThat(advisor.scaleUpAdviceCount()).isEqualTo(1);
        assertThat(advisor.scaleDownAdviceCount()).isEqualTo(1);
        assertThat(advisor.lastAdvice().get("a").suggestedMultiplier())
                .as("lastAdvice 不结新窗口").isEqualTo(1);
    }

    @Test
    void clampsToMaxMultiplier() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("a", 1), Duration.ZERO);
        try (AgentBulkhead.Lease lease = bulkhead.acquire("a")) {
            forceRejections(bulkhead, "a", 10);
        }
        BulkheadScalingAdvisor advisor = new BulkheadScalingAdvisor(bulkhead, 1, 2);
        assertThat(advisor.advise().get("a").suggestedMultiplier())
                .as("1 + 10/1 = 11 钳到 max=2——拒绝风暴不翻译成离谱倍率").isEqualTo(2);
    }

    @Test
    void advisesEachAgentIndependently() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("a", 1, "b", 1), Duration.ZERO);
        try (AgentBulkhead.Lease la = bulkhead.acquire("a");
                AgentBulkhead.Lease lb = bulkhead.acquire("b")) {
            forceRejections(bulkhead, "a", 2);
            forceRejections(bulkhead, "b", 5);
        }
        BulkheadScalingAdvisor advisor = new BulkheadScalingAdvisor(bulkhead, 2, 3);
        Map<String, BulkheadScalingAdvisor.Advice> advice = advisor.advise();
        assertThat(advice.get("a").suggestedMultiplier()).isEqualTo(2); // 1 + 2/2
        assertThat(advice.get("b").suggestedMultiplier()).isEqualTo(3); // 1 + 5/2
        assertThat(advice).containsOnlyKeys("a", "b");
    }

    @Test
    void emptyAdviceWhenNoRejectionHistory() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("a", 1), Duration.ZERO);
        try (AgentBulkhead.Lease lease = bulkhead.acquire("a")) {
            // 成功执行不拒——隐式建议 1 无需出建议面
        }
        BulkheadScalingAdvisor advisor = new BulkheadScalingAdvisor(bulkhead, 2, 3);
        assertThat(advisor.advise()).isEmpty();
        assertThat(advisor.lastAdvice()).isEmpty();
        assertThat(advisor.scaleUpAdviceCount()).isZero();
        assertThat(advisor.scaleDownAdviceCount()).isZero();
    }

    @Test
    void constructorValidates() {
        AgentBulkhead bulkhead = AgentBulkhead.unlimited();
        assertThatThrownBy(() -> new BulkheadScalingAdvisor(null, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BulkheadScalingAdvisor(bulkhead, 0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BulkheadScalingAdvisor(bulkhead, 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
