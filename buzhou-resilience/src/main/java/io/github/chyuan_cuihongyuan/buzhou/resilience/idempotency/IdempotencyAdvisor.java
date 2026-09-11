package io.github.chyuan_cuihongyuan.buzhou.resilience.idempotency;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheAdvisor;
import io.github.chyuan_cuihongyuan.buzhou.resilience.cache.ResponseCacheStore;
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
 * 请求幂等键 advisor（spec 501 / T753，Stripe Idempotency-Key 借鉴）：调用方
 * 经 advisor 参数 {@link #IDEMPOTENCY_KEY_PARAM} 供给幂等键——同键重入重放
 * 首次终态响应（客户端超时重试不二次真调二次计费）；键缺席=透传零行为。
 *
 * <p>order = ToolCallingAdvisor.DEFAULT_ORDER + 440：response-cache(+450) 外
 * ——同键重入连内容缓存读都跳过（「这是同一次调用」的最强声明）。存储复用
 * {@link ResponseCacheStore}（LRU+TTL+计数自带——同族语义不为第二用途造第二
 * 存储），键加 {@code idem:} 内部前缀防与内容缓存混用；写边界复用
 * {@link ResponseCacheAdvisor#isTerminal}（非终态不写半截）。
 *
 * <p>诚实边界：单实例存储（跨实例共享属已否决 Redis 缓存族）；重放不校验
 * payload 与键首见一致（宿主保证同键同请求语义——Stripe 同注记）。
 */
public class IdempotencyAdvisor implements BaseAdvisor {

    /** advisor 参数名（调用方 {@code advisors(a -> a.param(..., "sess:turn:42"))} 供给）。 */
    public static final String IDEMPOTENCY_KEY_PARAM = "buzhou.idempotency-key";

    /** advisor 链 order 偏移：idempotency=+440 / response-cache=+450。 */
    static final int CHAIN_ORDER_OFFSET = 440;

    static final String KEY_PREFIX = "idem:";

    private final ResponseCacheStore store;

    public IdempotencyAdvisor(ResponseCacheStore store) {
        this.store = store;
    }

    @Override
    public String getName() {
        return "BuzhouIdempotencyAdvisor";
    }

    @Override
    public int getOrder() {
        return ToolCallingAdvisor.DEFAULT_ORDER + CHAIN_ORDER_OFFSET;
    }

    /** BaseAdvisor 抽象槽：请求/响应原样（改写逻辑全部在本 advisor 的 advise* 内）。 */
    @Override
    public ChatClientRequest before(ChatClientRequest request,
            org.springframework.ai.chat.client.advisor.api.AdvisorChain chain) {
        return request;
    }

    /** BaseAdvisor 抽象槽：响应原样（同上）。 */
    @Override
    public ChatClientResponse after(ChatClientResponse response,
            org.springframework.ai.chat.client.advisor.api.AdvisorChain chain) {
        return response;
    }

    ResponseCacheStore store() {
        return store;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain callChain) {
        String key = keyOf(request);
        if (key == null) {
            return callChain.nextCall(request);
        }
        var cached = store.get(key);
        if (cached.isPresent()) {
            BuzhouMetricsHolder.metrics().counter("buzhou.resilience.idempotency.replayed");
            // 新建包装：不与历史命中方共享可变引用（53 命中同法）
            return new ChatClientResponse(cached.get(), request.context());
        }
        ChatClientResponse response = callChain.nextCall(request);
        if (ResponseCacheAdvisor.isTerminal(response.chatResponse())) {
            store.put(key, response.chatResponse());
            BuzhouMetricsHolder.metrics().counter("buzhou.resilience.idempotency.stored");
        }
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamChain) {
        String key = keyOf(request);
        if (key == null) {
            return streamChain.nextStream(request);
        }
        var cached = store.get(key);
        if (cached.isPresent()) {
            BuzhouMetricsHolder.metrics().counter("buzhou.resilience.idempotency.replayed");
            return Flux.just(new ChatClientResponse(cached.get(), request.context()));
        }
        // 聚合完整响应（内容 + usage + finishReason）后写；取消/错误不写半截（53 同法）
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
                    String finish = finishReason.get();
                    ChatGenerationMetadata generationMetadata = finish == null
                            ? ChatGenerationMetadata.NULL
                            : ChatGenerationMetadata.builder().finishReason(finish).build();
                    org.springframework.ai.chat.metadata.ChatResponseMetadata metadata =
                            org.springframework.ai.chat.metadata.ChatResponseMetadata.builder()
                                    .usage(usage.get() == null
                                            ? new org.springframework.ai.chat.metadata.DefaultUsage(0, 0)
                                            : usage.get())
                                    .build();
                    ChatResponse assembled = new ChatResponse(
                            java.util.List.of(new Generation(new AssistantMessage(
                                    textAccumulator.toString()), generationMetadata)), metadata);
                    if (ResponseCacheAdvisor.isTerminal(assembled)) {
                        store.put(key, assembled);
                        BuzhouMetricsHolder.metrics().counter("buzhou.resilience.idempotency.stored");
                    }
                });
    }

    /** 键提取：参数缺席/空白 = null（透传）；否则 idem: 前缀命名空间。 */
    private static String keyOf(ChatClientRequest request) {
        Object value = request.context().get(IDEMPOTENCY_KEY_PARAM);
        if (value == null) {
            return null;
        }
        String key = String.valueOf(value);
        return key.isBlank() ? null : KEY_PREFIX + key;
    }
}
