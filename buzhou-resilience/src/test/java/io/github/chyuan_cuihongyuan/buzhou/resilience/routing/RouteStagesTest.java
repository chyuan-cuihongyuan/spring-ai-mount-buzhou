package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 路由阶段标签测试（spec 723 / T997–T998 / impl 526）：三态过滤、条件可见、
 * 未标注保留、cap、不可变。
 */
class RouteStagesTest {

    private static Map<String, Integer> weights() {
        return Map.of("stable-a", 5, "canary-b", 1, "old-c", 2);
    }

    @Test
    void filterDropsNonVisibleStages() {
        RouteStages stages = new RouteStages();
        stages.tag("stable-a", RouteStages.Stage.STABLE);
        stages.tag("canary-b", RouteStages.Stage.CANARY);
        stages.tag("old-c", RouteStages.Stage.ARCHIVED);

        Map<String, Integer> visible = RouteStages.filter(weights(), stages,
                Set.of(RouteStages.Stage.STABLE));

        assertThat(visible).containsOnlyKeys("stable-a");
        assertThat(visible).containsEntry("stable-a", 5);
    }

    @Test
    void canaryVisibleWhenStageAllowed() {
        RouteStages stages = new RouteStages();
        stages.tag("stable-a", RouteStages.Stage.STABLE);
        stages.tag("canary-b", RouteStages.Stage.CANARY);
        stages.tag("old-c", RouteStages.Stage.ARCHIVED);

        Map<String, Integer> visible = RouteStages.filter(weights(), stages,
                Set.of(RouteStages.Stage.STABLE, RouteStages.Stage.CANARY));

        assertThat(visible).containsOnlyKeys("stable-a", "canary-b");
    }

    @Test
    void untaggedRoutesAlwaysKept() {
        RouteStages stages = new RouteStages(); // 无任何标注
        stages.tag("old-c", RouteStages.Stage.ARCHIVED);

        Map<String, Integer> visible = RouteStages.filter(weights(), stages,
                Set.of(RouteStages.Stage.STABLE));

        assertThat(visible).containsOnlyKeys("stable-a", "canary-b"); // 未标注保留
    }

    @Test
    void allFilteredYieldsEmptyMapNotThrow() {
        RouteStages stages = new RouteStages();
        weights().keySet().forEach(k -> stages.tag(k, RouteStages.Stage.ARCHIVED));

        assertThat(RouteStages.filter(weights(), stages, Set.of(RouteStages.Stage.STABLE)))
                .isEmpty();
    }

    @Test
    void tagCapAndImmutableView() {
        RouteStages stages = new RouteStages();
        for (int i = 0; i < RouteStages.CAP; i++) {
            assertThat(stages.tag("r-" + i, RouteStages.Stage.STABLE)).isTrue();
        }
        assertThat(stages.tag("r-overflow", RouteStages.Stage.STABLE)).isFalse(); // 拒绝
        assertThat(stages.tag("r-0", RouteStages.Stage.CANARY)).isTrue(); // 幂等覆盖

        assertThat(stages.view()).isUnmodifiable();
        assertThat(stages.tag(null, RouteStages.Stage.STABLE)).isFalse(); // 前置校验兜底
        assertThat(stages.tag("", RouteStages.Stage.STABLE)).isFalse();
    }

    @Test
    void stageOfUnknownIsNull() {
        RouteStages stages = new RouteStages();
        assertThat(stages.stageOf("ghost")).isNull();
        assertThat(RouteStages.filter(weights(), stages, Set.of()))
                .containsOnlyKeys("stable-a", "canary-b", "old-c"); // 未标注全保留（零变化）
    }
}
