package io.github.chyuan_cuihongyuan.buzhou.core.internal.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookChain;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.ModelCallContext;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import reactor.core.publisher.Flux;

import java.util.List;

public class HookAdvisor implements BaseAdvisor {

    private final HookChain chain;
    private final HookEnvironment env;

    public HookAdvisor(HookChain chain, HookEnvironment env) {
        this.chain = chain;
        this.env = env;
    }

    @Override
    public String getName() {
        return "BuzhouHookAdvisor";
    }

    @Override
    public int getOrder() {
        return ToolCallingAdvisor.DEFAULT_ORDER + 600;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain callChain) {
        DefaultModelCallContext ctx = new DefaultModelCallContext(request);
        HookResult before = chain.beforeModel(ctx);
        if (before instanceof HookResult.Block block) {
            return respondWith(block.reason());
        }
        try {
            ChatClientResponse response = callChain.nextCall(ctx.request());
            ctx.markResponded(response);
            chain.afterModel(ctx);
            return ctx.response();
        } catch (RuntimeException e) {
            // 终态失败（韧性层重试耗尽 / 命中不可重试类别 / 超时）：交 onModelError 切面决定兜底或放行。
            ctx.markFailed(e);
            ChatClientResponse fallback = resolveModelError(ctx, chain.onModelError(ctx));
            if (fallback != null) {
                return fallback; // Hook 经 Block(reason) / Replace(ChatClientResponse) 回填兜底响应、吞错
            }
            throw e; // 放行：异常按底座原语义抛出（行为与未接 onModelError Hook 一致）
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamChain) {
        DefaultModelCallContext ctx = new DefaultModelCallContext(request);
        HookResult before = chain.beforeModel(ctx);
        if (before instanceof HookResult.Block block) {
            return Flux.just(respondWith(block.reason()));
        }
        return streamChain.nextStream(ctx.request())
                .map(response -> {
                    ctx.markResponded(response);
                    chain.afterModel(ctx);
                    return ctx.response();
                })
                .onErrorResume(e -> {
                    // 终态失败（流式）：交 onModelError 切面决定兜底或放行（与 adviseCall 同构）。
                    ctx.markFailed(e);
                    ChatClientResponse fallback = resolveModelError(ctx, chain.onModelError(ctx));
                    return fallback != null ? Flux.just(fallback) : Flux.error(e);
                });
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    private ChatClientResponse respondWith(String text) {
        ChatResponse chatResponse = new ChatResponse(
                List.of(new Generation(new AssistantMessage(text))));
        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .build();
    }

    /**
     * onModelError 切面返回后的兜底决策（adviseCall / adviseStream 共用）：
     * 返回兜底响应（{@code Block(reason)} 回填文本、{@code Replace(ChatClientResponse)} 回填结构化响应），
     * 或 {@code null} 表示放行（由调用方抛回原异常 / {@code Flux.error}）——失败路径上未设置过
     * response，故 {@code ctx.response()} 仅在 Hook 显式 Replace 后非 null。
     */
    private ChatClientResponse resolveModelError(DefaultModelCallContext ctx, HookResult handled) {
        if (handled instanceof HookResult.Block block) {
            return respondWith(block.reason());
        }
        return ctx.response();
    }

    private class DefaultModelCallContext implements ModelCallContext {
        private ChatClientRequest request;
        private ChatClientResponse response;
        private Throwable error;

        DefaultModelCallContext(ChatClientRequest request) {
            this.request = request;
        }

        void markResponded(ChatClientResponse response) {
            this.response = response;
        }

        void markFailed(Throwable error) {
            this.error = error;
        }

        @Override
        public String sessionId() {
            return env.sessionId();
        }

        @Override
        public String agentName() {
            return env.agentName();
        }

        @Override
        public int turn() {
            return env.currentTurn();
        }

        @Override
        public SessionStateHandle state() {
            return env.stateHandle();
        }

        @Override
        public void emitEvent(SessionEvent event) {
            env.emit(event);
        }

        @Override
        public ChatClientRequest request() {
            return request;
        }

        @Override
        public ChatClientResponse response() {
            return response;
        }

        @Override
        public Throwable error() {
            return error;
        }

        @Override
        public void replaceRequest(ChatClientRequest newRequest) {
            this.request = newRequest;
        }

        @Override
        public void replaceResponse(ChatClientResponse newResponse) {
            this.response = newResponse;
        }
    }
}
