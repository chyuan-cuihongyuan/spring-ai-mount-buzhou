package io.github.chyuan_cuihongyuan.buzhou.resilience.advisor;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.resilience.Classification;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ErrorCategory;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ModelCallTimeoutException;
import io.github.chyuan_cuihongyuan.buzhou.resilience.ProviderErrorClassifier;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
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

    private static final System.Logger LOGGER = System.getLogger(ResilienceAdvisor.class.getName());

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
    /** 运维面（impl-44）：null 安全——编程式路径未传时静默。 */
    private final ResilienceStats stats;
    /** 熔断器（impl-56）：null = 未启用。 */
    private final io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitBreaker circuit;
    /** 分桶键（与限流器同口径，buzhou.model-name）。 */
    private final String modelName;
    /** 备模型降级链（impl-57）：null = 未配置。 */
    private final io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain fallback;
    /** shadow 探测（spec 49 §A / T176）：null = 未启用。 */
    private final io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowTrafficController shadow;
    /** 候选级限流闸（spec 49 §B / T177）：null = 未配置（候选调用不限流，既有行为）。 */
    private final io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit.ModelRateLimiter candidateLimiter;

    public ResilienceAdvisor(ResilienceProperties config, ProviderErrorClassifier classifier,
                             Consumer<SessionEvent> emitter, ExecutorService deadlineExecutor,
                             ModelCallInFlight inFlight) {
        this(config, classifier, emitter, deadlineExecutor, inFlight, null);
    }

    public ResilienceAdvisor(ResilienceProperties config, ProviderErrorClassifier classifier,
                             Consumer<SessionEvent> emitter, ExecutorService deadlineExecutor,
                             ModelCallInFlight inFlight, ResilienceStats stats) {
        this(config, classifier, emitter, deadlineExecutor, inFlight, stats, null, null);
    }

    public ResilienceAdvisor(ResilienceProperties config, ProviderErrorClassifier classifier,
                             Consumer<SessionEvent> emitter, ExecutorService deadlineExecutor,
                             ModelCallInFlight inFlight, ResilienceStats stats,
                             io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitBreaker circuit,
                             String modelName) {
        this(config, classifier, emitter, deadlineExecutor, inFlight, stats, circuit, modelName, null);
    }

    public ResilienceAdvisor(ResilienceProperties config, ProviderErrorClassifier classifier,
                             Consumer<SessionEvent> emitter, ExecutorService deadlineExecutor,
                             ModelCallInFlight inFlight, ResilienceStats stats,
                             io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitBreaker circuit,
                             String modelName,
                             io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain fallback) {
        this(config, classifier, emitter, deadlineExecutor, inFlight, stats, circuit, modelName,
                fallback, null);
    }

    /** spec 49 §A / T176 全参：+ shadow 探测控制器（null = 未启用）。 */
    public ResilienceAdvisor(ResilienceProperties config, ProviderErrorClassifier classifier,
                             Consumer<SessionEvent> emitter, ExecutorService deadlineExecutor,
                             ModelCallInFlight inFlight, ResilienceStats stats,
                             io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitBreaker circuit,
                             String modelName,
                             io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain fallback,
                             io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowTrafficController shadow) {
        this(config, classifier, emitter, deadlineExecutor, inFlight, stats, circuit, modelName,
                fallback, shadow, null);
    }

    /** spec 49 §A/§B 全参：+ shadow + 候选级限流闸（null = 候选不限流）。 */
    public ResilienceAdvisor(ResilienceProperties config, ProviderErrorClassifier classifier,
                             Consumer<SessionEvent> emitter, ExecutorService deadlineExecutor,
                             ModelCallInFlight inFlight, ResilienceStats stats,
                             io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitBreaker circuit,
                             String modelName,
                             io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain fallback,
                             io.github.chyuan_cuihongyuan.buzhou.resilience.shadow.ShadowTrafficController shadow,
                             io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit.ModelRateLimiter candidateLimiter) {
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
        this.stats = stats;
        this.circuit = circuit;
        this.modelName = modelName == null ? "unknown" : modelName;
        this.fallback = fallback;
        this.shadow = shadow;
        this.candidateLimiter = candidateLimiter;
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
        if (circuit == null && (fallback == null || fallback.isEmpty())) {
            return doAdviseCall(request, callChain);
        }
        // spec 48 §B / T175：金丝雀启用时首次调用按会话稳定哈希选初始目标（每会话粘住）；
        // 选中备模型 → 金丝雀路径；选中主模型 → 既有主路径（逐字节不变）。
        String initialTarget = resolveCanaryChoice(request);
        if (!modelName.equals(initialTarget)) {
            return adviseCallCanary(request, callChain, initialTarget);
        }
        if (circuit != null) {
            try {
                // 熔断前置闸（impl-56）+ 逻辑调用级三态结果记录：重试 attempt 不重复记；
                // OPEN 拒绝异常不进重试分类（跳闸后重试只会继续锤故障 provider）。
                circuit.beforeCall(modelName, emitter);
            } catch (io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitOpenException open) {
                // 熔断 OPEN 恒触发降级（impl-57）：主断路打开后请求零重试直达备模型；无备模型原样上抛。
                return fallbackOrRethrow(request, "CIRCUIT_OPEN", open);
            }
        }
        try {
            long startNs = System.nanoTime();
            ChatClientResponse response = doAdviseCall(request, callChain);
            if (circuit != null) {
                circuit.recordSuccess(modelName, emitter);
            }
            // spec 49 §A / T176：主模型成功后提交 shadow 对照（提交即返回，用户路径零增延迟；
            // 金丝雀/流式路径不探测——诚实边界见 spec）
            if (shadow != null && shadow.enabled()) {
                shadow.submit(request.prompt(), modelName,
                        (System.nanoTime() - startNs) / 1_000_000, emitter);
            }
            return response;
        } catch (ModelCallTimeoutException e) {
            if (circuit != null) {
                circuit.recordTerminal(modelName, "TIMEOUT", emitter);
            }
            return fallbackOrRethrow(request, "TIMEOUT", e);
        } catch (RuntimeException e) {
            String category = classifier.classify(e, null).category().name();
            if (circuit != null) {
                circuit.recordTerminal(modelName, category, emitter);
            }
            return fallbackOrRethrow(request, category, e);
        }
    }

    /** 会话级金丝雀首选记忆（advisor 每会话构造，随会话消亡——零泄漏面）。 */
    private String canaryChoice;
    private boolean canaryNotified;

    /** spec 48 §B / T175：解析（并粘住）本会话的初始目标；canary.selected 事件每会话一次。 */
    private String resolveCanaryChoice(ChatClientRequest request) {
        if (fallback == null || fallback.isEmpty() || !fallback.canaryEnabled()) {
            return modelName;
        }
        if (canaryChoice == null) {
            Object conversationId = request.context().get(
                    org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID);
            String sessionId = conversationId instanceof String s ? s : null;
            canaryChoice = fallback.selectInitialTarget(modelName, sessionId);
            if (!canaryNotified) {
                canaryNotified = true;
                emit(new SessionEvent(
                        io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain.EVENT_CANARY_SELECTED,
                        Map.of("model", canaryChoice, "primary", modelName), Instant.now()));
            }
        }
        return canaryChoice;
    }

    /**
     * spec 48 §B / T175：金丝雀选中备模型的初始路径——目标自身熔断闸 + 单次 deadline 调用 +
     * 终态独立记账；终态失败按链序回退剩余候选（主模型在链首位、跳过已试目标），全败上抛
     * 所选目标的原始错误（金丝雀语境下的「主因」）。
     */
    private ChatClientResponse adviseCallCanary(ChatClientRequest request, CallAdvisorChain callChain,
                                                String targetName) {
        io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel target =
                fallback.byName(targetName);
        if (target == null) {
            return doAdviseCall(request, callChain); // 配置竞态防御：目标已不在链上走主路径
        }
        if (!tryAcquireCandidateQuota(targetName)) {
            // 金丝雀目标配额尽：按链序回退（与目标失败同路，category 标配额维度）
            return degradeFromCanary(request, callChain, targetName, "RATE_LIMIT",
                    new io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit.ModelRateLimitExceededException(
                            targetName, "RPM+TPM", Duration.ZERO));
        }
        if (circuit != null) {
            try {
                circuit.beforeCall(targetName, emitter);
            } catch (io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitOpenException open) {
                return degradeFromCanary(request, callChain, targetName, "CIRCUIT_OPEN", open);
            }
        }
        try {
            ChatClientResponse response = callWithDeadline(
                    () -> new ChatClientResponse(target.model().call(request.prompt()), request.context()));
            if (circuit != null) {
                circuit.recordSuccess(targetName, emitter);
            }
            recordCandidateUsage(targetName, response);
            return response;
        } catch (RuntimeException e) {
            String category = e instanceof ModelCallTimeoutException
                    ? "TIMEOUT" : classifier.classify(e, null).category().name();
            if (circuit != null) {
                circuit.recordTerminal(targetName, category, emitter);
            }
            return degradeFromCanary(request, callChain, targetName, category, e);
        }
    }

    /**
     * spec 49 §B / T177：候选级限流闸——拒绝时计既有 rate-limit-rejected 家族 + 跳过
     * （不视作模型故障入熔断窗）；未配置限流器恒放行。
     */
    private boolean tryAcquireCandidateQuota(String candidateName) {
        if (candidateLimiter == null) {
            return true;
        }
        try {
            candidateLimiter.acquireOrThrow(candidateName, emitter);
            return true;
        } catch (io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit.ModelRateLimitExceededException e) {
            metrics().counter("buzhou.resilience.rate-limit-rejected",
                    "model", io.github.chyuan_cuihongyuan.buzhou.resilience.MetricTags.bound(candidateName));
            LOGGER.log(System.Logger.Level.WARNING,
                    "候选模型限流拒绝，跳到下一级：candidate=" + candidateName);
            return false;
        }
    }

    /** spec 49 §B / T177：候选成功后按实际 usage 记账 TPM（缺失留痕沿既有口径）。 */
    private void recordCandidateUsage(String candidateName, ChatClientResponse response) {
        if (candidateLimiter == null) {
            return;
        }
        Long tokens = null;
        if (response != null && response.chatResponse() != null
                && response.chatResponse().getMetadata() != null
                && response.chatResponse().getMetadata().getUsage() != null) {
            var usage = response.chatResponse().getMetadata().getUsage();
            if (usage.getTotalTokens() != null) {
                tokens = usage.getTotalTokens().longValue();
            } else if (usage.getPromptTokens() != null || usage.getCompletionTokens() != null) {
                tokens = (usage.getPromptTokens() == null ? 0L : usage.getPromptTokens().longValue())
                        + (usage.getCompletionTokens() == null ? 0L : usage.getCompletionTokens().longValue());
            }
        }
        candidateLimiter.recordUsage(candidateName, tokens, emitter);
    }

    /** 金丝雀目标终态失败后的链序回退：候选 = [主模型 + 备模型链] 跳过已试目标，单次尝试。 */
    private ChatClientResponse degradeFromCanary(ChatClientRequest request, CallAdvisorChain callChain,
                                                 String targetName, String category,
                                                 RuntimeException targetError) {
        record Candidate(String name, java.util.function.Supplier<ChatClientResponse> call) {
        }
        List<Candidate> candidates = new java.util.ArrayList<>();
        candidates.add(new Candidate(modelName,
                () -> modelTerminal(callChain).adviseCall(request, callChain)));
        for (io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel fb : fallback.models()) {
            if (fb.name().equals(targetName)) {
                continue;
            }
            candidates.add(new Candidate(fb.name(),
                    () -> new ChatClientResponse(fb.model().call(request.prompt()), request.context())));
        }
        for (Candidate candidate : candidates) {
            if (!tryAcquireCandidateQuota(candidate.name())) {
                continue;
            }
            if (circuit != null) {
                try {
                    circuit.beforeCall(candidate.name(), emitter);
                } catch (io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitOpenException skip) {
                    LOGGER.log(System.Logger.Level.WARNING,
                            "回退候选熔断 OPEN，跳过：candidate=" + candidate.name());
                    continue;
                }
            }
            try {
                ChatClientResponse response = callWithDeadline(candidate.call());
                if (circuit != null) {
                    circuit.recordSuccess(candidate.name(), emitter);
                }
                recordCandidateUsage(candidate.name(), response);
                if (stats != null) {
                    stats.recordFallbackSwitch();
                }
                metrics().counter("buzhou.resilience.fallback-switches",
                        "from", io.github.chyuan_cuihongyuan.buzhou.resilience.MetricTags.bound(targetName),
                        "to", io.github.chyuan_cuihongyuan.buzhou.resilience.MetricTags.bound(candidate.name()));
                emit(new SessionEvent(io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain.EVENT_SWITCHED,
                        Map.of("from", targetName, "to", candidate.name(), "category", category), Instant.now()));
                LOGGER.log(System.Logger.Level.WARNING,
                        "金丝雀目标失败回退：" + targetName + " → " + candidate.name()
                                + "（category=" + category + "）");
                return response;
            } catch (RuntimeException e) {
                String cCategory = e instanceof ModelCallTimeoutException
                        ? "TIMEOUT" : classifier.classify(e, null).category().name();
                if (circuit != null) {
                    circuit.recordTerminal(candidate.name(), cCategory, emitter);
                }
            }
        }
        if (stats != null) {
            stats.recordFallbackExhausted();
        }
        metrics().counter("buzhou.resilience.fallback-exhausted", "model", targetName);
        emit(new SessionEvent(io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain.EVENT_EXHAUSTED,
                Map.of("from", targetName, "category", category), Instant.now()));
        throw targetError;
    }

    /**
     * 备模型降级（impl-57 / spec 15）：触发条件命中后按序逐个尝试备模型——各自熔断前置闸
     * （备模型 OPEN 跳过该级）、单次带 deadline 调用、终态独立记账；任一成功即返回（外层 advisor
     * 观察到一次成功逻辑调用）；全败上抛<b>主模型原始错误</b>（根因不遮蔽）。CIRCUIT_OPEN 恒触发。
     */
    private ChatClientResponse fallbackOrRethrow(ChatClientRequest request, String category,
                                                 RuntimeException primaryError) {
        if (fallback == null || fallback.isEmpty()
                || (!"CIRCUIT_OPEN".equals(category) && !fallback.triggers(category))) {
            throw primaryError;
        }
        for (io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel fb : fallback.models()) {
            if (!tryAcquireCandidateQuota(fb.name())) {
                continue;
            }
            if (circuit != null) {
                try {
                    circuit.beforeCall(fb.name(), emitter);
                } catch (io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitOpenException skip) {
                    LOGGER.log(System.Logger.Level.WARNING, "备模型自身熔断 OPEN，跳过：fallback=" + fb.name());
                    continue;
                }
            }
            try {
                ChatClientResponse response = callWithDeadline(
                        () -> new ChatClientResponse(fb.model().call(request.prompt()), request.context()));
                if (circuit != null) {
                    circuit.recordSuccess(fb.name(), emitter);
                }
                recordCandidateUsage(fb.name(), response);
                if (stats != null) {
                    stats.recordFallbackSwitch();
                }
                metrics().counter("buzhou.resilience.fallback-switches",
                        "from", io.github.chyuan_cuihongyuan.buzhou.resilience.MetricTags.bound(modelName),
                        "to", io.github.chyuan_cuihongyuan.buzhou.resilience.MetricTags.bound(fb.name()));
                emit(new SessionEvent(io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain.EVENT_SWITCHED,
                        Map.of("from", modelName, "to", fb.name(), "category", category), Instant.now()));
                LOGGER.log(System.Logger.Level.WARNING,
                        "模型降级切换：primary=" + modelName + " → fallback=" + fb.name()
                                + "（category=" + category + "）");
                return response;
            } catch (RuntimeException e) {
                String fbCategory = e instanceof ModelCallTimeoutException
                        ? "TIMEOUT" : classifier.classify(e, null).category().name();
                if (circuit != null) {
                    circuit.recordTerminal(fb.name(), fbCategory, emitter);
                }
                LOGGER.log(System.Logger.Level.WARNING,
                        "备模型失败，尝试下一级：fallback=" + fb.name() + "，category=" + fbCategory
                                + "，error=" + e.getMessage());
            }
        }
        if (stats != null) {
            stats.recordFallbackExhausted();
        }
        metrics().counter("buzhou.resilience.fallback-exhausted", "model", modelName);
        emit(new SessionEvent(io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.FallbackChain.EVENT_EXHAUSTED,
                Map.of("from", modelName, "category", category), Instant.now()));
        LOGGER.log(System.Logger.Level.ERROR,
                "备模型链全部耗尽（category=" + category + "）：上抛主模型原始错误");
        throw primaryError;
    }

    private ChatClientResponse doAdviseCall(ChatClientRequest request, CallAdvisorChain callChain) {
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
                ChatClientResponse response = callWithDeadline(
                        () -> modelTerminal.adviseCall(request, callChain));
                // 成功返回：但仍可能是内容拒绝（静默通道，不抛异常）——分类响应元数据。
                Classification content = classifier.classify(null, response);
                if (content.category() == ErrorCategory.CONTENT) {
                    emit(new SessionEvent(EVENT_CONTENT_REFUSAL_DETECTED, Map.of(), Instant.now()));
                    emit(classifiedEvent(ErrorCategory.CONTENT));
                    if (stats != null) {
                        stats.recordContentRefusal();
                        stats.recordErrorCategory(ErrorCategory.CONTENT.name());
                    }
                    metrics().counter("buzhou.resilience.content-refusals");
                    LOGGER.log(System.Logger.Level.DEBUG,
                            "模型内容拒绝（静默通道）已检出：不重试、仅可观测");
                    return response; // 内容拒绝：不重试、不拦截（治理归内容安全机制），仅可观测
                }
                return response;
            } catch (ModelCallTimeoutException timeout) {
                // 超时是终态失败：不重试、向上抛回 HookAdvisor 触发 onModelError。
                if (attempt > 1) {
                    emit(new SessionEvent(EVENT_RETRY_EXHAUSTED,
                            Map.of("category", "TIMEOUT", "attempts", attempt), Instant.now()));
                    if (stats != null) {
                        stats.recordRetryExhausted();
                    }
                }
                if (stats != null) {
                    stats.recordModelTimeout();
                    stats.recordErrorCategory("TIMEOUT");
                }
                metrics().counter("buzhou.resilience.model-timeouts");
                LOGGER.log(System.Logger.Level.WARNING,
                        "模型调用超时（deadline=" + deadline.toMillis() + "ms，attempt=" + attempt + "）：终态失败上抛");
                throw timeout;
            } catch (RuntimeException e) {
                Classification c = classifier.classify(e, null);
                emit(classifiedEvent(c.category()));
                if (stats != null) {
                    stats.recordErrorCategory(c.category().name());
                }
                if (!isRetryable(c.category()) || attempt >= maxAttempts) {
                    if (attempt > 1) {
                        emit(new SessionEvent(EVENT_RETRY_EXHAUSTED,
                                Map.of("category", c.category().name(), "attempts", attempt), Instant.now()));
                        if (stats != null) {
                            stats.recordRetryExhausted();
                        }
                        metrics().counter("buzhou.resilience.retry-exhausted", "category", c.category().name());
                    }
                    LOGGER.log(System.Logger.Level.ERROR,
                            "模型调用重试耗尽/不可重试：category=" + c.category().name()
                                    + ", attempts=" + attempt + ", error=" + e.getMessage());
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
                if (stats != null) {
                    stats.recordRetryAttempt();
                }
                metrics().counter("buzhou.resilience.retries", "category", c.category().name());
                LOGGER.log(System.Logger.Level.WARNING,
                        "模型调用重试：category=" + c.category().name() + ", attempt=" + attempt
                                + ", backoffMs=" + backoff.toMillis()
                                + (c.retryAfter() != null ? "（尊重 Retry-After）" : ""));
                sleep(backoff);
            }
        }
    }

    /**
     * 把单次模型调用包进 deadline：提交到虚拟线程执行器、{@code Future.get(deadline)} 兜底，超时则
     * {@code cancel(true)} 把中断传播进在途模型调用（对齐执行脊柱 {@code HarnessToolCallingManager} 的超时手法）。
     * 该 Future 同时注册进 {@link ModelCallInFlight}，供 {@code session.cancel()} 中断。
     * impl-57 起泛化为 supplier——主链终端与备模型直调共用同一条 deadline/cancel 路径。
     */
    private ChatClientResponse callWithDeadline(java.util.function.Supplier<ChatClientResponse> call) {
        if (deadline == null || deadline.isZero()) {
            return call.get(); // 未启用 deadline：直连
        }
        Future<ChatClientResponse> future = deadlineExecutor.submit(call::get);
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

    private static io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetrics metrics() {
        return io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder.metrics();
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain streamChain) {
        if (circuit == null) {
            return doAdviseStream(request, streamChain);
        }
        // 熔断前置闸（impl-56）：流式入口 fail-fast——已发 token 不可回收，更不能对着故障 provider 开流。
        try {
            circuit.beforeCall(modelName, emitter);
        } catch (io.github.chyuan_cuihongyuan.buzhou.resilience.circuit.ModelCircuitOpenException e) {
            return Flux.error(e);
        }
        java.util.concurrent.atomic.AtomicBoolean recorded = new java.util.concurrent.atomic.AtomicBoolean();
        return doAdviseStream(request, streamChain)
                .doOnComplete(() -> {
                    if (recorded.compareAndSet(false, true)) {
                        circuit.recordSuccess(modelName, emitter);
                    }
                })
                .doOnError(e -> {
                    if (recorded.compareAndSet(false, true)) {
                        String category = e instanceof ModelCallTimeoutException
                                ? "TIMEOUT"
                                : classifier.classify((e instanceof RuntimeException re) ? re
                                        : new RuntimeException(e), null).category().name();
                        circuit.recordTerminal(modelName, category, emitter);
                    }
                });
    }

    private Flux<ChatClientResponse> doAdviseStream(ChatClientRequest request, StreamAdvisorChain streamChain) {
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
                if (stats != null) {
                    stats.recordModelTimeout();
                    stats.recordErrorCategory("TIMEOUT");
                }
                metrics().counter("buzhou.resilience.model-timeouts");
                return Flux.error(new ModelCallTimeoutException(deadline));
            }
            Throwable terminal = (e instanceof RuntimeException re) ? re : new RuntimeException(e);
            Classification c = classifier.classify(terminal, null);
            emit(classifiedEvent(c.category()));
            if (stats != null) {
                stats.recordErrorCategory(c.category().name());
            }
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
