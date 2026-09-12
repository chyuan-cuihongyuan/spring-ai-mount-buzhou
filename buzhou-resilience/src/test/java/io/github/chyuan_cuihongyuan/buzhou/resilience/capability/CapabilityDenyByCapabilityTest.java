package io.github.chyuan_cuihongyuan.buzhou.resilience.capability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 746 / T1094–T1095：能力维度 deny 聚合——vision vs tools 分布。
 */
class CapabilityDenyByCapabilityTest {

    @Test
    void denyByCapabilityAggregatesAcrossModels() {
        CapabilityDecisionAudit audit = new CapabilityDecisionAudit();
        audit.recordDeny("m1", "vision");
        audit.recordDeny("m2", "vision");
        audit.recordDeny("m3", "tools");
        CapabilityDecisionAudit.Report report = audit.snapshot();
        assertThat(report.denyByCapability()).containsEntry("vision", 2L).containsEntry("tools", 1L);
        // 与 denyByModel 并存（两维各自聚合）
        assertThat(report.denyByModel()).containsEntry("m1", 1L).containsEntry("m2", 1L);
    }
}
