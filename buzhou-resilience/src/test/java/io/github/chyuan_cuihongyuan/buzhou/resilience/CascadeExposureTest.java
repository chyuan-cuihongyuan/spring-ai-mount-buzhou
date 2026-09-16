package io.github.chyuan_cuihongyuan.buzhou.resilience;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1875 / T2952：级联暴露——风险权重、活/静风险、最脆边。 */
class CascadeExposureTest {

    /** 风险权重 = 流量占比×失败率；高权重边即最脆边。 */
    @Test
    void shouldRankEdgesByRiskWeight() {
        CascadeExposure.Exposure exposure = CascadeExposure.analyze(
                List.of(new CascadeExposure.Edge("api", "db", 0.8, 0.5),
                        new CascadeExposure.Edge("api", "cache", 0.2, 0.1)),
                List.of(new CascadeExposure.NodeHealth("db", true),
                        new CascadeExposure.NodeHealth("cache", true)));
        // 权重 0.4 vs 0.02——db 边最脆（静风险：下游健康但权重高）
        assertThat(exposure.worstEdge()).isEqualTo("api->db");
        assertThat(exposure.worstWeight()).isEqualTo(0.4d);
        assertThat(exposure.totalWeight()).isCloseTo(0.42d, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(exposure.activeRiskyEdges()).isZero(); // 下游全健康
        assertThat(exposure.activeRatio()).isZero();
    }

    /** 活风险：下游不健康且权重 >0 即正在传导。 */
    @Test
    void unhealthyDownstreamActivatesRisk() {
        CascadeExposure.Exposure exposure = CascadeExposure.analyze(
                List.of(new CascadeExposure.Edge("api", "db", 0.8, 0.5),
                        new CascadeExposure.Edge("api", "cache", 0.2, 0.1)),
                List.of(new CascadeExposure.NodeHealth("db", false),
                        new CascadeExposure.NodeHealth("cache", true)));
        assertThat(exposure.activeRiskyEdges()).isEqualTo(1);
        assertThat(exposure.activeRatio()).isEqualTo(0.5d);
    }

    /** 空表哨兵与并列取首。 */
    @Test
    void sentinelsAndTies() {
        assertThat(CascadeExposure.analyze(List.of(), List.of()).activeRatio())
                .isEqualTo(-1d);
        assertThat(CascadeExposure.analyze(null, null).worstEdge()).isNull();
        CascadeExposure.Exposure tie = CascadeExposure.analyze(
                List.of(new CascadeExposure.Edge("a", "x", 0.5, 0.2),
                        new CascadeExposure.Edge("b", "y", 0.2, 0.5)),
                List.of());
        assertThat(tie.worstEdge()).isEqualTo("a->x"); // 权重同 0.1 取首
    }

    /** 畸形入参 fail-fast：空白节点、比率越界/NaN。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> new CascadeExposure.Edge("", "b", 0.5, 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法依赖边");
        assertThatThrownBy(() -> new CascadeExposure.Edge("a", "b", 1.5, 0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CascadeExposure.Edge("a", "b", 0.5, Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CascadeExposure.NodeHealth(" ", true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
