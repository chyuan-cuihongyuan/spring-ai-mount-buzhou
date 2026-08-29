package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 147 / T502：轮内 memo 回归——复读单执行同值 / 异参各执行 / 轮清零再执行 /
 * 失败不 memo / 统计 / hook 接线。
 */
class TurnMemoTest {

    private ToolCallback countingTool(String name, AtomicInteger calls) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                return name + ":out-" + calls.incrementAndGet();
            }
        };
    }

    @Test
    void repeatedSameArgsExecutesOnceAndReturnsIdenticalValue() {
        TurnMemo memo = new TurnMemo();
        AtomicInteger calls = new AtomicInteger();
        MemoizedToolCallback tool = MemoizedToolCallback.wrap(countingTool("q", calls), memo);

        String first = tool.call("{x:1}");
        String second = tool.call("{x:1}");

        assertThat(calls.get()).isEqualTo(1);
        assertThat(second).isSameAs(first); // 引用一致——轮内自洽
        assertThat(memo.hits()).isEqualTo(1);
        assertThat(memo.misses()).isEqualTo(1);
    }

    @Test
    void differentArgsExecuteSeparately() {
        TurnMemo memo = new TurnMemo();
        AtomicInteger calls = new AtomicInteger();
        MemoizedToolCallback tool = MemoizedToolCallback.wrap(countingTool("q", calls), memo);

        tool.call("{x:1}");
        tool.call("{x:2}");

        assertThat(calls.get()).isEqualTo(2);
        assertThat(memo.size()).isEqualTo(2);
    }

    @Test
    void failureIsNotMemoizedAndRetryExecutes() {
        TurnMemo memo = new TurnMemo();
        AtomicInteger calls = new AtomicInteger();
        ToolCallback flaky = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name("flaky").description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String toolInput) {
                if (calls.incrementAndGet() == 1) {
                    throw new IllegalStateException("transient");
                }
                return "recovered";
            }
        };
        MemoizedToolCallback tool = MemoizedToolCallback.wrap(flaky, memo);

        assertThatThrownBy(() -> tool.call("{}")).isInstanceOf(IllegalStateException.class);
        assertThat(tool.call("{}")).isEqualTo("recovered"); // 失败未 memo——重试真执行
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void hookClearsMemoPerTurn() {
        TurnMemo memo = new TurnMemo();
        TurnMemoHook hook = new TurnMemoHook(memo);
        HookEnvironment env = new HookEnvironment("s1", "agent", new InMemorySessionStateStore());
        AtomicInteger calls = new AtomicInteger();
        MemoizedToolCallback tool = MemoizedToolCallback.wrap(countingTool("q", calls), memo);

        tool.call("{x:1}");
        tool.call("{x:1}");
        assertThat(calls.get()).isEqualTo(1);

        hook.beforeTurn(new DefaultTurnContext(env, "next turn")); // 轮清零
        tool.call("{x:1}");
        assertThat(calls.get()).isEqualTo(2); // 新轮新执行
        assertThat(memo.size()).isEqualTo(1);
    }

    @Test
    void whitespaceEquivalentArgsHashIdentically() {
        // argsHash 口径：strip 后哈希——空白差不构成不同键（与事件日志幂等键同语义）
        TurnMemo memo = new TurnMemo();
        AtomicInteger calls = new AtomicInteger();
        MemoizedToolCallback tool = MemoizedToolCallback.wrap(countingTool("q", calls), memo);

        tool.call("{}");
        tool.call("  {}  ");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void wrapValidatesArguments() {
        assertThatThrownBy(() -> MemoizedToolCallback.wrap(null, new TurnMemo()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MemoizedToolCallback.wrap(
                countingTool("q", new AtomicInteger()), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new TurnMemoHook(null).beforeTurn(new DefaultTurnContext(
                new HookEnvironment("s", "a", new InMemorySessionStateStore()), "x")))
                .isEqualTo(HookResult.CONTINUE); // null memo 兜底空表
    }
}
