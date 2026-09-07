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
 * spec 339 / impl-362：加权路由回归——3:1 八次恰 6:2（smooth WRR 确定性）/
 * stream 同路选取 / 单候选拒绝 / 权重非法红 / 观测面。
 */
class WeightedChatModelTest {

    private static final class StubModel implements ChatModel {
        private final String text;
        final List<String> served = new CopyOnWriteArrayList<>();

        StubModel(String text) {
            this.text = text;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            served.add("call");
            return response(text);
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            served.add("stream");
            return Flux.just(response(text));
        }
    }

    private static ChatResponse response(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
    }

    @Test
    void threeToOneDistributionExactAndSmooth() {
        StubModel cheap = new StubModel("cheap");
        StubModel strong = new StubModel("strong");
        WeightedChatModel router = new WeightedChatModel(
                Map.of("cheap-model", (ChatModel) cheap, "strong-model", (ChatModel) strong),
                Map.of("cheap-model", 3, "strong-model", 1));

        StringBuilder sequence = new StringBuilder();
        java.util.List<String> picks = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            ChatResponse response = router.call(new Prompt(List.of()));
            String text = response.getResult().getOutput().getText();
            picks.add(text);
            sequence.append(text).append(';');
        }
        // 比例精确（6:2——served 列表为事实源）且时间平滑（不连四爆发）
        assertThat(cheap.served).hasSize(6);
        assertThat(strong.served).hasSize(2);
        assertThat(String.join(",", picks)).doesNotContain("cheap,cheap,cheap,cheap");
    }

    @Test
    void streamUsesSameRoutingPath() {
        StubModel a = new StubModel("a");
        StubModel b = new StubModel("b");
        WeightedChatModel router = new WeightedChatModel(
                Map.of("a", (ChatModel) a, "b", (ChatModel) b),
                Map.of("a", 1, "b", 1));

        List<String> streamed = router.stream(new Prompt(List.of()))
                .map(r -> r.getResult().getOutput().getText())
                .collectList().block();
        assertThat(streamed).hasSize(1);
        // 恰一路真实服务了 stream（1:1 首选不假设落谁——以 served 事实源判定）
        long streamServers = a.served.stream().filter("stream"::equals).count()
                + b.served.stream().filter("stream"::equals).count();
        assertThat(streamServers).isEqualTo(1);
        assertThat(streamed.get(0)).isEqualTo(a.served.contains("stream") ? "a" : "b");
    }

    @Test
    void singleCandidateRejected_nonPositiveWeightRejected() {
        StubModel a = new StubModel("a");
        assertThatThrownBy(() -> new WeightedChatModel(
                Map.of("only", (ChatModel) a), Map.of("only", 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("至少两路");
        StubModel b = new StubModel("b");
        assertThatThrownBy(() -> new WeightedChatModel(
                Map.of("a", (ChatModel) a, "b", (ChatModel) b), Map.of("a", 0, "b", 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("正整数");
        assertThatThrownBy(() -> new WeightedChatModel(
                Map.of("a", (ChatModel) a, "b", (ChatModel) b), Map.of("b", 1)))
                .isInstanceOf(IllegalArgumentException.class); // 缺 a 权重
    }

    @Test
    void routesViewReflectsWeights() {
        StubModel a = new StubModel("a");
        StubModel b = new StubModel("b");
        WeightedChatModel router = new WeightedChatModel(
                Map.of("a", (ChatModel) a, "b", (ChatModel) b),
                Map.of("a", 7, "b", 3));
        assertThat(router.routes()).containsEntry("a", 7).containsEntry("b", 3);
    }
}
