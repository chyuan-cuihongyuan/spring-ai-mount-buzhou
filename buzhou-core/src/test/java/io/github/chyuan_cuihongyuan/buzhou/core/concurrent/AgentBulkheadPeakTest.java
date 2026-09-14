package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1417 / T2136：舱壁在飞峰值水位——峰值随并发爬升单调、释放后不回退、
 * 饱和度=峰值/上限（打满=1.0）、无限舱 -1 哨兵、拒绝不影响峰值。
 */
class AgentBulkheadPeakTest {

    @Test
    void peakTracksMaxObservedConcurrencyAndNeverFallsBack() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("ag", 5), Duration.ZERO);
        // 串行占两席：峰值应到 2
        AgentBulkhead.Lease l1 = bulkhead.acquire("ag");
        AgentBulkhead.Lease l2 = bulkhead.acquire("ag");
        assertThat(bulkhead.peakInFlight("ag")).isEqualTo(2);
        l1.close();
        assertThat(bulkhead.peakInFlight("ag")).isEqualTo(2); // 释放不回退
        l2.close();
        // 再占一席：峰值仍 2
        AgentBulkhead.Lease l3 = bulkhead.acquire("ag");
        assertThat(bulkhead.peakInFlight("ag")).isEqualTo(2);
        l3.close();
    }

    @Test
    void saturationIsPeakDividedByLimit() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("ag", 4), Duration.ZERO);
        AgentBulkhead.Lease l1 = bulkhead.acquire("ag");
        AgentBulkhead.Lease l2 = bulkhead.acquire("ag");
        assertThat(bulkhead.peakSaturation("ag")).isEqualTo(0.5d);
        l1.close();
        l2.close();
        assertThat(bulkhead.peakSaturation("ag")).isEqualTo(0.5d);
    }

    @Test
    void unlimitedAgentReportsSentinelAndZeroPeak() {
        AgentBulkhead bulkhead = AgentBulkhead.unlimited();
        assertThat(bulkhead.peakInFlight("never-configured")).isZero();
        assertThat(bulkhead.peakSaturation("never-configured")).isEqualTo(-1d);
    }

    @Test
    void rejectionDoesNotDistortPeak() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("hot", 1), Duration.ZERO);
        AgentBulkhead.Lease lease = bulkhead.acquire("hot");
        // 第二席被拒（QUOTA_EXCEEDED）——峰值仍 1，不因拒绝虚高
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> bulkhead.acquire("hot"))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException.class);
        assertThat(bulkhead.peakInFlight("hot")).isEqualTo(1);
        assertThat(bulkhead.peakSaturation("hot")).isEqualTo(1.0d); // 曾打满
        lease.close();
    }
}
