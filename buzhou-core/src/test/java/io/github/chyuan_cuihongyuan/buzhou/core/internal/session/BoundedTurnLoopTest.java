package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.TurnLoopContext;
import io.github.chyuan_cuihongyuan.buzhou.core.session.TurnLoopPolicy;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T17「有界 Turn + 可组合停止条件」（docs/spec/11 core）：
 * 模型陷入工具调用死循环时，Turn 在预算内终止并产出优雅最终回复（非崩溃、非无限烧 token）；
 * 停止条件可组合（预算 & 超时 & 自定义信号，JDK Predicate and/or）；正常单轮/多轮行为不回归。
 */
class BoundedTurnLoopTest {

    @Test
    void infiniteToolLoopTerminatesWithinBudgetWithGracefulFinal() {
        InfiniteToolCallModel model = new InfiniteToolCallModel();
        CountingTool tool = new CountingTool();
        RuntimeConfig config = RuntimeConfig.turnLoopPolicy(TurnLoopPolicy.of(3));
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(), config, tool);

        List<SessionEvent> events = new ArrayList<>();
        AgentSession session = runtime.spawn("bound-app", "looper", "bound-sess");
        session.addEventListener(events::add);
        String reply = session.chat("一直查下去");
        session.close();

        // 优雅收尾：非崩溃、回复为兜底文案（默认 handler 产出）
        assertThat(reply).contains("预算内收尾");
        // 预算硬上界：工具恰好执行 3 轮（第 4 轮被替换为优雅最终），模型被调 4 次
        assertThat(tool.invocations.get()).isEqualTo(3);
        assertThat(model.calls.get()).isEqualTo(4);
        // 停止可观测：发出 turn.loop.bounded 事件
        assertThat(events).anyMatch(e -> e.type().equals("turn.loop.bounded"));
    }

    @Test
    void customGracefulFinalizerProducesConfiguredFinal() {
        InfiniteToolCallModel model = new InfiniteToolCallModel();
        CountingTool tool = new CountingTool();
        TurnLoopPolicy policy = new TurnLoopPolicy(1, null, List.of(),
                ctx -> "自定义收尾：已跑 " + ctx.executedToolRounds() + " 轮");
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                RuntimeConfig.turnLoopPolicy(policy), tool);

        AgentSession session = runtime.spawn("bound-app", "looper", "custom-final-sess");
        String reply = session.chat("继续");
        session.close();

        assertThat(reply).isEqualTo("自定义收尾：已跑 1 轮");
        assertThat(tool.invocations.get()).isEqualTo(1);
    }

    @Test
    void timeoutConditionStopsLongRunningLoop() {
        InfiniteToolCallModel model = new InfiniteToolCallModel();
        CountingTool tool = new CountingTool(120);
        TurnLoopPolicy policy = new TurnLoopPolicy(null, Duration.ofMillis(300), List.of(), null);
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                RuntimeConfig.turnLoopPolicy(policy), tool);

        AgentSession session = runtime.spawn("bound-app", "looper", "timeout-sess");
        String reply = session.chat("慢速循环");
        session.close();

        assertThat(reply).contains("预算内收尾");
        // 超时生效：远早于默认 40 轮上限就停了（每轮 120ms，300ms ≈ 2-3 轮）
        assertThat(tool.invocations.get()).isLessThan(10);
    }

    @Test
    void composableStopConditionsSupportAndOr() {
        // 纯单测：Predicate 组合语义（and / or）与内置条件工厂
        AtomicReference<Boolean> externalCancelFlag = new AtomicReference<>(false);
        TurnLoopContext ctx = contextOf(2, 3, Duration.ofSeconds(5));

        // or：轮数预算 或 外部取消，任一命中即停
        var budgetOrCancel = TurnLoopPolicy.maxToolRounds(2)
                .or(c -> externalCancelFlag.get());
        assertThat(budgetOrCancel.test(ctx)).isTrue(); // nextToolRound(3) > 2

        // and：轮数预算 且 外部取消，两者同时满足才停
        externalCancelFlag.set(true);
        var budgetAndCancel = TurnLoopPolicy.maxToolRounds(2)
                .and(c -> externalCancelFlag.get());
        assertThat(budgetAndCancel.test(ctx)).isTrue(); // 3>2 且 取消旗标开

        externalCancelFlag.set(false);
        assertThat(budgetAndCancel.test(ctx)).isFalse(); // 取消旗标关 → and 不命中（预算条件虽真）
        assertThat(TurnLoopPolicy.<TurnLoopContext>loopTimeout(Duration.ofSeconds(60)).test(ctx)).isFalse();
    }

    @Test
    void policyShouldStopHonorsAllChannels() {
        TurnLoopContext ctx = contextOf(1, 2, Duration.ofSeconds(1));
        assertThat(TurnLoopPolicy.of(1).shouldStop(ctx)).isTrue(); // 第 2 轮 > 上界 1
        assertThat(TurnLoopPolicy.of(5).shouldStop(ctx)).isFalse();
        assertThat(new TurnLoopPolicy(null, Duration.ofMillis(500), List.of(), null).shouldStop(ctx)).isTrue();
        assertThat(new TurnLoopPolicy(null, null,
                List.of(c -> c.nextToolRound() >= 2), null).shouldStop(ctx)).isTrue();
        assertThat(TurnLoopPolicy.unbounded().shouldStop(ctx)).isFalse();
    }

    @Test
    void normalToolTurnUnaffectedByDefaultBound() {
        // 正常 1 轮工具 + 文本收尾：默认策略（40 轮）下行为不回归
        ReactiveOnceModel model = new ReactiveOnceModel();
        CountingTool tool = new CountingTool();
        AgentRuntime runtime = Buzhou.runtime(model, Buzhou.inMemoryStores(),
                RuntimeConfig.defaults(), tool);

        AgentSession session = runtime.spawn("bound-app", "looper", "normal-sess");
        String reply = session.chat("查一次");
        session.close();

        assertThat(reply).isEqualTo("查完了：round-1");
        assertThat(tool.invocations.get()).isEqualTo(1);
    }

    private static TurnLoopContext contextOf(int executed, int next, Duration elapsed) {
        return new TurnLoopContext() {
            @Override
            public int executedToolRounds() {
                return executed;
            }

            @Override
            public int nextToolRound() {
                return next;
            }

            @Override
            public Duration elapsed() {
                return elapsed;
            }

            @Override
            public String sessionId() {
                return "s";
            }

            @Override
            public String agentName() {
                return "a";
            }
        };
    }

    /** 死循环模拟模型：永远只发工具调用、永不给文本（runaway 语义）。 */
    static final class InfiniteToolCallModel implements ChatModel {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            calls.incrementAndGet();
            int n = calls.get();
            return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                    .content("").toolCalls(List.of(new AssistantMessage.ToolCall(
                            "tc-" + n, "function", "echo_tool", "{}"))).build())));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    /** 正常反应式模型：首次调工具、见到工具结果后给文本。 */
    static final class ReactiveOnceModel implements ChatModel {
        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            List<Message> msgs = prompt.getInstructions();
            Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            if (last instanceof ToolResponseMessage) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("查完了：round-1"))));
            }
            return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                    .content("").toolCalls(List.of(new AssistantMessage.ToolCall(
                            "tc", "function", "echo_tool", "{}"))).build())));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    static final class CountingTool implements ToolCallback {
        final AtomicInteger invocations = new AtomicInteger();
        private final long sleepMillis;

        CountingTool() {
            this(0);
        }

        CountingTool(long sleepMillis) {
            this.sleepMillis = sleepMillis;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return ToolDefinition.builder().name("echo_tool").description("d")
                    .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
        }

        @Override
        public String call(String toolInput) {
            invocations.incrementAndGet();
            if (sleepMillis > 0) {
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return "round-" + invocations.get();
        }
    }
}
