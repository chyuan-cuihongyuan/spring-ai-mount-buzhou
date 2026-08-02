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
        ChatClientResponse response = callChain.nextCall(ctx.request());
        ctx.markResponded(response);
        chain.afterModel(ctx);
        return ctx.response();
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

    private class DefaultModelCallContext implements ModelCallContext {
        private ChatClientRequest request;
        private ChatClientResponse response;

        DefaultModelCallContext(ChatClientRequest request) {
            this.request = request;
        }

        void markResponded(ChatClientResponse response) {
            this.response = response;
        }

        @Override
        public String sessionId() {
            return env.sessionId();
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
        public void replaceRequest(ChatClientRequest newRequest) {
            this.request = newRequest;
        }

        @Override
        public void replaceResponse(ChatClientResponse newResponse) {
            this.response = newResponse;
        }
    }
}
