package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import io.github.chyuan_cuihongyuan.buzhou.resilience.routing.WeightedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 339 / impl-362：路由装配回归——双 bean+weights → @Primary 路由器 /
 * 未配零变化 / 单项零变化 / 缺名启动红。
 */
class WeightedRoutingAssemblyTest {

    static final class StubA implements ChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("a"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    static final class StubB implements ChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("b"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouResilienceAutoConfiguration.class))
            .withBean("cheapModel", StubA.class)
            .withBean("strongModel", StubB.class);

    @Test
    void twoWeightsAssemblePrimaryRouter() {
        runner.withPropertyValues(
                "buzhou.routing.weights.cheapModel=7",
                "buzhou.routing.weights.strongModel=3")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(WeightedChatModel.class);
                    WeightedChatModel router = context.getBean(WeightedChatModel.class);
                    assertThat(router.routes())
                            .containsEntry("cheapModel", 7)
                            .containsEntry("strongModel", 3);
                    // @Primary——按类型注入 ChatModel 得到路由器
                    ChatModel primary = context.getBean(ChatModel.class);
                    assertThat(primary).isSameAs(router);
                });
    }

    @Test
    void unconfiguredOrSingleWeightZeroChange() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(WeightedChatModel.class);
        });
        runner.withPropertyValues("buzhou.routing.weights.cheapModel=7")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(WeightedChatModel.class); // 单项零变化
                });
    }

    @Test
    void ghostBeanNameFailsFast() {
        runner.withPropertyValues(
                "buzhou.routing.weights.cheapModel=7",
                "buzhou.routing.weights.ghostModel=3")
                .run(context -> assertThat(context).hasFailed());
    }
}
