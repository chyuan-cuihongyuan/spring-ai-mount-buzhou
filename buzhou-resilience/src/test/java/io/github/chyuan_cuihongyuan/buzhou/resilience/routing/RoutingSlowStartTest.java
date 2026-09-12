package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 路由慢启动爬坡测试（spec 702 / T955–T956 / impl 505）：floor 起步、手动
 * tick 逐步到位（零等待零抖动）、ramp 升级收敛、热重载上调走爬坡 / 下调瞬时、
 * 默认 null 零回归。
 */
class RoutingSlowStartTest {

    private final RoutingSlowStartTest.Stub a = new RoutingSlowStartTest.Stub("a");
    private final RoutingSlowStartTest.Stub b = new RoutingSlowStartTest.Stub("b");
    private final WeightedChatModel route = new WeightedChatModel(
            java.util.Map.of("a", (org.springframework.ai.chat.model.ChatModel) a,
                    "b", (org.springframework.ai.chat.model.ChatModel) b),
            java.util.Map.of("a", 3, "b", 1));
    /** 测试注入：调度器永不自动 tick——全手动推进，零线程竞态。 */
    private final ScheduledExecutorService idleScheduler = Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void tearDown() {
        idleScheduler.shutdownNow();
    }

    private RoutingSlowStart manualSlowStart() {
        // slowStart 任意（调度器为空转 idle——tick 全手动）
        return new RoutingSlowStart(route, Duration.ofHours(1), idleScheduler);
    }

    @Test
    void rampClimbsFloorToTargetInSteps() {
        RoutingSlowStart slowStart = manualSlowStart();
        try {
            slowStart.ramp("a", 8);
            assertThat(route.routes()).containsEntry("a", RoutingSlowStart.FLOOR_WEIGHT); // 立即落 floor
            assertThat(slowStart.ramps()).containsKey("a");

            for (int i = 0; i < RoutingSlowStart.STEPS; i++) {
                slowStart.tick();
            }
            assertThat(route.routes()).containsEntry("a", 8); // 终点恰 target
            assertThat(slowStart.ramps()).isEmpty(); // 完成即清
            // 中途单调不回退
        } finally {
            slowStart.close();
        }
    }

    @Test
    void intermediateStepsAreMonotonic() {
        RoutingSlowStart slowStart = manualSlowStart();
        try {
            slowStart.ramp("b", 9);
            int previous = RoutingSlowStart.FLOOR_WEIGHT;
            for (int i = 0; i < RoutingSlowStart.STEPS; i++) {
                slowStart.tick();
                int current = route.routes().get("b");
                assertThat(current).isGreaterThan(previous).isLessThanOrEqualTo(9);
                previous = current;
            }
            assertThat(previous).isEqualTo(9);
        } finally {
            slowStart.close();
        }
    }

    @Test
    void rampUpgradeConvergesToNewTarget() {
        RoutingSlowStart slowStart = manualSlowStart();
        try {
            slowStart.ramp("a", 4);
            slowStart.tick(); // 中途升级
            slowStart.ramp("a", 9);
            assertThat(slowStart.ramps().get("a").targetWeight()).isEqualTo(9);
            for (int i = 0; i < RoutingSlowStart.STEPS + 1; i++) {
                slowStart.tick();
            }
            assertThat(route.routes()).containsEntry("a", 9);
            assertThat(slowStart.ramps()).isEmpty();
        } finally {
            slowStart.close();
        }
    }

    @Test
    void upgradeBelowCurrentWeightLandsInstantly() {
        RoutingSlowStart slowStart = manualSlowStart();
        try {
            slowStart.ramp("a", 9);
            slowStart.tick();
            slowStart.tick(); // 已爬到中途
            int current = route.routes().get("a");
            slowStart.ramp("a", 2); // 降 target 且当前已超——瞬时降权到位
            assertThat(route.routes()).containsEntry("a", 2);
            assertThat(slowStart.ramps()).isEmpty();
            assertThat(current).isGreaterThan(2);
        } finally {
            slowStart.close();
        }
    }

    @Test
    void unknownRouteRejected() {
        RoutingSlowStart slowStart = manualSlowStart();
        try {
            assertThatThrownBy(() -> slowStart.ramp("ghost", 5))
                    .isInstanceOf(IllegalArgumentException.class);
        } finally {
            slowStart.close();
        }
    }

    @Test
    void hotReloadWeightIncreaseGoesThroughRamp() {
        RoutingSlowStart slowStart = manualSlowStart();
        try {
            MockEnvironment environment = new MockEnvironment()
                    .withProperty("buzhou.routing.weights.a", "8");
            RoutingWeightsHotReload reload =
                    new RoutingWeightsHotReload(route, environment, slowStart);
            reload.onApplicationEvent(
                    new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent("t"));

            assertThat(route.routes()).containsEntry("a", RoutingSlowStart.FLOOR_WEIGHT); // 爬坡起步而非 8
            assertThat(slowStart.ramps()).containsKey("a");
        } finally {
            slowStart.close();
        }
    }

    @Test
    void hotReloadWeightDecreaseIsInstant() {
        RoutingSlowStart slowStart = manualSlowStart();
        try {
            MockEnvironment environment = new MockEnvironment()
                    .withProperty("buzhou.routing.weights.a", "1");
            RoutingWeightsHotReload reload =
                    new RoutingWeightsHotReload(route, environment, slowStart);
            reload.onApplicationEvent(
                    new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent("t"));

            assertThat(route.routes()).containsEntry("a", 1); // 降权瞬时
            assertThat(slowStart.ramps()).isEmpty();
        } finally {
            slowStart.close();
        }
    }

    @Test
    void nullSlowStartKeepsLegacyBehavior() {
        AtomicInteger ticks = new AtomicInteger();
        ScheduledExecutorService idle = Executors.newSingleThreadScheduledExecutor();
        try {
            MockEnvironment environment = new MockEnvironment()
                    .withProperty("buzhou.routing.weights.a", "8");
            RoutingWeightsHotReload reload =
                    new RoutingWeightsHotReload(route, environment);
            reload.onApplicationEvent(
                    new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent("t"));
            assertThat(route.routes()).containsEntry("a", 8); // 瞬时到位——既有语义
            assertThat(ticks.get()).isZero();
        } finally {
            idle.shutdownNow();
        }
    }

    static final class Stub implements org.springframework.ai.chat.model.ChatModel {
        private final String text;

        Stub(String text) {
            this.text = text;
        }

        @Override
        public org.springframework.ai.chat.model.ChatResponse call(
                org.springframework.ai.chat.prompt.Prompt prompt) {
            return new org.springframework.ai.chat.model.ChatResponse(
                    java.util.List.of(new org.springframework.ai.chat.model.Generation(
                            new org.springframework.ai.chat.messages.AssistantMessage(text))));
        }

        @Override
        public reactor.core.publisher.Flux<org.springframework.ai.chat.model.ChatResponse> stream(
                org.springframework.ai.chat.prompt.Prompt prompt) {
            return reactor.core.publisher.Flux.just(call(prompt));
        }
    }
}
