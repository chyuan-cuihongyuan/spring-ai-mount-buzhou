package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.QosClassifier.QosClass;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.QosClassifier.ResourceRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2019 / T3140：QoS 分级合同——三态、K8s 多维合成（任一零声明拉
 * 低整体 / 全维保额才 GUARANTEED）、驱逐序、让位判定、畸形 fail-fast。
 */
class QosClassifierTest {

    @Test
    void guaranteedWhenAllDimensionsRequestEqualsLimit() {
        assertThat(QosClassifier.classify(List.of(
                new ResourceRequest(10, 10),
                new ResourceRequest(100, 100)))).isEqualTo(QosClass.GUARANTEED);
    }

    @Test
    void bestEffortWhenAllZeroOrUndeclared() {
        assertThat(QosClassifier.classify(List.of())).isEqualTo(QosClass.BEST_EFFORT);
        assertThat(QosClassifier.classify(List.of(
                new ResourceRequest(0, 0),
                new ResourceRequest(0, 0)))).isEqualTo(QosClass.BEST_EFFORT);
    }

    @Test
    void burstableWhenSomeRoomBetweenRequestAndLimit() {
        assertThat(QosClassifier.classify(List.of(
                new ResourceRequest(5, 10)))).isEqualTo(QosClass.BURSTABLE);
    }

    @Test
    void mixedDimensionsShouldDegradeToBurstable() {
        // 一维 GUARANTEED + 一维零声明 → 整体 BURSTABLE（任一零声明拉低）
        assertThat(QosClassifier.classify(List.of(
                new ResourceRequest(10, 10),
                new ResourceRequest(0, 0)))).isEqualTo(QosClass.BURSTABLE);
        // 一维 GUARANTEED + 一维 BURSTABLE → 整体 BURSTABLE
        assertThat(QosClassifier.classify(List.of(
                new ResourceRequest(10, 10),
                new ResourceRequest(5, 10)))).isEqualTo(QosClass.BURSTABLE);
        // 纯上限声明（request=0, limit>0）也是 BURSTABLE——有上限无保底
        assertThat(QosClassifier.classify(List.of(
                new ResourceRequest(0, 10)))).isEqualTo(QosClass.BURSTABLE);
    }

    @Test
    void evictionRankShouldOrderBestEffortFirst() {
        assertThat(QosClassifier.evictionRank(QosClass.BEST_EFFORT)).isZero();
        assertThat(QosClassifier.evictionRank(QosClass.BURSTABLE)).isEqualTo(1);
        assertThat(QosClassifier.evictionRank(QosClass.GUARANTEED)).isEqualTo(2);
    }

    @Test
    void yieldDecisionShouldProtectGuaranteed() {
        assertThat(QosClassifier.shouldYield(QosClass.BEST_EFFORT, QosClass.GUARANTEED)).isTrue();
        assertThat(QosClassifier.shouldYield(QosClass.BURSTABLE, QosClass.GUARANTEED)).isTrue();
        assertThat(QosClassifier.shouldYield(QosClass.GUARANTEED, QosClass.GUARANTEED)).isFalse();
        assertThat(QosClassifier.shouldYield(QosClass.BEST_EFFORT, QosClass.BEST_EFFORT)).isFalse();
    }

    @Test
    void contradictoryOrNegativeDeclarationShouldFailFast() {
        assertThatThrownBy(() -> new ResourceRequest(10, 5)) // request > limit 矛盾
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("矛盾");
        assertThatThrownBy(() -> new ResourceRequest(-1, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResourceRequest(0, -5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QosClassifier.evictionRank(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
