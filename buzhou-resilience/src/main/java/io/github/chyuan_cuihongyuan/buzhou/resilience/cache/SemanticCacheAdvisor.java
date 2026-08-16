package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 语义缓存 advisor（spec 55 §B / T241 / effort#15，LiteLLM semantic caching 同思想）：
 * 同义问法（embedding cosine ≥ 阈值）命中已缓存终态答案——零模型调用。order =
 * ToolCallingAdvisor.DEFAULT_ORDER + 460：<b>精确缓存（+450）之后</b>——精确键（零成本）
 * 先短路，语义查（嵌入成本）只在精确 miss 后发生。
 *
 * <p>分桶 = modelName + options 采样（{@link ResponseCacheKeys#optionsSample} 同口径）：
 * 跨模型/参数变体不进入相似度比较。写入边界复用 {@link ResponseCacheAdvisor#isTerminal}
 * （带 toolCalls 不缓存——spec 53 §B 工具副作用安全沿用）。
 *
 * <p><b>嵌入故障旁路降级</b>：嵌入调用异常 → 语义层旁路（WARN + bypass 计数），主调用
 * 不受阻断——嵌入故障不该弄坏主路径（与限流 fail-fast 不同：这里降级只损失一次可能命中，
 * 不损害正确性）。命中重放不进熔断窗/无 MODEL_CALL span（与精确缓存同诚实语义）。
 */
public class SemanticCacheAdvisor implements BaseAdvisor {

    private final SemanticCacheStore store;
    private final EmbeddingModel embeddingModel;
    private final String modelName;
    private final AtomicLong bypasses = new AtomicLong();

    public SemanticCacheAdvisor(SemanticCacheStore store, EmbeddingModel embeddingModel, String modelName) {
        this.store = store;
        this.embeddingModel = embeddingModel;
        this.modelName = modelName;
    }

    @Override
    public String getName() {
        return "BuzhouSemanticCacheAdvisor";
    }

    @Override
    public int getOrder() {
        return ToolCallingAdvisor.DEFAULT_ORDER + 460;
    }

    public SemanticCacheStore store() {
        return store;
    }

    /** 嵌入旁路计数（观测面：嵌入故障不应静默）。 */
    public long bypassCount() {
        return bypasses.get();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain callChain) {
        String bucket = bucketOf(request.prompt());
        var cached = lookup(bucket, request.prompt());
        if (cached.isPresent()) {
            return new ChatClientResponse(cached.get(), request.context()); // 新建包装不共享可变引用
        }
        ChatClientResponse response = callChain.nextCall(request);
        cacheIfTerminal(bucket, request.prompt(), response.chatResponse());
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamChain) {
        String bucket = bucketOf(request.prompt());
        var cached = lookup(bucket, request.prompt());
        if (cached.isPresent()) {
            return Flux.just(new ChatClientResponse(cached.get(), request.context()));
        }
        // 聚合完整响应后写（取消/错误不写半截——对齐精确缓存流式口径）
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
                    ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                            .usage(usage.get() == null ? new DefaultUsage(0, 0) : usage.get())
                            .build();
                    ChatResponse assembledResponse = new ChatResponse(
                            List.of(new Generation(assembled, generationMetadata)), metadata);
                    cacheIfTerminal(bucket, request.prompt(), assembledResponse);
                });
    }

    /** 语义查：嵌入查询文本 → 桶内最近邻；嵌入异常旁路降级（miss 口径 + bypass 计数）。 */
    private java.util.Optional<ChatResponse> lookup(String bucket, Prompt prompt) {
        float[] queryEmbedding;
        try {
            queryEmbedding = embeddingModel.embed(queryTextOf(prompt));
        } catch (RuntimeException e) {
            bypasses.incrementAndGet();
            System.getLogger(SemanticCacheAdvisor.class.getName()).log(
                    System.Logger.Level.WARNING,
                    "语义缓存嵌入查询失败——本调用旁路直通主路径（bypass 计数可见）：" + e.getMessage());
            return java.util.Optional.empty();
        }
        return store.findNearest(bucket, queryEmbedding);
    }

    private void cacheIfTerminal(String bucket, Prompt prompt, ChatResponse response) {
        if (response == null || !ResponseCacheAdvisor.isTerminal(response)) {
            return;
        }
        try {
            store.put(bucket, embeddingModel.embed(queryTextOf(prompt)), response);
        } catch (RuntimeException e) {
            bypasses.incrementAndGet();
            System.getLogger(SemanticCacheAdvisor.class.getName()).log(
                    System.Logger.Level.WARNING,
                    "语义缓存嵌入写入失败——本条不缓存（bypass 计数可见）：" + e.getMessage());
        }
    }

    /** 桶键 = modelName + options 采样（跨模型/参数变体隔离，不进入相似度比较）。 */
    private String bucketOf(Prompt prompt) {
        return "model=" + (modelName == null ? "" : modelName)
                + "|" + ResponseCacheKeys.optionsSample(prompt == null ? null : prompt.getOptions());
    }

    /** 嵌入文本 = messages 全文拼接（role 不入——问法语义与角色无关；memory 注入后视图）。 */
    static String queryTextOf(Prompt prompt) {
        StringBuilder sb = new StringBuilder(256);
        if (prompt != null && prompt.getInstructions() != null) {
            for (Message m : prompt.getInstructions()) {
                if (m != null && m.getText() != null) {
                    sb.append(m.getText()).append('\n');
                }
            }
        }
        return sb.toString();
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request,
            org.springframework.ai.chat.client.advisor.api.AdvisorChain chain) {
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response,
            org.springframework.ai.chat.client.advisor.api.AdvisorChain chain) {
        return response;
    }
}
