package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;

/**
 * 幂等工具重试装饰器（spec 133 / T479，Temporal Activity retry policy +
 * Failsafe 退避借鉴）：<b>仅异常触发重试</b>（指数退避 initial×2ⁿ 封顶
 * maxBackoff，最多 maxAttempts 次，耗尽上抛最后异常——harness 既有兜底词汇不变）；
 * 工具正常返回的<b>错误反馈文案</b>是语义结局，不重试。
 *
 * <p><b>幂等性契约（显性）</b>：宿主只包幂等工具（查询/只读类）——非幂等工具
 * 重试有重复副作用风险，责任归声明方。
 *
 * <p>装饰器零侵入：{@link ToolDefinition} 原样透传（名称/schema 不变，装配面
 * 零感知）；与 spec 131 工具熔断正交（重试管瞬时抖动，熔断管持续故障）。
 */
public final class RetryingToolCallback implements ToolCallback {

    /** 重试策略（maxAttempts≥1；backoff 正值；1 = 零重试裸行为）。 */
    public record RetryPolicy(int maxAttempts, Duration initialBackoff, Duration maxBackoff) {
        public RetryPolicy {
            if (maxAttempts < 1 || initialBackoff == null || initialBackoff.isNegative()
                    || maxBackoff == null || maxBackoff.isNegative()
                    || maxBackoff.compareTo(initialBackoff) < 0) {
                throw new IllegalArgumentException(
                        "重试策略非法（maxAttempts>=1、backoff 非负、max>=initial）");
            }
        }

        public static RetryPolicy defaults() {
            return new RetryPolicy(3, Duration.ofMillis(50), Duration.ofMillis(500));
        }
    }

    private static final String RETRY_COUNTER = "buzhou.tool-retry.retries";
    private static final String BUDGET_DENIED_COUNTER = "buzhou.tool-retry.retry-budget-denied";

    private final ToolCallback delegate;
    private final RetryPolicy policy;
    /** spec 302 / impl-325：进程级重试预算（null = 未启用——行为逐位不变）。 */
    private final io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudget retryBudget;

    private RetryingToolCallback(ToolCallback delegate, RetryPolicy policy) {
        this(delegate, policy, null);
    }

    private RetryingToolCallback(ToolCallback delegate, RetryPolicy policy,
            io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudget retryBudget) {
        this.delegate = delegate;
        this.policy = policy;
        this.retryBudget = retryBudget;
    }

    /** 包装（policy null = 默认 3 次/50ms/500ms；预算默认取进程 holder——spec 302）。 */
    public static RetryingToolCallback wrap(ToolCallback delegate, RetryPolicy policy) {
        return new RetryingToolCallback(delegate, policy == null ? RetryPolicy.defaults() : policy,
                io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudgetHolder.current());
    }

    /**
     * 包装（显式预算——测试/编程式覆盖用；null = 本装饰器不参与预算）。
     *
     * @param budget 进程级重试预算（每次 call 存入、每次重试前支取；余额不足即止）
     */
    public static RetryingToolCallback wrap(ToolCallback delegate, RetryPolicy policy,
            io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudget budget) {
        return new RetryingToolCallback(delegate, policy == null ? RetryPolicy.defaults() : policy, budget);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return withRetries(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return withRetries(toolInput, toolContext);
    }

    private String withRetries(String toolInput, ToolContext toolContext) {
        if (retryBudget != null) {
            retryBudget.deposit(); // 流量即预算：每次逻辑调用存入（spec 302）
        }
        RuntimeException last = null;
        for (int attempt = 1; attempt <= policy.maxAttempts(); attempt++) {
            if (attempt > 1) {
                if (retryBudget != null && !retryBudget.tryAcquire()) {
                    // 预算不足即止（spec 302）：不重试、最后异常上抛——预算的意义就是少打一枪
                    BuzhouMetricsHolder.metrics().counter(BUDGET_DENIED_COUNTER, 1,
                            "tool", delegate.getToolDefinition().name());
                    throw last;
                }
                BuzhouMetricsHolder.metrics().counter(RETRY_COUNTER, 1,
                        "tool", delegate.getToolDefinition().name());
                sleep(backoffMillis(attempt));
            }
            try {
                return toolContext == null
                        ? delegate.call(toolInput)
                        : delegate.call(toolInput, toolContext);
            } catch (RuntimeException e) {
                last = e;
            }
        }
        throw last;
    }

    /** 第 attempt 次尝试失败后的退避：initial×2^(attempt-1)，封顶 max。 */
    private long backoffMillis(int attempt) {
        long exponential = policy.initialBackoff().toMillis() << Math.min(attempt - 1, 20);
        return Math.min(exponential, policy.maxBackoff().toMillis());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("重试等待被中断", e);
        }
    }
}
