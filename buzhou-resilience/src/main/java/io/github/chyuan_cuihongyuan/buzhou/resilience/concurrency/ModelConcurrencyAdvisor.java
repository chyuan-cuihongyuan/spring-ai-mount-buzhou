package io.github.chyuan_cuihongyuan.buzhou.resilience.concurrency;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 * 模型并发舱 Advisor（spec 426 / T744，Resilience4j SemaphoreBulkhead
 * 借鉴——供应商并发配额分层）。
 *
 * <p>挂在 ChatClient advisor 链 {@code ToolCallingAdvisor.DEFAULT_ORDER + 660}
 * ——rate-limit(+650) 内、{@code ResilienceAdvisor}(+700) 外：<b>并发许可在
 * 重试包裹之外获取一次、重试期间持续持有</b>（在飞 = 逻辑调用占用，重试
 * 不重复扣也不提前还——语义正确）。拒绝在 {@code nextCall} 之前抛出 →
 * ResilienceAdvisor 不可见（不进入重试分类）→ 直接到 HookAdvisor 的
 * {@code onModelError} 切面（用户可兜底 Replace/Block——RateLimitAdvisor
 * 同语义）。流式 {@code doFinally} 释放——许可持续到流终结（含 CANCEL）。
 */
public final class ModelConcurrencyAdvisor implements BaseAdvisor {

    /** advisor 链 order 偏移：rate-limit=+650 / model-concurrency=+660 / resilience=+700。 */
    static final int CHAIN_ORDER_OFFSET = 660;

    private final ModelConcurrencyLimiter limiter;
    private final String modelName;

    public ModelConcurrencyAdvisor(ModelConcurrencyLimiter limiter, String modelName) {
        this.limiter = limiter;
        this.modelName = modelName == null ? "unknown" : modelName;
    }

    @Override
    public String getName() {
        return "BuzhouModelConcurrencyAdvisor";
    }

    @Override
    public int getOrder() {
        // rate-limit(+650) 内、resilience(+700) 外：许可在重试外获取、持有跨重试。
        return ToolCallingAdvisor.DEFAULT_ORDER + CHAIN_ORDER_OFFSET;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain callChain) {
        limiter.acquireOrThrow(modelName);
        try {
            return callChain.nextCall(request);
        } finally {
            limiter.release(modelName);
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamChain) {
        limiter.acquireOrThrow(modelName);
        // 许可持续到流终结（onComplete/onError/CANCEL 均 doFinally 收口）
        return streamChain.nextStream(request)
                .doFinally(signal -> limiter.release(modelName));
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }
}
