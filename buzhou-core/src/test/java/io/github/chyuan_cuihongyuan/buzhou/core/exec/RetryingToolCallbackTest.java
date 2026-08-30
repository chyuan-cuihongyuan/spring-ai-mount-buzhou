package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 133 / T480：幂等工具重试回归——抖动恢复（尝试数正确）/ 耗尽上抛最后异常 /
 * maxAttempts=1 零重试等价裸工具 / 错误文案返回不重试 / 退避时长下限与参数校验。
 */
class RetryingToolCallbackTest {

    /** 前 failFirstN 次抛异常、之后成功的抖动工具。 */
    private ToolCallback flaky(String name, int failFirstN, AtomicInteger attempts) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                if (attempts.incrementAndGet() <= failFirstN) {
                    throw new IllegalStateException("downstream 503");
                }
                return "recovered";
            }
        };
    }

    @Test
    void transientFailuresRecoveredSilently() {
        AtomicInteger attempts = new AtomicInteger();
        RetryingToolCallback retrying = RetryingToolCallback.wrap(
                flaky("query", 2, attempts),
                new RetryingToolCallback.RetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(4)));

        assertThat(retrying.call("{}")).isEqualTo("recovered");
        assertThat(attempts.get()).isEqualTo(3);
        // 装饰器透传定义（名称/schema 不变——装配面零感知）
        assertThat(retrying.getToolDefinition().name()).isEqualTo("query");
    }

    @Test
    void exhaustedRetriesRethrowLastException() {
        AtomicInteger attempts = new AtomicInteger();
        RetryingToolCallback retrying = RetryingToolCallback.wrap(
                flaky("dead", 99, attempts),
                new RetryingToolCallback.RetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(2)));

        assertThatThrownBy(() -> retrying.call("{}", new ToolContext(Map.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("503");
        assertThat(attempts.get()).isEqualTo(3);
    }

    @Test
    void singleAttemptEqualsBareBehavior() {
        AtomicInteger attempts = new AtomicInteger();
        RetryingToolCallback retrying = RetryingToolCallback.wrap(
                flaky("once", 1, attempts),
                new RetryingToolCallback.RetryPolicy(1, Duration.ofMillis(1), Duration.ofMillis(1)));

        assertThatThrownBy(() -> retrying.call("{}"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(attempts.get()).isEqualTo(1);
    }

    @Test
    void errorFeedbackTextIsNotRetried() {
        AtomicInteger attempts = new AtomicInteger();
        ToolCallback semanticFailure = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("biz").description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                attempts.incrementAndGet();
                return ToolErrorFeedback.format("biz", toolInput, "余额不足");
            }
        };
        RetryingToolCallback retrying = RetryingToolCallback.wrap(semanticFailure,
                new RetryingToolCallback.RetryPolicy(5, Duration.ofMillis(1), Duration.ofMillis(2)));

        String result = retrying.call("{}");
        assertThat(result).contains(ToolFeedbackType.EXECUTION_FAILURE.marker());
        assertThat(attempts.get()).isEqualTo(1); // 语义结局不重试
    }

    @Test
    void backoffFloorHeldAndPolicyValidated() {
        AtomicInteger attempts = new AtomicInteger();
        long start = System.nanoTime();
        RetryingToolCallback.wrap(flaky("slow", 2, attempts),
                        new RetryingToolCallback.RetryPolicy(3, Duration.ofMillis(30),
                                Duration.ofMillis(40)))
                .call("{}");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        // 两次重试退避：30 + 60→封顶 40 = 至少 70ms
        assertThat(elapsedMs).isGreaterThanOrEqualTo(70);

        assertThatThrownBy(() -> new RetryingToolCallback.RetryPolicy(0,
                Duration.ofMillis(1), Duration.ofMillis(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RetryingToolCallback.RetryPolicy(3,
                Duration.ofMillis(10), Duration.ofMillis(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
