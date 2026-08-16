package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 精确响应缓存 advisor（spec 53 / T203–T205）：同请求（model + 注入后 messages + options
 * 采样）二次调用命中缓存短路——零模型开销、不进熔断窗、无 MODEL_CALL span（诚实语义：
 * 没调模型就没模型调用痕迹；命中可观测走 hit 计数）。
 *
 * <p>order = ToolCallingAdvisor.DEFAULT_ORDER + 450：memory(+400) 之后、observability(+500) /
 * resilience(+700) 之前。写入边界（T204）：只缓存终态响应（无 toolCalls 且内容非空）；
 * 流式（T205）聚合完整后写、取消/错误不写半截（LiteLLM 流式组装语义）。
 */
public class ResponseCacheAdvisor implements BaseAdvisor {

    private final ResponseCacheStore store;
    private final String modelName;

    public ResponseCacheAdvisor(ResponseCacheStore store, String modelName) {
        this.store = store;
        this.modelName = modelName;
    }

    @Override
    public String getName() {
        return "BuzhouResponseCacheAdvisor";
    }

    @Override
    public int getOrder() {
        return ToolCallingAdvisor.DEFAULT_ORDER + 450;
    }

    public ResponseCacheStore store() {
        return store;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain callChain) {
        String key = ResponseCacheKeys.keyOf(modelName, request.prompt());
        var cached = store.get(key);
        if (cached.isPresent()) {
            // 新建包装：不与历史命中方共享可变引用
            return new ChatClientResponse(cached.get(), request.context());
        }
        ChatClientResponse response = callChain.nextCall(request);
        cacheIfTerminal(key, response.chatResponse());
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamChain) {
        String key = ResponseCacheKeys.keyOf(modelName, request.prompt());
        var cached = store.get(key);
        if (cached.isPresent()) {
            ChatResponse cachedResponse = cached.get();
            return Flux.just(new ChatClientResponse(cachedResponse, request.context()));
        }
        // 聚合完整响应（内容 + usage + finishReason）后写缓存；取消/错误不写（doOnComplete 内组装）
        StringBuilder textAccumulator = new StringBuilder();
        AtomicReference<Usage> usage = new AtomicReference<>();
        AtomicReference<String> finishReason = new AtomicReference<>();
        return streamChain.nextStream(request)
                .doOnNext(response -> {
                    ChatResponse chat = response.chatResponse();
                    if (chat == null) {
                        return;
                    }
                    if (chat.getMetadata() != null && chat.getMetadata().getUsage() != null) {
                        usage.set(chat.getMetadata().getUsage());
                    }
                    Generation result = chat.getResult();
                    if (result == null) {
                        return;
                    }
                    if (result.getMetadata() != null && result.getMetadata().getFinishReason() != null) {
                        finishReason.set(result.getMetadata().getFinishReason());
                    }
                    AssistantMessage output = result.getOutput();
                    if (output != null && output.getText() != null) {
                        textAccumulator.append(output.getText());
                    }
                })
                .doOnComplete(() -> {
                    AssistantMessage assembled = new AssistantMessage(textAccumulator.toString());
                    ChatGenerationMetadata generationMetadata = finishReason.get() == null
                            ? ChatGenerationMetadata.NULL
                            : ChatGenerationMetadata.builder().finishReason(finishReason.get()).build();
                    org.springframework.ai.chat.metadata.ChatResponseMetadata metadata =
                            org.springframework.ai.chat.metadata.ChatResponseMetadata.builder()
                                    .usage(usage.get() == null
                                            ? new org.springframework.ai.chat.metadata.DefaultUsage(0, 0)
                                            : usage.get())
                                    .build();
                    ChatResponse assembledResponse = new ChatResponse(
                            java.util.List.of(new Generation(assembled, generationMetadata)), metadata);
                    cacheIfTerminal(key, assembledResponse);
                });
    }

    /** T204 写入边界：终态（无 toolCalls 且内容非空）才写。 */
    private void cacheIfTerminal(String key, ChatResponse response) {
        if (response == null || !isTerminal(response)) {
            return;
        }
        store.put(key, response);
    }

    /** T204 写入边界（公开钉住语义）：终态（无 toolCalls 且内容非空）才可缓存。 */
    public static boolean isTerminal(ChatResponse response) {
        Generation result = response.getResult();
        if (result == null) {
            return false;
        }
        AssistantMessage output = result.getOutput();
        return output != null
                && output.getText() != null
                && !output.getText().isBlank()
                && !output.hasToolCalls();
    }

    @Override
    public org.springframework.ai.chat.client.ChatClientRequest before(
            org.springframework.ai.chat.client.ChatClientRequest request,
            org.springframework.ai.chat.client.advisor.api.AdvisorChain chain) {
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response,
            org.springframework.ai.chat.client.advisor.api.AdvisorChain chain) {
        return response;
    }
}
