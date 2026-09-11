package io.github.chyuan_cuihongyuan.buzhou.core.experiment;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 505 / T761–T762：在线实验分桶——确定性（同键恒同组）、分布健全性、
 * 未入组余量、未知实验零状态、权重越界 fail-fast、曝光快照、yml 装配。
 */
class ExperimentBucketerTest {

    @Test
    void sameUnitKeyAlwaysSameVariant() {
        ExperimentBucketer bucketer = new ExperimentBucketer(
                Map.of("prompt-v2", Map.of("control", 50, "treatment", 50)));
        String first = bucketer.assign("prompt-v2", "app|sess-1");
        for (int i = 0; i < 20; i++) {
            assertThat(bucketer.assign("prompt-v2", "app|sess-1")).isEqualTo(first);
        }
        // 不同 unit 可不同组（分布健全性另行断言）
        assertThat(bucketer.variantsOf("prompt-v2")).containsExactly("control", "treatment");
    }

    @Test
    void distributionCoversVariantsAndLeavesUnenrolledRemainder() {
        ExperimentBucketer half = new ExperimentBucketer(
                Map.of("e50", Map.of("a", 50)));
        int enrolled = 0;
        for (int i = 0; i < 1000; i++) {
            if (half.assign("e50", "u" + i) != null) {
                enrolled++;
            }
        }
        // 50% 余量未入组——宽容差 ±15pp
        assertThat(enrolled).isBetween(350, 650);
        Map<String, Long> row = half.snapshot().get("e50");
        assertThat(row.get("__unenrolled__")).isNotNull();

        // 全量（100）必无未入组且两变体都有命中
        ExperimentBucketer full = new ExperimentBucketer(
                Map.of("e100", Map.of("a", 50, "b", 50)));
        long aHits = 0;
        long bHits = 0;
        for (int i = 0; i < 1000; i++) {
            String v = full.assign("e100", "u" + i);
            if ("a".equals(v)) {
                aHits++;
            } else {
                bHits++;
            }
        }
        assertThat(aHits).isBetween(300L, 700L);
        assertThat(bHits).isBetween(300L, 700L);
    }

    @Test
    void unknownExperimentAndBlankKeyReturnNull() {
        ExperimentBucketer bucketer = new ExperimentBucketer(
                Map.of("known", Map.of("a", 100)));
        assertThat(bucketer.assign("unknown", "u1")).isNull();
        assertThat(bucketer.assign("known", "")).isNull();
        assertThat(bucketer.assign("known", null)).isNull();
    }

    @Test
    void oversubscribedWeightsFailFast() {
        assertThatThrownBy(() -> new ExperimentBucketer(
                Map.of("bad", Map.of("a", 80, "b", 30))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("权重和超");
        assertThatThrownBy(() -> new ExperimentBucketer(
                Map.of("bad", Map.of("a", -5))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exposureSnapshotCountsAssignments() {
        ExperimentBucketer bucketer = new ExperimentBucketer(
                Map.of("exp", Map.of("a", 100)));
        bucketer.assign("exp", "u1");
        bucketer.assign("exp", "u1");
        bucketer.assign("exp", "u2");
        Map<String, Long> row = bucketer.snapshot().get("exp");
        assertThat(row.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(3);
    }

    @Test
    void ymlAssemblyOnlyWhenExperimentsDeclared() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config
                                .BuzhouCoreAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.experiments.prompt-v2.control=50",
                        "buzhou.experiments.prompt-v2.treatment=50")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouExperimentBucketer");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.core.config
                                .BuzhouCoreAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouExperimentBucketer");
                });
    }
}
