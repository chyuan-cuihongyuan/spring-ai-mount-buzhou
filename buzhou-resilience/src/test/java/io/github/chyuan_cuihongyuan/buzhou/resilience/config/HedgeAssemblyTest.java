package io.github.chyuan_cuihongyuan.buzhou.resilience.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 301 / impl-324：对冲装配回归——默认关零 bean / yml 绑定 + @Primary 升位 +
 * 慢主快冲端到端先回先得 / 未命中 fail-fast / 属性组非法值拒绝。
 */
class HedgeAssemblyTest {

    /** 慢主模型延迟（> delay 200ms，保证对冲触发窗口稳定敞开）。 */
    private static final long SLOW_PRIMARY_MILLIS = 900L;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouResilienceAutoConfiguration.class));

    /** 可控延迟的固定回复模型（对冲竞速断言用）。 */
    static final class DelayedChatModel implements ChatModel {
        private final String text;
        private final long delayMillis;

        DelayedChatModel(String text, long delayMillis) {
            this.text = text;
            this.delayMillis = delayMillis;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("测试模型被中断", e);
                }
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }
    }

    @Test
    void hedgeDisabledByDefaultRegistersNothing() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean("buzhouHedgedChatModel");
            ResilienceProperties props = context.getBean(ResilienceProperties.class);
            assertThat(props.hedge().effectiveEnabled()).isFalse();
            assertThat(props.hedge().delay()).isEqualTo(Duration.ofMillis(200));
        });
    }

    @Test
    void hedgeYmlBindsAndPrimaryInjectionWinsHedged() {
        runner.withBean("mainModel", ScriptedChatModel.class, ScriptedChatModel::new)
                .withBean("backupModel", ScriptedChatModel.class, ScriptedChatModel::new)
                .withPropertyValues(
                        "buzhou.resilience.hedge.enabled=true",
                        "buzhou.resilience.hedge.primary-model=mainModel",
                        "buzhou.resilience.hedge.model=backupModel",
                        "buzhou.resilience.hedge.delay=350ms")
                .run(context -> {
                    ResilienceProperties props = context.getBean(ResilienceProperties.class);
                    assertThat(props.hedge().effectiveEnabled()).isTrue();
                    assertThat(props.hedge().delay()).isEqualTo(Duration.ofMillis(350));
                    // 按类型注入位（@Primary）升为对冲装饰器
                    assertThat(context.getBean(ChatModel.class))
                            .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.HedgedChatModel.class);
                    // 按名注入不受影响
                    assertThat(context.getBean("mainModel", ChatModel.class))
                            .isInstanceOf(ScriptedChatModel.class);
                });
    }

    @Test
    void hedgeWinsRaceWhenPrimarySlow() {
        runner.withBean("mainModel", DelayedChatModel.class, () -> new DelayedChatModel("main:slow", SLOW_PRIMARY_MILLIS))
                .withBean("backupModel", DelayedChatModel.class, () -> new DelayedChatModel("backup:fast", 0))
                .withPropertyValues(
                        "buzhou.resilience.hedge.enabled=true",
                        "buzhou.resilience.hedge.primary-model=mainModel",
                        "buzhou.resilience.hedge.model=backupModel",
                        "buzhou.resilience.hedge.delay=200ms")
                .run(context -> {
                    ChatModel hedged = context.getBean(ChatModel.class);
                    ChatResponse response = hedged.call(new Prompt("ping"));
                    assertThat(response.getResult().getOutput().getText())
                            .isEqualTo("backup:fast");
                });
    }

    @Test
    void missingHedgeBeanNameFailsFast() {
        runner.withBean("mainModel", ScriptedChatModel.class, ScriptedChatModel::new)
                .withPropertyValues(
                        "buzhou.resilience.hedge.enabled=true",
                        "buzhou.resilience.hedge.primary-model=mainModel",
                        "buzhou.resilience.hedge.model=ghostModel")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BuzhouConfigurationException.class)
                            .hasMessageContaining("ghostModel");
                });
    }

    @Test
    void hedgeGroupRejectsSelfHedgeAndBadDelay() {
        assertThatThrownBy(() -> new ResilienceProperties.Hedge(true, "a", "a", null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("不得与主模型同名");
        assertThatThrownBy(() -> new ResilienceProperties.Hedge(true, "a", null, null))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("hedge.model");
        assertThatThrownBy(() -> new ResilienceProperties.Hedge(true, "a", "b", Duration.ZERO))
                .isInstanceOf(BuzhouConfigurationException.class)
                .hasMessageContaining("hedge.delay");
    }
}
