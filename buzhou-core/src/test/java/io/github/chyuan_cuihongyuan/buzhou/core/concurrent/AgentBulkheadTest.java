package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 84 §B / T324：agent 并发 Turn 隔离舱红队——默认不限（NOOP 零行为）；
 * 上限拒绝（QUOTA_EXCEEDED fail-fast）；释放后复得；等待超时档；端到端（chat 并发
 * 一成一拒）。借鉴：resilience4j Bulkhead（per-resource 并发隔离——热点 agent 不
 * 吃光全实例模型吞吐；spawn 闸限会话数，本舱限在飞 Turn 数，正交）。
 */
class AgentBulkheadTest {

    @AfterEach
    void cleanup() {
        AgentBulkhead.install(null);
    }

    @Test
    void unlimitedByDefaultAndNoopForUnconfiguredAgent() {
        assertThat(AgentBulkhead.global().limitOf("any-agent"))
                .isEqualTo(Integer.MAX_VALUE);
        try (AgentBulkhead.Lease lease = AgentBulkhead.global().acquire("any-agent")) {
            assertThat(lease).isSameAs(AgentBulkhead.Lease.NOOP);
        }
        try (AgentBulkhead.Lease second = AgentBulkhead.global().acquire("any-agent")) {
            assertThat(AgentBulkhead.global().inFlight("any-agent")).isZero(); // NOOP 不计数
        }
    }

    @Test
    void limitRejectsSecondConcurrentTurnUntilReleased() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("hot", 1), Duration.ZERO);
        AgentBulkhead.install(bulkhead);

        try (AgentBulkhead.Lease first = bulkhead.acquire("hot")) {
            assertThat(bulkhead.inFlight("hot")).isEqualTo(1);
            assertThatThrownBy(() -> bulkhead.acquire("hot"))
                    .isInstanceOf(BuzhouException.class)
                    .hasMessageContaining("agent=hot")
                    .hasMessageContaining("limit=1")
                    .extracting(e -> ((BuzhouException) e).errorCode())
                    .isEqualTo(ErrorCode.QUOTA_EXCEEDED);
        }
        assertThat(bulkhead.inFlight("hot")).isZero();
        try (AgentBulkhead.Lease again = bulkhead.acquire("hot")) { // 释放后复得
            assertThat(bulkhead.inFlight("hot")).isEqualTo(1);
        }
    }

    @Test
    void waitTimeoutThenReject() {
        AgentBulkhead bulkhead = AgentBulkhead.of(Map.of("waiter", 1), Duration.ofMillis(50));
        AgentBulkhead.Lease held = bulkhead.acquire("waiter");
        long start = System.nanoTime();
        assertThatThrownBy(() -> bulkhead.acquire("waiter"))
                .isInstanceOf(BuzhouException.class);
        long waitedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(waitedMs).isGreaterThanOrEqualTo(40); // 真等过（容差时钟粒度）
        held.close();
    }

    /** 端到端：同 agent 两会话并发 chat——模型挂起期间舱满，第二个 QUOTA_EXCEEDED。 */
    @Test
    void concurrentChatsOnSameAgentShareBulkhead() throws Exception {
        CountDownLatch modelEntered = new CountDownLatch(1);
        CountDownLatch releaseModel = new CountDownLatch(1);
        AtomicInteger rejected = new AtomicInteger();
        ScriptedChatModel blocking = new ScriptedChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                modelEntered.countDown();
                try {
                    releaseModel.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return new ChatResponse(java.util.List.of(
                        new Generation(new AssistantMessage("done"))));
            }
        };
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(blocking, stores, RuntimeConfig.defaults());
        AgentBulkhead.install(AgentBulkhead.of(Map.of("shared", 1), Duration.ZERO));

        Thread first = new Thread(() -> {
            try (var session = runtime.spawn("app", "shared", "bulk-a")) {
                session.chat("hold");
            } catch (RuntimeException e) {
                // 第一路不应被拒（若被拒则计入 rejected——测试会失败）
                rejected.incrementAndGet();
            }
        });
        first.start();
        assertThat(modelEntered.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();

        try (var session = runtime.spawn("app", "shared", "bulk-b")) {
            assertThatThrownBy(() -> session.chat("second"))
                    .isInstanceOf(BuzhouException.class)
                    .hasMessageContaining("agent=shared");
        }
        releaseModel.countDown();
        first.join(5_000);
        assertThat(rejected.get()).isZero();
    }
}
