package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 金丝雀过滤×慢启动×热重载联动补验（spec 735 / T1021–T1022 / impl 538）：
 * filter→构造→热调升配 ramp→tick 到位编排闭环。
 */
class StagesSlowStartE2ETest {

    static final class Stub implements org.springframework.ai.chat.model.ChatModel {
        @Override
        public org.springframework.ai.chat.model.ChatResponse call(
                org.springframework.ai.chat.prompt.Prompt prompt) {
            return new org.springframework.ai.chat.model.ChatResponse(java.util.List.of());
        }

        @Override
        public reactor.core.publisher.Flux<org.springframework.ai.chat.model.ChatResponse> stream(
                org.springframework.ai.chat.prompt.Prompt prompt) {
            return reactor.core.publisher.Flux.empty();
        }
    }

    @Test
    void orchestrationFilterThenRampThenConverge() {
        Stub a = new Stub();
        Stub b = new Stub();
        Stub archived = new Stub();

        // ① 阶段过滤：archived 剔除出候选（即使权重表里写着 9）
        RouteStages stages = new RouteStages();
        stages.tag("primary", RouteStages.Stage.STABLE);
        stages.tag("canary", RouteStages.Stage.CANARY);
        stages.tag("old", RouteStages.Stage.ARCHIVED);
        Map<String, Integer> visibleWeights = RouteStages.filter(
                Map.of("primary", 3, "canary", 1, "old", 9), stages,
                Set.of(RouteStages.Stage.STABLE, RouteStages.Stage.CANARY));
        assertThat(visibleWeights).containsOnlyKeys("primary", "canary");

        // ② 过滤后的候选构造路由（weightedChatModel bean 同构）
        WeightedChatModel route = new WeightedChatModel(
                Map.of("primary", a, "canary", b), visibleWeights);

        // ③ 慢启动挂热重载：上调走 ramp
        ScheduledExecutorService idle = Executors.newSingleThreadScheduledExecutor();
        try {
            RoutingSlowStart slowStart = new RoutingSlowStart(route, Duration.ofHours(1), idle);
            MockEnvironment env = new MockEnvironment()
                    .withProperty("buzhou.routing.weights.canary", "8");
            RoutingWeightsHotReload reload = new RoutingWeightsHotReload(route, env, slowStart);
            reload.onApplicationEvent(
                    new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent("t"));

            assertThat(route.routes()).containsEntry("canary", 1); // floor 起步
            for (int i = 0; i < RoutingSlowStart.STEPS; i++) {
                slowStart.tick();
            }
            assertThat(route.routes()).containsEntry("canary", 8); // 逐步到位

            // ④ archived 始终不在路由候选（weights 里有 9 也没用）
            assertThat(route.routes()).doesNotContainKey("old");
        } finally {
            idle.shutdownNow();
        }
    }
}
