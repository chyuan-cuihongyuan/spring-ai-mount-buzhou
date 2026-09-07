package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 320 / impl-343：舱容量热调整回归——扩容放行（在飞不受扰）/缩容拒新直到
 * 释放到限内/热加 agent/热移除回 NOOP/拒绝计数保留。
 */
class AgentBulkheadResizeTest {

    @Test
    void growAdmitsMoreWithoutDisturbingInFlight() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("a", 1), Duration.ZERO);
        AgentBulkhead.Lease first = bulkhead.acquire("a"); // 占满旧限
        bulkhead.resize(Map.of("a", 3));
        try (AgentBulkhead.Lease second = bulkhead.acquire("a");
                AgentBulkhead.Lease third = bulkhead.acquire("a")) {
            assertThat(bulkhead.inFlight("a"))
                    .as("扩容补 permit——在飞 1 + 新 2 = 3").isEqualTo(3);
        }
        first.close();
    }

    @Test
    void shrinkBlocksNewUntilInFlightDropsBelowLimit() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("a", 3), Duration.ZERO);
        AgentBulkhead.Lease first = bulkhead.acquire("a");
        AgentBulkhead.Lease second = bulkhead.acquire("a");
        bulkhead.resize(Map.of("a", 1));
        assertThat(bulkhead.inFlight("a"))
                .as("缩容不抢占——在飞 2 瞬时超新限 1").isEqualTo(2);
        assertThatThrownBy(() -> bulkhead.acquire("a"))
                .as("在飞 2 ≥ 新限 1——拒新").isInstanceOf(BuzhouException.class);
        second.close();
        assertThatThrownBy(() -> bulkhead.acquire("a"))
                .as("在飞 1 = 新限 1——仍拒（须低于限才放）")
                .isInstanceOf(BuzhouException.class);
        first.close();
        try (AgentBulkhead.Lease admitted = bulkhead.acquire("a")) {
            assertThat(bulkhead.inFlight("a")).isEqualTo(1); // 收敛后正常放行
        }
    }

    @Test
    void hotAddAgentStartsBulkhead() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of(), Duration.ZERO);
        assertThat(bulkhead.acquire("b")).isSameAs(AgentBulkhead.Lease.NOOP); // 未配 = NOOP
        bulkhead.resize(Map.of("b", 2));
        try (AgentBulkhead.Lease lease = bulkhead.acquire("b")) {
            assertThat(bulkhead.inFlight("b")).isEqualTo(1);
            assertThat(bulkhead.limitOf("b")).isEqualTo(2);
        }
    }

    @Test
    void hotRemoveAgentFallsBackToNoop() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("a", 1), Duration.ZERO);
        AgentBulkhead.Lease inFlight = bulkhead.acquire("a");
        bulkhead.resize(Map.of());
        try (AgentBulkhead.Lease noop = bulkhead.acquire("a")) {
            assertThat(noop).isSameAs(AgentBulkhead.Lease.NOOP); // 摘舱回 NOOP
        }
        assertThat(bulkhead.inFlight("a")).isZero();
        assertThat(bulkhead.limitOf("a")).isEqualTo(Integer.MAX_VALUE);
        inFlight.close(); // 释放到已摘对象——无害
    }

    @Test
    void rejectionCountersSurviveResize() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("a", 1), Duration.ZERO);
        try (AgentBulkhead.Lease lease = bulkhead.acquire("a")) {
            for (int i = 0; i < 2; i++) {
                assertThatThrownBy(() -> bulkhead.acquire("a"))
                        .isInstanceOf(BuzhouException.class);
            }
        }
        bulkhead.resize(Map.of("a", 2));
        assertThat(bulkhead.topRejections(5))
                .as("拒绝计数单调不清零——伸缩建议器窗口增量依赖（spec 319）")
                .containsExactly(Map.entry("a", 2L));
    }
}
