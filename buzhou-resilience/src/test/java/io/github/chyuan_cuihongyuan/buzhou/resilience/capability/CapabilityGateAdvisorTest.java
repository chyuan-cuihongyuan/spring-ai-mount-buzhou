package io.github.chyuan_cuihongyuan.buzhou.resilience.capability;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.MimeType;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 502 / T755–T756：能力门——未注册模型零门透传；注册无 vision 拦
 * media 请求（ARGS_VALIDATION_FAILED + yml 键指引）；注册无 tools 拦带
 * 工具请求；能力齐备照常；yml 装配缺席。
 */
class CapabilityGateAdvisorTest {

    private static final class StubChain implements CallAdvisorChain {
        final List<ChatClientRequest> seen = new CopyOnWriteArrayList<>();

        @Override
        public ChatClientResponse nextCall(ChatClientRequest request) {
            seen.add(request);
            return new ChatClientResponse(new ChatResponse(
                    List.of(new Generation(new AssistantMessage("ok")))), request.context());
        }

        @Override
        public List<org.springframework.ai.chat.client.advisor.api.CallAdvisor> getCallAdvisors() {
            return List.of();
        }

        @Override
        public CallAdvisorChain copy(org.springframework.ai.chat.client.advisor.api.CallAdvisor advisor) {
            throw new UnsupportedOperationException();
        }
    }

    /** media 用户消息（公开 UserMessage 构造不带 media——子类覆写 getMedia() 同型）。 */
    private static final class MediaUserMessage extends UserMessage {
        private final List<Media> media;

        MediaUserMessage(String text, List<Media> media) {
            super(text);
            this.media = media;
        }

        @Override
        public List<Media> getMedia() {
            return media;
        }
    }

    private static ChatClientRequest request(boolean withMedia, boolean withTools) {
        UserMessage user = withMedia
                ? new MediaUserMessage("看这张图", List.of(new Media(
                        MimeType.valueOf("image/png"), URI.create("file:///tmp/a.png"))))
                : new UserMessage("纯文本问题");
        ChatOptions options = withTools
                ? ToolCallingChatOptions.builder().toolCallbacks(List.of(new ToolCallback() {
                    @Override
                    public ToolDefinition getToolDefinition() {
                        return ToolDefinition.builder().name("query")
                                .description("查询").inputSchema("{\"type\":\"object\"}").build();
                    }

                    @Override
                    public String call(String toolInput) {
                        return "{}";
                    }
                })).build()
                : ChatOptions.builder().build();
        return new ChatClientRequest(new Prompt(List.of(user), options), Map.of());
    }

    @Test
    void unregisteredModelPassesThroughWithZeroGate() {
        CapabilityGateAdvisor advisor = new CapabilityGateAdvisor(new ModelCapabilityRegistry(
                Map.of("other-model", new ModelCapabilities(false, false, -1))), "primary");
        StubChain chain = new StubChain();
        advisor.adviseCall(request(true, true), chain);
        assertThat(chain.seen).hasSize(1); // 未注册 = 零门透传
    }

    @Test
    void visionRequestBlockedWhenCapabilityNotDeclared() {
        CapabilityGateAdvisor advisor = new CapabilityGateAdvisor(new ModelCapabilityRegistry(
                Map.of("primary", new ModelCapabilities(false, true, 128_000))), "primary");
        StubChain chain = new StubChain();
        assertThatThrownBy(() -> advisor.adviseCall(request(true, false), chain))
                .isInstanceOf(BuzhouException.class)
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.ARGS_VALIDATION_FAILED))
                .hasMessageContaining("vision")
                .hasMessageContaining("model-capabilities");
        assertThat(chain.seen).isEmpty(); // 事前拦——链未触
        // 纯文本请求照常
        advisor.adviseCall(request(false, false), chain);
        assertThat(chain.seen).hasSize(1);
    }

    @Test
    void toolRequestBlockedWhenCapabilityNotDeclared() {
        CapabilityGateAdvisor advisor = new CapabilityGateAdvisor(new ModelCapabilityRegistry(
                Map.of("primary", new ModelCapabilities(true, false, -1))), "primary");
        StubChain chain = new StubChain();
        assertThatThrownBy(() -> advisor.adviseCall(request(false, true), chain))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("tools");
        assertThat(chain.seen).isEmpty();
    }

    @Test
    void capableModelPassesAllRequests() {
        CapabilityGateAdvisor advisor = new CapabilityGateAdvisor(new ModelCapabilityRegistry(
                Map.of("primary", new ModelCapabilities(true, true, 200_000))), "primary");
        StubChain chain = new StubChain();
        advisor.adviseCall(request(true, true), chain);
        assertThat(chain.seen).hasSize(1);
    }

    @Test
    void ymlAssemblyOnlyWhenCapabilitiesDeclared() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.resilience.config
                                .BuzhouResilienceAutoConfiguration.class))
                .withPropertyValues(
                        "buzhou.model-name=primary",
                        "buzhou.resilience.model-capabilities.primary.vision=false",
                        "buzhou.resilience.model-capabilities.primary.tools=true",
                        "buzhou.resilience.model-capabilities.primary.context-window=128000")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("buzhouModelCapabilityRegistry");
                    assertThat(context).hasBean("capabilityGateRuntimeConfig");
                });
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                        io.github.chyuan_cuihongyuan.buzhou.resilience.config
                                .BuzhouResilienceAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("buzhouModelCapabilityRegistry");
                    assertThat(context).doesNotHaveBean("capabilityGateRuntimeConfig");
                });
    }
}
