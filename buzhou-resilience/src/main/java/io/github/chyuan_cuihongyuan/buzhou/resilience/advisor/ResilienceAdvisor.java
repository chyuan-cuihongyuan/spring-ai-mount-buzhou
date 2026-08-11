package io.github.chyuan_cuihongyuan.buzhou.resilience.advisor;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.resilience.Classification;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ErrorCategory;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ModelCallTimeoutException;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ProviderErrorClassifier;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 模型韧性层的最内层包裹 Advisor（spec「执行点：ResilienceAdvisor」）。
 *
 * <p>挂在 ChatClient advisor 链最内层（紧邻 {@code HookAdvisor}、包裹裸 ChatModel 调用），
 * 对<b>单次模型调用</b>做：错误归一化分类 → 按策略重试。重试 / 耗尽事件进会话既有事件通道。
 *
 * <p>位置决策：{@link #getOrder()} 取 {@code ToolCallingAdvisor.DEFAULT_ORDER + 700}——memory(+400) /
 * observability(+500) / hook(+600) 之后，故 {@code HookAdvisor} 在外、本 advisor 在内：
 * {@code beforeModel}/{@code afterModel} 观察到的是「经韧性层解决后」的一次逻辑模型调用；重试耗尽 /
 * 命中不可重试 / 超时等终态失败时，异常按原语义向上抛回 {@code HookAdvisor}，由其触发 {@code onModelError}。
 *
 * <p>不引入 Resilience4j / Spring Retry：手写指数退避 + 抖动小回路。
 */
public class ResilienceAdvisor implements BaseAdvisor {

    /** 重试尝试事件：每次重试前发射（含类别 / 第几次 / 本次退避）。 */
    public static final String EVENT_RETRY_ATTEMPTED = "retry-attempted";
    /** 重试耗尽事件：重试后仍失败、放弃重试时发射。 */
    public static final String EVENT_RETRY_EXHAUSTED = "retry-exhausted";
    /** 错误归一化分类事件：每次分类发射，带五类标签（SRE 统一口径）。 */
    public static final String EVENT_ERROR_CLASSIFIED = "error-classified";
    /** 内容拒绝检测事件：响应元数据识别到内容过滤的静默拒绝时发射。 */
    public static final String EVENT_CONTENT_REFUSAL_DETECTED = "content-refusal-detected";
    /** 超时事件：模型调用超过 deadline 时发射（终态失败）。 */
    public static final String EVENT_TIMEOUT_FIRED = "timeout-fired";

    /**
     * advisor 链 order 偏移：memory=+400 / observability=+500 / hook=+600 / resilience=+700（最内层模型包裹，
     * 仍在模型终端 {@code Integer.MAX_VALUE} 之外）。见 docs/spec/10-resilience.md「Order 生效位」。
     */
    static final int CHAIN_ORDER_OFFSET = 700;

    private final ResilienceProperties config;
    private final ProviderErrorClassifier classifier;
    private final Consumer<SessionEvent> emitter;
    private final Set<String> retryable;
    private final ExecutorService deadlineExecutor;
    private final ModelCallInFlight inFlight;
    private final Duration deadline;

    public ResilienceAdvisor(ResilienceProperties config, ProviderErrorClassifier classifier,
                             Consumer<SessionEvent> emitter, ExecutorService deadlineExecutor,
                             ModelCallInFlight inFlight) {
        this.config = config;
        this.classifier = classifier;
        this.emitter = emitter == null ? event -> {
        } : emitter;
        this.retryable = config.retryableCategories().stream()
                .map(c -> c.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.deadlineExecutor = deadlineExecutor;
        this.inFlight = inFlight;
        this.deadline = config.deadline();
    }

    @Override
    public String getName() {
        return "BuzhouResilienceAdvisor";
    }

    @Override
    public int getOrder() {
        // 紧邻 HookAdvisor(+600) 之内，包裹裸 ChatModel 调用（每轮工具循环内的单次模型调用粒度）。
        return ToolCallingAdvisor.DEFAULT_ORDER + CHAIN_ORDER_OFFSET;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain callChain) {
        // 直接拿到链最内层的模型调用终端（Spring AI 的 ChatModelCallAdvisor，order=Integer.MAX_VALUE，
        // 其 adviseCall 直接 chatModel.call(prompt)、不回调链）。每次尝试都直接调用它——
        // 这样重试不会重新跑外层 advisor（Memory / HookAdvisor），保证 beforeModel/afterModel
        // 只观察「一次逻辑模型调用」（重试对 Hook 不可见）；也规避了 advisor 链 Deque 一次性消费、
        // 重复 nextCall 会「No CallAdvisors」的陷阱（ToolCallingAdvisor 用 chain.copy(this) 回避，
        // 但那会重放外层 advisor，不符合本层语义）。
        CallAdvisor modelTerminal = modelTerminal(callChain);
        int maxAttempts = config.maxAttempts();
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                ChatClientResponse response = callWithDeadline(modelTerminal, request, callChain);
                // 成功返回：但仍可能是内容拒绝（静默通道，不抛异常）——分类响应元数据。
                Classification content = classifier.classify(null, response);
                if (content.category() == ErrorCategory.CONTENT) {
                    emit(new SessionEvent(EVENT_CONTENT_REFUSAL_DETECTED, Map.of(), Instant.now()));
                    emit(classifiedEvent(ErrorCategory.CONTENT));
                    return response; // 内容拒绝：不重试、不拦截（治理归内容安全机制），仅可观测
                }
                return response;
            } catch (ModelCallTimeoutException timeout) {
                // 超时是终态失败：不重试、向上抛回 HookAdvisor 触发 onModelError。
                if (attempt > 1) {
                    emit(new SessionEvent(EVENT_RETRY_EXHAUSTED,
                            Map.of("category", "TIMEOUT", "attempts", attempt), Instant.now()));
                }
                throw timeout;
            } catch (RuntimeException e) {
                Classification c = classifier.classify(e, null);
                emit(classifiedEvent(c.category()));
                if (!isRetryable(c.category()) || attempt >= maxAttempts) {
                    if (attempt > 1) {
                        emit(new SessionEvent(EVENT_RETRY_EXHAUSTED,
                                Map.of("category", c.category().name(), "attempts", attempt), Instant.now()));
                    }
                    throw e;
                }
                Duration backoff = c.retryAfter() != null
                        ? clamp(c.retryAfter())          // 尊重 Retry-After，钳制到 maxBackoff
                        : computeBackoff(attempt);       // 否则指数退避 + 抖动
                emit(new SessionEvent(EVENT_RETRY_ATTEMPTED,
                        Map.of("category", c.category().name(), "attempt", attempt,
                                "backoffMs", backoff.toMillis(),
                                "retryAfter", c.retryAfter() != null),
                        Instant.now()));
                sleep(backoff);
            }
        }
    }

    /**
     * 把单次模型调用终端包进 deadline：提交到虚拟线程执行器、{@code Future.get(deadline)} 兜底，超时则
     * {@code cancel(true)} 把中断传播进在途模型调用（对齐执行脊柱 {@code HarnessToolCallingManager} 的超时手法）。
     * 该 Future 同时注册进 {@link ModelCallInFlight}，供 {@code session.cancel()} 中断。
     */
    private ChatClientResponse callWithDeadline(CallAdvisor terminal, ChatClientRequest request,
                                                CallAdvisorChain chain) {
        if (deadline == null || deadline.isZero()) {
            return terminal.adviseCall(request, chain); // 未启用 deadline：直连
        }
        Future<ChatClientResponse> future = deadlineExecutor.submit(
                () -> terminal.adviseCall(request, chain));
        inFlight.register(future);
        try {
            return future.get(deadline.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            future.cancel(true);
            emit(new SessionEvent(EVENT_TIMEOUT_FIRED,
                    Map.of("deadlineMs", deadline.toMillis()), Instant.now()));
            throw new ModelCallTimeoutException(deadline);
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(cause);
        } catch (CancellationException ce) {
            // session.cancel() 中断了在途调用：传播为中断态。
            Thread.currentThread().interrupt();
            throw new RuntimeException("model call cancelled", ce);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("model call interrupted", ie);
        } finally {
            inFlight.unregister(future);
        }
    }

    private SessionEvent classifiedEvent(ErrorCategory category) {
        return new SessionEvent(EVENT_ERROR_CLASSIFIED, Map.of("category", category.name()), Instant.now());
    }

    private Duration clamp(Duration backoff) {
        return backoff.compareTo(config.maxBackoff()) > 0 ? config.maxBackoff() : backoff;
    }

    /**
     * 取链最内层的模型调用终端（{@code ChatModelCallAdvisor}，order 最高）。
     * 由 ChatClient 装配保证始终存在；本 advisor 的 order（{@code DEFAULT_ORDER+700}）远低于
     * 终端的 {@code Integer.MAX_VALUE}，故链尾恒为终端、非本 advisor。
     */
    private CallAdvisor modelTerminal(CallAdvisorChain callChain) {
        List<CallAdvisor> advisors = callChain.getCallAdvisors();
        CallAdvisor last = advisors.get(advisors.size() - 1);
        if (last == this) {
            // 不应发生：装配异常导致本 advisor 成了链尾、无模型终端。
            throw new IllegalStateException("ResilienceAdvisor is innermost; no model-call terminal in chain");
        }
        return last;
    }

    /**
     * 可重试判定读 {@code retryable-categories} 配置（默认 {@code [RATE_LIMIT, NETWORK]}）。
     */
    private boolean isRetryable(ErrorCategory category) {
        return retryable.contains(category.name());
    }

    /**
     * 指数退避 {@code initial * multiplier^(attempt-1)}，钳制到 {@code maxBackoff}，再加 {@code ±jitter} 抖动。
     * 02 号票接入 Retry-After（限流时优先尊重、并钳制到 maxBackoff）。
     */
    private Duration computeBackoff(int attempt) {
        double grown = config.initialBackoff().toMillis() * Math.pow(config.multiplier(), attempt - 1);
        long capped = (long) Math.min(grown, config.maxBackoff().toMillis());
        double j = config.jitter();
        long jittered = j <= 0 ? capped
                : (long) (capped * (1.0 - j + 2 * j * ThreadLocalRandom.current().nextDouble()));
        return Duration.ofMillis(Math.max(1, jittered));
    }

    private void sleep(Duration backoff) {
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("resilience retry backoff interrupted", ie);
        }
    }

    private void emit(SessionEvent event) {
        emitter.accept(event);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamChain) {
        Flux<ChatClientResponse> stream = streamChain.nextStream(request);
        if (deadline != null && !deadline.isZero()) {
            // deadline 作为「首 token / 帧间空闲」超时：active 流式每帧重置计时，挂住的流在 deadline 内终止。
            stream = stream.timeout(deadline);
        }
        // M1 流式边界：不做中途重试（已发 token 不可回收）。失败即分类 + 传播，
        // 由外层 HookAdvisor 触发 onModelError（兜底或放行）。
        return stream.onErrorResume(e -> {
            if (e instanceof TimeoutException && deadline != null && !deadline.isZero()) {
                // 本层 .timeout(deadline) 触发的超时（deadline 关闭时此分支不进入，TimeoutException 交分类）。
                emit(new SessionEvent(EVENT_TIMEOUT_FIRED,
                        Map.of("deadlineMs", deadline.toMillis()), Instant.now()));
                return Flux.error(new ModelCallTimeoutException(deadline));
            }
            Throwable terminal = (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            Classification c = classifier.classify(terminal, null);
            emit(classifiedEvent(c.category()));
            return Flux.error(terminal);
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
}
