package io.github.chyuan_cuihongyuan.buzhou.resilience.routing;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 340 / impl-363：路由权重热调回归——setWeight 翻转分布（WRR 动量
 * 保留自然收敛）/未知名拒/routes 同步/refresh 事件重读生效/面外跳过。
 */
class RoutingWeightsHotReloadTest {

    static final class Stub implements ChatModel {
        final String text;
        final List<String> served = new CopyOnWriteArrayList<>();

        Stub(String text) {
            this.text = text;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            served.add("call");
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    private static WeightedChatModel router(Stub a, Stub b) {
        return new WeightedChatModel(
                Map.of("a", (ChatModel) a, "b", (ChatModel) b),
                Map.of("a", 3, "b", 1));
    }

    @Test
    void setWeightFlipsDistribution_momentumPreserved() {
        Stub a = new Stub("a");
        Stub b = new Stub("b");
        WeightedChatModel route = router(a, b);
        for (int i = 0; i < 4; i++) { // 3:1 窗口
            route.call(new Prompt(List.of()));
        }
        route.setWeight("a", 1);
        route.setWeight("b", 3);
        assertThat(route.routes()).containsEntry("a", 1).containsEntry("b", 3); // 观测同步
        for (int i = 0; i < 8; i++) { // 新窗口 1:3
            route.call(new Prompt(List.of()));
        }
        // 12 次总分布：老窗口 3a:1b + 新窗口趋 2a:6b（动量过渡）——b 显著反超
        assertThat(b.served.size()).isGreaterThan(a.served.size());
    }

    @Test
    void unknownNameRejected() {
        Stub a = new Stub("a");
        Stub b = new Stub("b");
        WeightedChatModel route = router(a, b);
        assertThatThrownBy(() -> route.setWeight("ghost", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost");
    }

    @Test
    void refreshEventRereadsWeights_skipsUnknownNames() {
        Stub a = new Stub("a");
        Stub b = new Stub("b");
        WeightedChatModel route = router(a, b);
        org.springframework.mock.env.MockEnvironment environment =
                new org.springframework.mock.env.MockEnvironment()
                        .withProperty("buzhou.routing.weights.a", "1")
                        .withProperty("buzhou.routing.weights.b", "3")
                        .withProperty("buzhou.routing.weights.faceless", "9"); // 面外——跳过
        RoutingWeightsHotReload reload = new RoutingWeightsHotReload(route, environment);

        reload.onApplicationEvent(
                new io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigRefreshEvent("test"));

        assertThat(route.routes()).containsEntry("a", 1).containsEntry("b", 3)
                .doesNotContainKey("faceless"); // 面外路未加入（候选面定死）
        assertThat(reload.reloadCount()).isEqualTo(1);
    }
}
