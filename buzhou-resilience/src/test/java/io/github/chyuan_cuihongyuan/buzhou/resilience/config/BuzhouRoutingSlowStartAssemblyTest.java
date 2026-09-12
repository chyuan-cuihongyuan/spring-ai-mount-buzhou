package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import io.github.chyuan_cuihongyuan.buzhou.resilience.routing.RoutingSlowStart;
import io.github.chyuan_cuihongyuan.buzhou.resilience.routing.RoutingWeightsHotReload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 路由慢启动 yml 装配测试（spec 725 / T1001–T1002 / impl 528）：properties
 * 三态绑定 + 热重载 ObjectProvider 接线直调（声明 → ramp 生效、缺省 → 瞬时）。
 */
class BuzhouRoutingSlowStartAssemblyTest {

    @Test
    void propertiesBindingThreeStates() {
        assertThat(new BuzhouRoutingProperties(Map.of("a", 1, "b", 2)).slowStart()).isNull();

        BuzhouRoutingProperties declared = new BuzhouRoutingProperties(
                Map.of("a", 1, "b", 2), Duration.ofSeconds(30));
        assertThat(declared.slowStart()).isEqualTo(Duration.ofSeconds(30));
        assertThat(declared.routingConfigured()).isTrue();

        assertThatThrownBy(() -> new BuzhouRoutingProperties(
                Map.of("a", 1, "b", 2), Duration.ofSeconds(-1)))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException.class);
    }

    @Test
    void hotReloadWiresSlowStartWhenPresent() {
        BuzhouRoutingSlowStartAssemblyTest.Stub a = new BuzhouRoutingSlowStartAssemblyTest.Stub();
        BuzhouRoutingSlowStartAssemblyTest.Stub b = new BuzhouRoutingSlowStartAssemblyTest.Stub();
        io.github.chyuan_cuihongyuan.buzhou.resilience.routing.WeightedChatModel route =
                new io.github.chyuan_cuihongyuan.buzhou.resilience.routing.WeightedChatModel(
                        Map.of("a", a, "b", b), Map.of("a", 3, "b", 1));
        MockEnvironment env = new MockEnvironment()
                .withProperty("buzhou.routing.weights.a", "8");

        RoutingSlowStart slowStart = new RoutingSlowStart(route, Duration.ofHours(1));
        try {
            RoutingWeightsHotReload reload = new RoutingWeightsHotReload(route, env, slowStart);
            reload.onApplicationEvent(new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent("t"));

            assertThat(route.routes()).containsEntry("a", 1); // 爬坡起步（floor=1）
        } finally {
            slowStart.close();
        }
    }

    @Test
    void absentSlowStartObjectProviderFallsBackToInstant() {
        BuzhouRoutingSlowStartAssemblyTest.Stub a = new BuzhouRoutingSlowStartAssemblyTest.Stub();
        BuzhouRoutingSlowStartAssemblyTest.Stub b = new BuzhouRoutingSlowStartAssemblyTest.Stub();
        io.github.chyuan_cuihongyuan.buzhou.resilience.routing.WeightedChatModel route =
                new io.github.chyuan_cuihongyuan.buzhou.resilience.routing.WeightedChatModel(
                        Map.of("a", a, "b", b), Map.of("a", 3, "b", 1));
        MockEnvironment env = new MockEnvironment()
                .withProperty("buzhou.routing.weights.a", "8");

        // ObjectProvider 缺席语义：getIfAvailable() == null
        ObjectProvider<RoutingSlowStart> emptyProvider = new ObjectProvider<>() {
            @Override
            public RoutingSlowStart getObject(Object... args) {
                return null;
            }

            @Override
            public RoutingSlowStart getObject() {
                return null;
            }

            @Override
            public RoutingSlowStart getIfAvailable() {
                return null;
            }

            @Override
            public RoutingSlowStart getIfUnique() {
                return null;
            }
        };
        RoutingWeightsHotReload reload = new RoutingWeightsHotReload(route, env,
                emptyProvider.getIfAvailable());
        reload.onApplicationEvent(new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent("t"));

        assertThat(route.routes()).containsEntry("a", 8); // 瞬时到位——缺省零变化
    }

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
}
