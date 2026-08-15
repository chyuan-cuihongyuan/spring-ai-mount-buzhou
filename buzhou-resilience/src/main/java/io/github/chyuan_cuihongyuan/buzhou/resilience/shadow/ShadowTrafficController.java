package io.github.chyuan_cuihongyuan.buzhou.resilience.shadow;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.fallback.NamedFallbackModel;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * shadow 探测控制器（spec 49 §A / T176 / impl-145）：主模型调用成功后异步把同一 Prompt
 * 发给 shadow 模型做延迟/token 对照——**不回注用户、不重放工具循环**（裸 ChatModel 调用，
 * 工具副作用红线）；护栏：进程级并发信号量 + UTC 日预算池。
 *
 * <p>失败语义：shadow 自身异常吞掉（DEBUG 日志 + outcome=error 计数），绝不拖垮主链路；
 * 提交即返回（用户路径零增延迟）。未启用（默认）零提交零事件零计数。
 */
public final class ShadowTrafficController {

    /** 对照事件（payload：primary/shadow/primaryMs/shadowMs/deltaMs/tokens）。 */
    public static final String EVENT_COMPARED = "shadow.compared";

    private static final System.Logger LOGGER = System.getLogger(ShadowTrafficController.class.getName());

    private final List<NamedFallbackModel> shadowModels;
    private final int maxConcurrent;
    private final long dailyBudget;
    private final Semaphore permits;
    /** UTC 日预算已花费（提交次数口径；dayKey 滚动重置）。 */
    private final AtomicLong daySpent = new AtomicLong();
    private volatile String dayKey = utcDayKey();

    public ShadowTrafficController(List<NamedFallbackModel> shadowModels, ResilienceProperties.Shadow config) {
        this.shadowModels = shadowModels == null ? List.of() : List.copyOf(shadowModels);
        this.maxConcurrent = config == null ? 2 : config.maxConcurrent();
        this.dailyBudget = config == null ? 1000L : config.dailyBudget();
        this.permits = new Semaphore(this.maxConcurrent);
    }

    /** 是否启用（无模型 = 未启用）。 */
    public boolean enabled() {
        return !shadowModels.isEmpty();
    }

    /**
     * 主调用成功后提交对照探测（异步、即发即忘）。护栏顺序：并发 → 日预算；
     * 拦下即计数返回（不排队——shadow 是增益面，不占用户路径资源）。
     */
    public void submit(Prompt prompt, String primaryName, long primaryLatencyMs,
                       Consumer<SessionEvent> emitter) {
        if (!enabled()) {
            return;
        }
        if (!permits.tryAcquire()) {
            shadowCounter("skipped-concurrency");
            return;
        }
        if (!trySpendBudget()) {
            permits.release();
            shadowCounter("skipped-budget");
            return;
        }
        Consumer<SessionEvent> sink = emitter == null ? event -> {
        } : emitter;
        Thread.startVirtualThread(() -> {
            try {
                for (NamedFallbackModel shadow : shadowModels) {
                    long startNs = System.nanoTime();
                    try {
                        ChatResponse response = shadow.model().call(prompt);
                        long shadowMs = (System.nanoTime() - startNs) / 1_000_000;
                        shadowCounter("ok");
                        sink.accept(new SessionEvent(EVENT_COMPARED, Map.of(
                                "primary", primaryName,
                                "shadow", shadow.name(),
                                "primaryMs", primaryLatencyMs,
                                "shadowMs", shadowMs,
                                "deltaMs", shadowMs - primaryLatencyMs,
                                "tokens", totalTokens(response)), Instant.now()));
                    } catch (RuntimeException e) {
                        shadowCounter("error");
                        LOGGER.log(System.Logger.Level.DEBUG,
                                "shadow 探测失败（吞掉，不影响主链路）：shadow=" + shadow.name()
                                        + "，error=" + e.getMessage());
                    }
                }
            } finally {
                permits.release();
            }
        });
    }

    /** 日预算原子扣减（UTC 日滚动；低频路径，synchronized 足够）。 */
    private synchronized boolean trySpendBudget() {
        String today = utcDayKey();
        if (!today.equals(dayKey)) {
            dayKey = today;
            daySpent.set(0);
        }
        if (daySpent.get() >= dailyBudget) {
            return false;
        }
        daySpent.incrementAndGet();
        return true;
    }

    private static String utcDayKey() {
        return LocalDate.ofInstant(Instant.now(), ZoneOffset.UTC).toString();
    }

    private static long totalTokens(ChatResponse response) {
        if (response == null || response.getMetadata() == null) {
            return 0;
        }
        Usage usage = response.getMetadata().getUsage();
        if (usage == null || usage.getTotalTokens() == null) {
            long sum = 0;
            if (usage != null) {
                if (usage.getPromptTokens() != null) {
                    sum += usage.getPromptTokens();
                }
                if (usage.getCompletionTokens() != null) {
                    sum += usage.getCompletionTokens();
                }
            }
            return sum;
        }
        return usage.getTotalTokens();
    }

    private static void shadowCounter(String outcome) {
        BuzhouMetricsHolder.metrics().counter("buzhou.resilience.shadow.calls", 1, "outcome", outcome);
    }
}
