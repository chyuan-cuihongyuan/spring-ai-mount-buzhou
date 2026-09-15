package io.github.chyuan_cuihongyuan.buzhou.core.internal.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookChain;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HookAdvisor 切面分发直测（K 会话 R22 / spec 1221 / T1851——R7 逐类分支数据精定制导）：
 * beforeModel Block 短路（nextCall 不触达）、afterModel replaceResponse 回填生效、
 * onModelError 决策树三分支（Block 回填文本/Replace 结构化响应/Continue 放行 rethrow）、
 * adviseCall × adviseStream 同构对称。HookChain 子类脚本化三切面。
 */
class HookAdvisorTest {

    /** 脚本化 HookChain：三切面结果可编程 + 调用录制。 */
    static final class ScriptedChain extends HookChain {
        HookResult beforeResult = HookResult.CONTINUE;
        HookResult afterResult = HookResult.CONTINUE;
        HookResult onErrorResult = HookResult.CONTINUE;
        boolean replaceResponseInAfter;
        ChatClientResponse onModelReplacePayload;
        final List<String> calls = new ArrayList<>();
        RuntimeException nextCallError;
        ChatClientResponse nextCallResponse;

        ScriptedChain() {
            super(List.of(), Set.of());
        }

        @Override
        public HookResult beforeModel(ModelCallContext ctx) {
            calls.add("beforeModel");
            return beforeResult;
        }

        @Override
        public HookResult afterModel(ModelCallContext ctx) {
            calls.add("afterModel");
            if (replaceResponseInAfter) {
                ctx.replaceResponse(ChatClientResponse.builder()
                        .chatResponse(new ChatResponse(List.of(
                                new Generation(new AssistantMessage("被 afterModel 回填")))))
                        .build());
            }
            return afterResult;
        }

        @Override
        public HookResult onModelError(ModelCallContext ctx) {
            calls.add("onModelError");
            // 契约建模：Hook 返回 Replace 时由链内 applyReplace 回填 ctx 后 continue
            //（HookChain.run 语义），advisor 收到的是 CONTINUE —— 链从不向上转发 Replace
            if (onModelReplacePayload != null) {
                ctx.replaceResponse(onModelReplacePayload);
                return HookResult.CONTINUE;
            }
            return onErrorResult;
        }
    }

    private static ChatClientResponse response(String text) {
        return new ChatClientResponse(new ChatResponse(
                List.of(new Generation(new AssistantMessage(text)))), new HashMap<>());
    }

    private CallAdvisorChain callChain(ScriptedChain scripted) {
        return new CallAdvisorChain() {
            @Override
            public ChatClientResponse nextCall(ChatClientRequest req) {
                scripted.calls.add("nextCall");
                if (scripted.nextCallError != null) {
                    throw scripted.nextCallError;
                }
                return scripted.nextCallResponse;
            }

            @Override
            public List<org.springframework.ai.chat.client.advisor.api.CallAdvisor> getCallAdvisors() {
                return List.of();
            }

            @Override
            public CallAdvisorChain copy(org.springframework.ai.chat.client.advisor.api.CallAdvisor a) {
                return this;
            }
        };
    }

    private StreamAdvisorChain streamChain(ScriptedChain scripted, Flux<ChatClientResponse> flux) {
        return new StreamAdvisorChain() {
            @Override
            public Flux<ChatClientResponse> nextStream(ChatClientRequest req) {
                scripted.calls.add("nextStream");
                return flux;
            }

            @Override
            public List<org.springframework.ai.chat.client.advisor.api.StreamAdvisor> getStreamAdvisors() {
                return List.of();
            }

            @Override
            public StreamAdvisorChain copy(org.springframework.ai.chat.client.advisor.api.StreamAdvisor a) {
                return this;
            }
        };
    }

    @Test
    void beforeModelBlockShortCircuitsWithoutReachingNextCall() {
        ScriptedChain chain = new ScriptedChain();
        chain.beforeResult = HookResult.block("策略禁止");
        HookAdvisor advisor = new HookAdvisor(chain, env());

        ChatClientResponse out = advisor.adviseCall(request(), callChain(chain));

        assertThat(out.chatResponse().getResult().getOutput().getText()).isEqualTo("策略禁止");
        assertThat(chain.calls).containsExactly("beforeModel"); // nextCall 未触达
    }

    @Test
    void happyPathMarksRespondedAndRunsAfterModel() {
        ScriptedChain chain = new ScriptedChain();
        chain.nextCallResponse = response("正常答复");
        chain.replaceResponseInAfter = true; // afterModel 回填结构化响应
        HookAdvisor advisor = new HookAdvisor(chain, env());

        ChatClientResponse out = advisor.adviseCall(request(), callChain(chain));

        assertThat(chain.calls).containsExactly("beforeModel", "nextCall", "afterModel");
        assertThat(out.chatResponse().getResult().getOutput().getText()).isEqualTo("被 afterModel 回填");
    }

    @Test
    void nextCallExceptionWithBlockHandlerReturnsTextFallback() {
        ScriptedChain chain = new ScriptedChain();
        chain.nextCallError = new IllegalStateException("重试耗尽");
        chain.onErrorResult = HookResult.block("降级答复");
        HookAdvisor advisor = new HookAdvisor(chain, env());

        ChatClientResponse out = advisor.adviseCall(request(), callChain(chain));

        assertThat(out.chatResponse().getResult().getOutput().getText()).isEqualTo("降级答复");
        assertThat(chain.calls).contains("onModelError");
    }

    @Test
    void onModelReplaceHandlerYieldsStructuredFallbackViaContext() {
        ScriptedChain chain = new ScriptedChain();
        chain.nextCallError = new IllegalStateException("boom");
        // 契约建模：hook 返回 Replace → 链 applyReplace 回填 ctx 后 continue（不向上转发 Replace）
        ChatClientResponse structured = response("结构化兜底");
        chain.onModelReplacePayload = structured;
        chain.onErrorResult = HookResult.CONTINUE;
        HookAdvisor advisor = new HookAdvisor(chain, env());

        ChatClientResponse out = advisor.adviseCall(request(), callChain(chain));

        assertThat(out).isSameAs(structured);
    }

    @Test
    void streamOnModelReplaceHandlerYieldsStructuredFallback() {
        ScriptedChain chain = new ScriptedChain();
        chain.nextCallError = new IllegalStateException("boom");
        ChatClientResponse structured = response("流式结构化兜底");
        chain.onModelReplacePayload = structured;
        chain.onErrorResult = HookResult.CONTINUE;
        HookAdvisor advisor = new HookAdvisor(chain, env());

        List<ChatClientResponse> received = advisor
                .adviseStream(request(), streamChain(chain, Flux.error(
                        chain.nextCallError)))
                .collectList().block();

        assertThat(received).hasSize(1);
        assertThat(received.get(0)).isSameAs(structured);
    }

    @Test
    void nextCallExceptionWithUnhandledResultRethrows() {
        ScriptedChain chain = new ScriptedChain();
        chain.nextCallError = new IllegalStateException("boom");
        chain.onErrorResult = HookResult.CONTINUE; // 无处理：放行
        HookAdvisor advisor = new HookAdvisor(chain, env());

        assertThatThrownBy(() -> advisor.adviseCall(request(), callChain(chain)))
                .isSameAs(chain.nextCallError);
    }

    @Test
    void streamBeforeModelBlockShortCircuitsAsFallbackFlux() {
        ScriptedChain chain = new ScriptedChain();
        chain.beforeResult = HookResult.block("策略禁止");
        HookAdvisor advisor = new HookAdvisor(chain, env());

        List<ChatClientResponse> received = advisor
                .adviseStream(request(), streamChain(chain, Flux.just(response("不该到达"))))
                .collectList().block();

        assertThat(received).hasSize(1);
        assertThat(received.get(0).chatResponse().getResult().getOutput().getText()).isEqualTo("策略禁止");
        assertThat(chain.calls).doesNotContain("nextStream");
    }

    @Test
    void streamErrorWithBlockHandlerReturnsFallbackFlux() {
        ScriptedChain chain = new ScriptedChain();
        chain.nextCallError = null;
        chain.onErrorResult = HookResult.block("降级答复");
        HookAdvisor advisor = new HookAdvisor(chain, env());

        List<ChatClientResponse> received = advisor
                .adviseStream(request(), streamChain(chain, Flux.error(new IllegalStateException("boom"))))
                .collectList().block();

        assertThat(received).hasSize(1);
        assertThat(received.get(0).chatResponse().getResult().getOutput().getText()).isEqualTo("降级答复");
    }

    @Test
    void streamErrorWithoutHandlerPropagates() {
        ScriptedChain chain = new ScriptedChain();
        chain.onErrorResult = HookResult.CONTINUE;
        HookAdvisor advisor = new HookAdvisor(chain, env());
        IllegalStateException boom = new IllegalStateException("boom");

        assertThatThrownBy(() -> advisor
                .adviseStream(request(), streamChain(chain, Flux.error(boom)))
                .collectList().block())
                .isSameAs(boom);
    }

    private ChatClientRequest request() {
        return new ChatClientRequest(new Prompt(List.of(new AssistantMessage("hi"))), new HashMap<>());
    }

    private io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment env() {
        return new io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment(
                "sess-1", "agent", new InMemorySessionStateStore());
    }
}
