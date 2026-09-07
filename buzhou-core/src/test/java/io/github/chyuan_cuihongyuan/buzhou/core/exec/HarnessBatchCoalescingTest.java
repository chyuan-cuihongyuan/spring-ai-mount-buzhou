package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 300 / impl-323：批内合并接线回归——同参合并执行一次且各回喂位 id 一致 /
 * 异参不合并 / 默认关零变化 / 取消桥接中断底层任务。
 */
class HarnessBatchCoalescingTest {

    private static final long POLL_TIMEOUT_MILLIS = 5000L;
    private static final long LATCH_TIMEOUT_SECONDS = 5L;

    /** 计数工具（无阻塞）：返回 name:ok。 */
    private ToolCallback countingTool(String name, AtomicInteger invocations) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                invocations.incrementAndGet();
                return name + ":ok";
            }
        };
    }

    /** 阻塞工具：进入即 started 计数，挂起等 release——保证合并窗口确定敞开。 */
    private ToolCallback blockingTool(String name, AtomicInteger invocations,
                                      CountDownLatch started, CountDownLatch release) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                invocations.incrementAndGet();
                started.countDown();
                try {
                    if (!release.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("测试释放闩超时");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return name + ":interrupted";
                }
                return name + ":ok";
            }
        };
    }

    private HarnessToolCallingManager newManager() {
        return new HarnessToolCallingManager(
                DefaultToolCallingManager.builder().build(),
                Executors.newVirtualThreadPerTaskExecutor(), 8, Duration.ofSeconds(5), Map.of());
    }

    private ToolExecutionResult dispatch(HarnessToolCallingManager manager,
                                         List<ToolCallback> tools,
                                         AssistantMessage.ToolCall... calls) {
        AssistantMessage assistant = AssistantMessage.builder()
                .content("").toolCalls(List.of(calls)).build();
        ChatResponse response = new ChatResponse(List.of(new Generation(assistant)));
        ToolCallingChatOptions options = ToolCallingChatOptions.builder()
                .toolCallbacks(tools).build();
        return manager.executeToolCalls(new Prompt(List.of(), options), response);
    }

    private AssistantMessage.ToolCall toolCallOf(String id, String name, String arguments) {
        return new AssistantMessage.ToolCall(id, "function", name, arguments);
    }

    private ToolResponseMessage responsesOf(ToolExecutionResult result) {
        return (ToolResponseMessage) result.conversationHistory().getLast();
    }

    /** 有限时轮询（确定性等待——直到条件成立或超时失败）。 */
    private static void awaitTrue(java.util.function.BooleanSupplier condition)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        while (!condition.getAsBoolean()) {
            assertThat(System.currentTimeMillis() < deadline)
                    .as("轮询条件超时（%dms）", POLL_TIMEOUT_MILLIS).isTrue();
            Thread.sleep(10L);
        }
    }

    @Test
    void shouldExecuteOnceAndRewriteIds_whenBatchHasIdenticalCalls() throws Exception {
        HarnessToolCallingManager manager = newManager();
        ToolCallCoalescer coalescer = new ToolCallCoalescer();
        manager.setBatchCoalescer(coalescer);
        AtomicInteger invocations = new AtomicInteger();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        ExecutorService testRunner = Executors.newVirtualThreadPerTaskExecutor();
        Future<ToolExecutionResult> running = testRunner.submit(() -> dispatch(manager,
                List.of(blockingTool("q", invocations, started, release)),
                toolCallOf("1", "q", "{}"), toolCallOf("2", "q", "{}"),
                toolCallOf("3", "q", "{}")));

        started.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // 三个提交位全部入窗（首执行已起、后两位已折叠）后才放行——确定性前提
        awaitTrue(() -> coalescer.coalescedCount() >= 2L);
        assertThat(invocations.get()).as("窗口内只有首达者真实执行").isEqualTo(1);
        release.countDown();

        ToolResponseMessage responses = responsesOf(running.get());
        assertThat(invocations.get()).isEqualTo(1);
        assertThat(coalescer.coalescedCount()).isEqualTo(2L);
        assertThat(responses.getResponses())
                .extracting(ToolResponseMessage.ToolResponse::id)
                .containsExactly("1", "2", "3");
        assertThat(responses.getResponses())
                .extracting(ToolResponseMessage.ToolResponse::responseData)
                .containsExactly("q:ok", "q:ok", "q:ok");
        testRunner.shutdownNow();
    }

    @Test
    void shouldNotCoalesce_whenArgsDiffer() {
        HarnessToolCallingManager manager = newManager();
        ToolCallCoalescer coalescer = new ToolCallCoalescer();
        manager.setBatchCoalescer(coalescer);
        AtomicInteger invocations = new AtomicInteger();

        ToolExecutionResult result = dispatch(manager,
                List.of(countingTool("q", invocations)),
                toolCallOf("1", "q", "{\"k\":\"a\"}"),
                toolCallOf("2", "q", "{\"k\":\"b\"}"));

        assertThat(invocations.get()).isEqualTo(2);
        assertThat(coalescer.coalescedCount()).isZero();
        assertThat(responsesOf(result).getResponses())
                .extracting(ToolResponseMessage.ToolResponse::id)
                .containsExactly("1", "2");
    }

    @Test
    void shouldExecuteEachCall_whenCoalescerAbsent() {
        HarnessToolCallingManager manager = newManager();
        AtomicInteger invocations = new AtomicInteger();

        ToolExecutionResult result = dispatch(manager,
                List.of(countingTool("q", invocations)),
                toolCallOf("1", "q", "{}"), toolCallOf("2", "q", "{}"));

        assertThat(invocations.get()).as("默认关 = 既有 per-tool 行为").isEqualTo(2);
        assertThat(responsesOf(result).getResponses())
                .extracting(ToolResponseMessage.ToolResponse::responseData)
                .containsExactly("q:ok", "q:ok");
    }

    @Test
    void shouldInterruptUnderlyingTask_whenSharedFutureCancelled() throws Exception {
        ToolCallCoalescer coalescer = new ToolCallCoalescer();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(false);

        java.util.concurrent.CompletableFuture<String> shared = coalescer.submit("k", () -> {
            taskStarted.countDown();
            try {
                release.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrupted.set(true);
                Thread.currentThread().interrupt();
            }
            return "never";
        }, executor);

        taskStarted.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        shared.cancel(true);

        awaitTrue(interrupted::get);
        assertThat(interrupted.get())
                .as("取消桥接：共享 Future cancel 须中断底层任务").isTrue();
        executor.shutdownNow();
    }
}
