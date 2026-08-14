package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import io.github.chyuan_cuihongyuan.buzhou.core.session.TurnDeadline;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FaultInjectingToolCallback;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class HarnessToolCallingManagerTest {

    private ToolCallback tool(String name, long sleepMillis, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                if (sleepMillis > 0) {
                    try {
                        Thread.sleep(sleepMillis);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                return result;
            }
        };
    }

    private HarnessToolCallingManager manager(Map<String, String> serialGroups) {
        return manager(serialGroups, Duration.ofSeconds(1));
    }

    private HarnessToolCallingManager manager(Map<String, String> serialGroups, Duration toolTimeout) {
        return manager(serialGroups, toolTimeout, 8);
    }

    private HarnessToolCallingManager manager(Map<String, String> serialGroups, Duration toolTimeout,
                                              int maxConcurrencyPerTurn) {
        return new HarnessToolCallingManager(
                DefaultToolCallingManager.builder().build(),
                Executors.newVirtualThreadPerTaskExecutor(), maxConcurrencyPerTurn, toolTimeout,
                serialGroups);
    }

    private ToolExecutionResult execute(HarnessToolCallingManager manager,
                                        List<ToolCallback> tools, AssistantMessage.ToolCall... calls) {
        AssistantMessage assistant = AssistantMessage.builder()
                .content("").toolCalls(List.of(calls)).build();
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(tools).build();
        return manager.executeToolCalls(new Prompt(List.of(), options), response);
    }

    private AssistantMessage.ToolCall call(String id, String name) {
        return new AssistantMessage.ToolCall(id, "function", name, "{}");
    }

    private AssistantMessage.ToolCall call(String id, String name, String arguments) {
        return new AssistantMessage.ToolCall(id, "function", name, arguments);
    }

    @Test
    void parallelExecutionTakesMaxNotSum() {
        HarnessToolCallingManager manager = manager(Map.of());
        List<ToolCallback> tools = List.of(
                tool("slow_a", 300, "ra"), tool("slow_b", 300, "rb"), tool("slow_c", 300, "rc"));

        long start = System.nanoTime();
        ToolExecutionResult result = execute(manager, tools,
                call("1", "slow_a"), call("2", "slow_b"), call("3", "slow_c"));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(600);
        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(responses.getResponses())
                .extracting(ToolResponseMessage.ToolResponse::id)
                .containsExactly("1", "2", "3");
    }

    @Test
    void timeoutConvertsToTextWithoutBlockingSiblings() {
        HarnessToolCallingManager manager = manager(Map.of());
        List<ToolCallback> tools = List.of(
                tool("hang", 5000, "never"), tool("fast", 0, "quick"));

        ToolExecutionResult result = execute(manager, tools,
                call("1", "hang"), call("2", "fast"));

        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(responses.getResponses().get(0).responseData()).contains("执行超时");
        assertThat(responses.getResponses().get(1).responseData()).isEqualTo("quick");
    }

    @Test
    void exceptionConvertsToFailureText() {
        ToolCallback broken = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("broken").description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                throw new IllegalStateException("boom");
            }
        };
        ToolExecutionResult result = execute(manager(Map.of()), List.of(broken), call("1", "broken"));

        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(responses.getResponses().getFirst().responseData()).contains("执行失败");
    }

    @Test
    void serialGroupNeverRunsConcurrently() {
        AtomicInteger concurrent = new AtomicInteger();
        AtomicLong maxSeen = new AtomicLong();
        ToolCallback groupToolA = slowProbe("ga", concurrent, maxSeen);
        ToolCallback groupToolB = slowProbe("gb", concurrent, maxSeen);

        HarnessToolCallingManager manager = manager(Map.of("ga", "db", "gb", "db"));
        execute(manager, List.of(groupToolA, groupToolB), call("1", "ga"), call("2", "gb"));

        assertThat(maxSeen.get()).isEqualTo(1);
    }

    private ToolCallback slowProbe(String name, AtomicInteger concurrent, AtomicLong maxSeen) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                int now = concurrent.incrementAndGet();
                maxSeen.accumulateAndGet(now, Math::max);
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                concurrent.decrementAndGet();
                return "ok";
            }
        };
    }

    @Test
    void cancelInterruptsInFlightCalls() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        ToolCallback slow = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("slow").description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                started.countDown();
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "interrupted";
                }
                return "done";
            }
        };
        HarnessToolCallingManager manager = manager(Map.of());

        var future = Executors.newVirtualThreadPerTaskExecutor()
                .submit(() -> execute(manager, List.of(slow), call("1", "slow")));
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();
        manager.cancelInFlight();

        ToolExecutionResult result = future.get(5, TimeUnit.SECONDS);
        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        // T16 后取消路径为结构化反馈文案（含「执行已取消」子串）；工具自身中断分支仍返回 interrupted。
        assertThat(responses.getResponses().getFirst().responseData())
                .containsAnyOf("interrupted", "执行已取消");
    }

    // ---- impl-28 / spec 13 §core-2：Turn Deadline 挂起免疫 ----

    private static final Duration TIGHT_DEADLINE = Duration.ofMillis(200L);
    /** 时间断言上限（CI 宽松系数 ~15x：断言「有限时间内收尾」而非精确计时）。 */
    private static final long BOUNDED_ASSERT_MILLIS = 3_000L;

    @Test
    void deadlineDefaultsToNoneUntilBeginTurn() {
        assertThat(manager(Map.of()).turnDeadline()).isEqualTo(TurnDeadline.none());
    }

    @Test
    void outerJoinDeadlineCutsHangingDispatchWithTimeoutFeedback() {
        // 挂起点①（外层 join）：派发队列吞掉任务永不执行——只有外层 join 的 Deadline 兜底能收尾
        HarnessToolCallingManager manager = new HarnessToolCallingManager(
                DefaultToolCallingManager.builder().build(),
                neverRunningExecutor(), 8, Duration.ofSeconds(60L), Map.of());
        manager.beginTurn(TurnDeadline.in(TIGHT_DEADLINE));

        long start = System.nanoTime();
        ToolExecutionResult result = execute(manager, List.of(), call("1", "any_tool"));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(BOUNDED_ASSERT_MILLIS);
        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        // 外层 join 超时回喂：TIMEOUT 语义、含「执行超时」词汇与「Turn 剩余预算耗尽」细节
        assertThat(responses.getResponses().getFirst().responseData())
                .contains("执行超时")
                .contains("Turn 剩余预算耗尽");
    }

    @Test
    void uninterruptibleHangToolIsBoundedByDeadlineAndFeedsBack() {
        // 不响应中断的挂死工具（hangForever 吞中断）：派发时限 = min(60s 单工具超时, Deadline 剩余)
        FaultInjectingToolCallback hangTool =
                new FaultInjectingToolCallback(FaultInjectingToolCallback.FaultSpec.hanging());
        HarnessToolCallingManager manager = manager(Map.of(), Duration.ofSeconds(60L));
        manager.beginTurn(TurnDeadline.in(TIGHT_DEADLINE));

        long start = System.nanoTime();
        ToolExecutionResult result = execute(manager, List.of(hangTool),
                call("1", FaultInjectingToolCallback.DEFAULT_TOOL_NAME));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(BOUNDED_ASSERT_MILLIS);
        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(responses.getResponses().getFirst().responseData())
                .contains("执行超时")
                .contains(FaultInjectingToolCallback.DEFAULT_TOOL_NAME);
        // 工具确实被派发过（挂死在执行中，而非未派发）
        assertThat(hangTool.invocations()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void serialGroupLockWaitIsBoundedByDeadline() {
        // 挂起点②（组锁 tryLock(剩余)）：同组两个慢工具并发提交，先持锁者占满预算，
        // 等锁者在 Deadline 处限时失败（串行组超时或剩余耗尽未派发——两者同为 TIMEOUT 词汇）
        HarnessToolCallingManager manager = manager(Map.of("slow_a", "db", "slow_b", "db"),
                Duration.ofSeconds(10L));
        manager.beginTurn(TurnDeadline.in(TIGHT_DEADLINE));

        long start = System.nanoTime();
        ToolExecutionResult result = execute(manager,
                List.of(tool("slow_a", 500, "ra"), tool("slow_b", 500, "rb")),
                call("1", "slow_a"), call("2", "slow_b"));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(BOUNDED_ASSERT_MILLIS);
        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(responses.getResponses()).hasSize(2);
        responses.getResponses().forEach(r ->
                assertThat(r.responseData()).contains("执行超时"));
    }

    @Test
    void permitWaitIsBoundedByDeadline() {
        // 挂起点③（许可 tryAcquire(剩余)）：并发上限 1，两个慢工具并发提交——
        // 等许可者在 Deadline 处限时失败（许可超时或剩余耗尽未派发——同为 TIMEOUT 词汇）
        HarnessToolCallingManager manager = manager(Map.of(), Duration.ofSeconds(10L), 1);
        manager.beginTurn(TurnDeadline.in(TIGHT_DEADLINE));

        long start = System.nanoTime();
        ToolExecutionResult result = execute(manager,
                List.of(tool("slow_a", 500, "ra"), tool("slow_b", 500, "rb")),
                call("1", "slow_a"), call("2", "slow_b"));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(BOUNDED_ASSERT_MILLIS);
        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(responses.getResponses()).hasSize(2);
        responses.getResponses().forEach(r ->
                assertThat(r.responseData()).contains("执行超时"));
    }

    @Test
    void expiredDeadlineSkipsDispatchEntirely() {
        // 构造即到期（零预算）：不占组锁/许可、不派发，直接 TIMEOUT 回喂
        HarnessToolCallingManager manager = manager(Map.of(), Duration.ofSeconds(10L));
        manager.beginTurn(TurnDeadline.in(Duration.ZERO));
        FaultInjectingToolCallback healthy = new FaultInjectingToolCallback(
                FaultInjectingToolCallback.FaultSpec.none());

        long start = System.nanoTime();
        ToolExecutionResult result = execute(manager, List.of(healthy),
                call("1", FaultInjectingToolCallback.DEFAULT_TOOL_NAME));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(BOUNDED_ASSERT_MILLIS);
        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        // worker 的「未派发」与外层 join 的「已取消」可能竞先——两者同为 TIMEOUT 语义与预算耗尽文案
        assertThat(responses.getResponses().getFirst().responseData())
                .contains("执行超时")
                .contains("Turn 剩余预算");
        assertThat(healthy.invocations()).isZero();
    }

    @Test
    void noDeadlineKeepsLegacyBoundedOnlyByToolTimeout() {
        // 无 Deadline 回归：beginTurn 未调用（none 哨兵）——单工具超时仍是唯一时限（既有行为）
        HarnessToolCallingManager manager = manager(Map.of(), Duration.ofMillis(150L));

        long start = System.nanoTime();
        ToolExecutionResult result = execute(manager, List.of(tool("slow", 5_000, "never")),
                call("1", "slow"));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMs).isLessThan(BOUNDED_ASSERT_MILLIS);
        ToolResponseMessage responses = (ToolResponseMessage) result.conversationHistory().getLast();
        assertThat(responses.getResponses().getFirst().responseData())
                .contains("执行超时")
                .contains("150ms");
    }

    /** 派发队列「吞掉」任务：submit 返回永不完成的 Future——模拟派发后永久无响应。 */
    private static ExecutorService neverRunningExecutor() {
        return new AbstractExecutorService() {
            @Override
            public void execute(Runnable command) {
                // 故意不执行：outer future 永不完成
            }

            @Override
            public void shutdown() {
            }

            @Override
            public List<Runnable> shutdownNow() {
                return List.of();
            }

            @Override
            public boolean isShutdown() {
                return false;
            }

            @Override
            public boolean isTerminated() {
                return false;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) {
                return false;
            }
        };
    }
}
