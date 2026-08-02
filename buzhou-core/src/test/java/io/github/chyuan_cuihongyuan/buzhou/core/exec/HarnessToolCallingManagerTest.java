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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
        return new HarnessToolCallingManager(
                DefaultToolCallingManager.builder().build(),
                Executors.newVirtualThreadPerTaskExecutor(), 8, Duration.ofSeconds(1), serialGroups);
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
        assertThat(responses.getResponses().getFirst().responseData())
                .isIn("interrupted", "执行已取消");
    }
}
