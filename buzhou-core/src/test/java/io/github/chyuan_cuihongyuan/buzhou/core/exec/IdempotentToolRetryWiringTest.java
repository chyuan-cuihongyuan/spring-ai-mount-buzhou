package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.net.ConnectException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 1511 / T2273–T2274：幂等工具瞬断重试自动装配通道——
 * 幂等门（注解/白名单）三态包装判定 + 装配后瞬断重试端到端 + Holder 未开零包装。
 * 既有 RetryingToolCallback（spec 133）装饰器行为不复测（其测试域），只测通道。
 */
class IdempotentToolRetryWiringTest {

    @AfterEach
    void tearDown() {
        IdempotentToolRetryHolder.Holder.reset();
    }

    /** 幂等注解工具（spec 06 注册模型先例：read_file 同款 idempotent=true）。 */
    @BuzhouTool(name = "probe_read", idempotent = true)
    private static final class IdempotentTool implements ToolCallback {
        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name("probe_read").description("t")
                    .inputSchema("{}").build();
        }

        @Override
        public String call(String toolInput) {
            return "ok";
        }
    }

    /** 非幂等无注解工具。 */
    private static final class PlainTool implements ToolCallback {
        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name("plain").description("t")
                    .inputSchema("{}").build();
        }

        @Override
        public String call(String toolInput) {
            return "ok";
        }
    }

    /** 瞬断两次后成功的幂等工具（重试端到端）。 */
    @BuzhouTool(name = "flaky_read", idempotent = true)
    private static final class FlakyIdempotentTool implements ToolCallback {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name("flaky_read").description("t")
                    .inputSchema("{}").build();
        }

        @Override
        public String call(String toolInput) {
            if (calls.incrementAndGet() <= 2) {
                throw new RuntimeException(new ConnectException("connection reset"));
            }
            return "recovered";
        }
    }

    /** Holder 未开启：wrapIfIdempotent 零包装（原引用透传——用未开启时 null 通道由装配层判空，此处钉 record 级幂等门）。 */
    @Test
    void idempotentGateShouldWrapOnlyAnnotatedOrAllowlisted() {
        IdempotentToolRetryHolder wiring = new IdempotentToolRetryHolder(
                new RetryingToolCallback.RetryPolicy(2, Duration.ofMillis(1), Duration.ofMillis(2), true),
                Set.of("plain"));
        assertThat(wiring.wrapIfIdempotent(new IdempotentTool()))
                .isInstanceOf(RetryingToolCallback.class);
        // 白名单 override：无注解工具入册即包
        assertThat(wiring.wrapIfIdempotent(new PlainTool()))
                .isInstanceOf(RetryingToolCallback.class);
        // 既无注解也不在册：原引用透传
        IdempotentToolRetryHolder noOverride = new IdempotentToolRetryHolder(
                new RetryingToolCallback.RetryPolicy(2, Duration.ofMillis(1), Duration.ofMillis(2), true),
                Set.of());
        assertThat(noOverride.wrapIfIdempotent(new PlainTool()))
                .isNotInstanceOf(RetryingToolCallback.class);
    }

    /** 幂等工具瞬断（IOException 族包装 RuntimeException）两次、第三次成功 → 端到端恢复。 */
    @Test
    void transientFailuresShouldRecoverAfterRetry() {
        IdempotentToolRetryHolder wiring = new IdempotentToolRetryHolder(
                new RetryingToolCallback.RetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(4), true),
                Set.of());
        FlakyIdempotentTool flaky = new FlakyIdempotentTool();
        ToolCallback wrapped = wiring.wrapIfIdempotent(flaky);

        assertThat(wrapped.call("{}")).isEqualTo("recovered");
        assertThat(flaky.calls.get()).isEqualTo(3);
    }

    /** 非瞬断异常零重试原样上抛（参数/业务类——既有语义由装饰器保证，此处通道冒烟）。 */
    @Test
    void nonTransientShouldNotRetry() {
        @BuzhouTool(name = "biz_tool", idempotent = true)
        class BizTool implements ToolCallback {
            final AtomicInteger calls = new AtomicInteger();

            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("biz_tool").description("t")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                calls.incrementAndGet();
                throw new IllegalStateException("业务参数错误");
            }
        }
        BizTool biz = new BizTool();
        IdempotentToolRetryHolder wiring = new IdempotentToolRetryHolder(
                new RetryingToolCallback.RetryPolicy(3, Duration.ofMillis(1), Duration.ofMillis(4), true),
                Set.of());
        assertThatThrownBy(() -> wiring.wrapIfIdempotent(biz).call("{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("业务参数错误");
        assertThat(biz.calls.get()).isEqualTo(1); // 零重试
    }
}
