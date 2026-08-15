package io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit;

import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 模型限流 Advisor（spec「背压 · 维度③ 模型 RPM+TPM 双桶」）。
 *
 * <p>挂在 ChatClient advisor 链 {@code ToolCallingAdvisor.DEFAULT_ORDER + 650}——
 * 外于 {@code ResilienceAdvisor}(+700)、内于 {@code HookAdvisor}(+600)。
 * <b>限流裁决在最外层</b>（先于重试/超时包裹）：每次逻辑模型调用只过一次桶，
 * 重试不会绕过限流（{@code ResilienceAdvisor} 内部直接调模型终端、不重放外层 advisor，
 * 故重试不重复扣 RPM——语义正确：RPM = 逻辑请求数，非物理调用数）。
 *
 * <p>自限流拒绝抛 {@link ModelRateLimitExceededException}，在 {@code callChain.nextCall} 之前
 * 抛出——{@code ResilienceAdvisor} 不可见此异常（不进入重试分类），直接到 {@code HookAdvisor}
 * 的 {@code onModelError} 切面（用户可兜底 Replace/Block）。
 *
 * <p>TPM 记账：调用后读 {@code chatResponse.getMetadata().getUsage()}（{@code ObservabilityAdvisor} 先例），
 * 流式在流末尾聚合 usage（{@code accumulateStreamChunk} 先例）。provider 不返回 usage 时记 0 + 留痕。
 */
public class RateLimitAdvisor implements BaseAdvisor {

    private static final System.Logger LOGGER = System.getLogger(RateLimitAdvisor.class.getName());

    /** advisor 链 order 偏移：hook=+600 / rate-limit=+650 / resilience=+700。 */
    static final int CHAIN_ORDER_OFFSET = 650;

    private final ModelRateLimiter limiter;
    private final String modelName;
    /** 运维面（impl-44）：null 安全。 */
    private final ResilienceStats stats;
    /** 会话事件通道（impl-59：限流器进程级共享后，事件走当次调用会话）。 */
    private final java.util.function.Consumer<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> emitter;

    public RateLimitAdvisor(ModelRateLimiter limiter, String modelName) {
        this(limiter, modelName, null);
    }

    public RateLimitAdvisor(ModelRateLimiter limiter, String modelName, ResilienceStats stats) {
        this(limiter, modelName, stats, null);
    }

    public RateLimitAdvisor(ModelRateLimiter limiter, String modelName, ResilienceStats stats,
                            java.util.function.Consumer<io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent> emitter) {
        this.limiter = limiter;
        this.modelName = modelName == null ? "unknown" : modelName;
        this.stats = stats;
        this.emitter = emitter;
    }

    /** 限流预检 + 拒绝计数/日志（call/stream 共用；拒绝异常原样上抛）。 */
    private void acquireWithStats(String model) {
        try {
            limiter.acquireOrThrow(model, emitter);
        } catch (ModelRateLimitExceededException e) {
            if (stats != null) {
                stats.recordRateLimitRejection();
            }
            io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics()
                    .counter("buzhou.resilience.rate-limit-rejected", "model", boundedModel());
            LOGGER.log(System.Logger.Level.INFO,
                    "模型限流拒绝：model=" + modelName + ", dimension=" + e.dimension());
            throw e;
        }
    }

    /** 模型名进指标 tag 前截断（tag 值有界纪律，防无界 tag 基数）。 */
    private String boundedModel() {
        return io.github.chyuan_cuihongyuan.buzhou.resilience.MetricTags.bound(modelName);
    }

    @Override
    public String getName() {
        return "BuzhouRateLimitAdvisor";
    }

    @Override
    public int getOrder() {
        // 外于 ResilienceAdvisor(+700)：限流裁决先于重试/超时包裹。
        // 自限流拒绝在 nextCall 之前抛出 → ResilienceAdvisor 不可见 → 不进入重试分类。
        return ToolCallingAdvisor.DEFAULT_ORDER + CHAIN_ORDER_OFFSET;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain callChain) {
        // 调用前：RPM 预检+扣减 + TPM 预检
        acquireWithStats(modelName);
        // 调用模型（经 ResilienceAdvisor → 模型终端）
        ChatClientResponse response = callChain.nextCall(request);
        // 调用后：TPM 按 usage 记账
        recordUsage(response);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamChain) {
        // 调用前：RPM 预检+扣减 + TPM 预检
        acquireWithStats(modelName);
        // 流式：末尾聚合 usage 记账（accumulateStreamChunk 先例）
        AtomicReference<Usage> lastUsage = new AtomicReference<>();
        return streamChain.nextStream(request)
                .doOnEach(signal -> {
                    ChatClientResponse resp = signal.get();
                    if (resp != null && resp.chatResponse() != null) {
                        Usage u = extractUsage(resp.chatResponse());
                        if (u != null && (u.getPromptTokens() != null || u.getCompletionTokens() != null)) {
                            lastUsage.set(u);
                        }
                    }
                })
                .doOnComplete(() -> {
                    Usage usage = lastUsage.get();
                    if (usage != null) {
                        limiter.recordUsage(modelName, totalTokens(usage), emitter);
                    } else {
                        limiter.recordUsage(modelName, 0L, emitter);
                    }
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

    private void recordUsage(ChatClientResponse response) {
        if (response == null || response.chatResponse() == null) {
            limiter.recordUsage(modelName, 0L, emitter);
            return;
        }
        Usage usage = extractUsage(response.chatResponse());
        limiter.recordUsage(modelName, usage != null ? totalTokens(usage) : 0L, emitter);
    }

    private static Usage extractUsage(ChatResponse chatResponse) {
        if (chatResponse.getMetadata() == null) {
            return null;
        }
        return chatResponse.getMetadata().getUsage();
    }

    private static Long totalTokens(Usage usage) {
        Integer prompt = usage.getPromptTokens();
        Integer completion = usage.getCompletionTokens();
        long total = 0L;
        if (prompt != null) {
            total += prompt;
        }
        if (completion != null) {
            total += completion;
        }
        return total;
    }
}
