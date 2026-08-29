package io.github.chyuan_cuihongyuan.buzhou.resilience.fallback;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 模型对冲请求（spec 137 / T485，Google tail-tolerant RPC / gRPC hedging 借鉴）：
 * call() 主模型超 hedgeDelay 未回即<b>并发</b>发备模型，先回先得、输家取消；
 * 主模型在延迟内<b>快速失败</b>也立即转对冲（错误≠长尾，但不让用户吃确定的失败等待）；
 * 双败抛主模型异常（保既有错误语义与降级链触发口径）。stream() 诚实委派主模型。
 *
 * <p>与 spec 15 备模型降级链互补：链管「终态失败后串行换人」，本类管「长尾等待中
 * 并行押注」。装饰器零侵入——宿主把它当主模型挂进任何装配位。
 *
 * <p>诚实边界：同一 Prompt 原样发两模型（模型特定 options 兼容归宿主）；
 * 对冲成本 = 双倍调用（hedgeDelay 建议设在主模型 p95 之上）。计数：
 * buzhou.hedge.primary-won / fired / won。
 */
public final class HedgedChatModel implements ChatModel {

    private static final String COUNTER_PRIMARY_WON = "buzhou.hedge.primary-won";
    private static final String COUNTER_FIRED = "buzhou.hedge.fired";
    private static final String COUNTER_HEDGE_WON = "buzhou.hedge.won";

    private final ChatModel primary;
    private final ChatModel hedge;
    private final Duration hedgeDelay;
    private final ExecutorService executor;

    public HedgedChatModel(ChatModel primary, ChatModel hedge,
                           Duration hedgeDelay, ExecutorService executor) {
        if (primary == null || hedge == null || executor == null || hedgeDelay == null
                || hedgeDelay.isZero() || hedgeDelay.isNegative()) {
            throw new IllegalArgumentException(
                    "primary/hedge/executor 必须非空、hedgeDelay 为正");
        }
        this.primary = primary;
        this.hedge = hedge;
        this.hedgeDelay = hedgeDelay;
        this.executor = executor;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        Future<ChatResponse> primaryTask = executor.submit(() -> primary.call(prompt));
        try {
            ChatResponse fast = primaryTask.get(
                    hedgeDelay.toMillis(), TimeUnit.MILLISECONDS);
            BuzhouMetricsHolder.metrics().counter(COUNTER_PRIMARY_WON, 1);
            return fast;
        } catch (java.util.concurrent.TimeoutException notYetBack) {
            // 长尾：发对冲，先回先得
            BuzhouMetricsHolder.metrics().counter(COUNTER_FIRED, 1);
            return race(primaryTask, prompt);
        } catch (java.util.concurrent.ExecutionException primaryFastError) {
            // 快速失败：不等满延迟，立即转对冲（对冲仍算 fired）
            BuzhouMetricsHolder.metrics().counter(COUNTER_FIRED, 1);
            return afterPrimaryFailure(primaryTask, prompt, primaryFastError.getCause());
        } catch (InterruptedException e) {
            primaryTask.cancel(true);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("对冲等待被中断", e);
        }
    }

    /** 两路竞速：先回先得，输家取消。 */
    private ChatResponse race(Future<ChatResponse> primaryTask, Prompt prompt) {
        Future<ChatResponse> hedgeTask = executor.submit(() -> hedge.call(prompt));
        while (true) {
            if (primaryTask.isDone()) {
                hedgeTask.cancel(true);
                try {
                    BuzhouMetricsHolder.metrics().counter(COUNTER_PRIMARY_WON, 1);
                    return primaryTask.get();
                } catch (Exception primaryFailed) {
                    // 主在对冲后失败：等对冲结果
                    return awaitHedgeAfterPrimaryFailure(hedgeTask, primaryFailed);
                }
            }
            if (hedgeTask.isDone()) {
                primaryTask.cancel(true);
                try {
                    BuzhouMetricsHolder.metrics().counter(COUNTER_HEDGE_WON, 1);
                    return hedgeTask.get();
                } catch (Exception hedgeFailed) {
                    return awaitPrimaryAfterHedgeFailure(primaryTask, hedgeFailed);
                }
            }
            sleepQuietly(5);
        }
    }

    /** 主快速失败后的对冲：单等备模型；备也败 → 抛主因。 */
    private ChatResponse afterPrimaryFailure(Future<ChatResponse> deadPrimary,
                                             Prompt prompt, Throwable primaryCause) {
        deadPrimary.cancel(true);
        Future<ChatResponse> hedgeTask = executor.submit(() -> hedge.call(prompt));
        try {
            BuzhouMetricsHolder.metrics().counter(COUNTER_HEDGE_WON, 1);
            return hedgeTask.get();
        } catch (Exception hedgeAlsoFailed) {
            throw asRuntime(primaryCause == null
                    ? new IllegalStateException("对冲双败") : primaryCause);
        }
    }

    private ChatResponse awaitHedgeAfterPrimaryFailure(Future<ChatResponse> hedgeTask,
                                                       Throwable primaryCause) {
        try {
            BuzhouMetricsHolder.metrics().counter(COUNTER_HEDGE_WON, 1);
            return hedgeTask.get();
        } catch (Exception hedgeAlsoFailed) {
            throw asRuntime(primaryCause);
        }
    }

    private ChatResponse awaitPrimaryAfterHedgeFailure(Future<ChatResponse> primaryTask,
                                                       Throwable hedgeCause) {
        try {
            BuzhouMetricsHolder.metrics().counter(COUNTER_PRIMARY_WON, 1);
            return primaryTask.get();
        } catch (Exception primaryAlsoFailed) {
            throw asRuntime(primaryAlsoFailed.getCause() == null
                    ? primaryAlsoFailed : primaryAlsoFailed.getCause());
        }
    }

    /** stream 诚实委派主模型（流竞速复杂度不成比例——spec 137 显式不做）。 */
    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return primary.stream(prompt);
    }

    @Override
    public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
        return primary.getOptions();
    }

    private static RuntimeException asRuntime(Throwable t) {
        return t instanceof RuntimeException r ? r
                : new IllegalStateException("对冲双败", t);
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("对冲竞速被中断", e);
        }
    }
}
