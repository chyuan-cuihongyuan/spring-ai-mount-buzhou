package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouRunawayProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 死循环与失控检测 e2e（spec「死循环与失控检测」）。
 *
 * <p>复用 {@code CrashRecoveryEndToEndTest}/{@code GracefulShutdownEndToEndTest} 装配与手法：
 * {@code Buzhou.runtime(model, stores, config)} + core test-jar {@link ScriptedChatModel}
 * （enqueue 多个 tool-call assistant message）+ {@code session.addEventListener(events::add)}
 * + {@code messageStore.load(sid)} 断言部分结果保留。
 *
 * <p>全程断言「硬顶在正确的步数触发 / 软退出在软阈值注入 / 部分结果落库 / 事件流正确」——
 * 一律从外部观察判定。计时用 CountDownLatch / BlockingChatModel / BlockingTool，不用 wall-clock sleep。
 */
class RunawayEndToEndTest {

    // ---- 装配辅助 ----

    private AgentRuntime runtime(ScriptedChatModel model, BuzhouStores stores,
                                 BuzhouRunawayProperties props, RunawayCounters counters,
                                 ToolCallback... tools) {
        // 传入 observabilityStore 使 runaway.* 事件双重写入（SessionEvent + EventRecord）
        RunawayHook hook = new RunawayHook(props, counters, stores.observabilityStore());
        RuntimeConfig config = new RuntimeConfig(List.of(hook), Set.of(), Set.of(), null, List.of());
        return Buzhou.runtime(model, stores, config, tools);
    }

    private static List<SessionEvent> listen(AgentSession session) {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        return events;
    }

    /** 脚本化「助手发起一次工具调用」。 */
    private static AssistantMessage toolCall(String id, String name) {
        return AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, "{}")))
                .build();
    }

    /** 计数型工具：记录调用次数，返回固定结果。 */
    private static ToolCallback countingTool(String name, AtomicInteger calls, String result) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\"}").build();
            }

            @Override
            public String call(String toolInput) {
                calls.incrementAndGet();
                return result;
            }
        };
    }

    // ---- Issue 01：轮次级步数硬顶 ----

    @Test
    void perTurnStepHardcapStopsAtLimit() {
        ScriptedChatModel model = new ScriptedChatModel();
        // 预排足够多的 tool-call（证明是硬顶掐停，而非脚本耗尽）
        model.enqueue(toolCall("tc-1", "search"));
        model.enqueue(toolCall("tc-2", "search"));
        model.enqueue(toolCall("tc-3", "search"));
        model.enqueue(toolCall("tc-4", "search"));
        model.enqueueText("完成");

        BuzhouStores stores = Buzhou.inMemoryStores();
        AtomicInteger toolCalls = new AtomicInteger();
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                new BuzhouRunawayProperties.PerTurn(3, null, null),
                null, null, null, null, null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", toolCalls, "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-steps");
        List<SessionEvent> events = listen(session);

        String reply = session.chat("反复搜索");
        session.close();

        // 第 4 次模型调用未发起（硬顶在步边界掐停）
        assertThat(model.seenPrompts).hasSize(3);
        // 工具被调 3 次（3 步各一次）
        assertThat(toolCalls.get()).isEqualTo(3);
        // 硬顶事件出现，payload reason=steps、limit/value 正确、含部分结果指针
        SessionEvent hardStop = events.stream()
                .filter(e -> RunawayHook.EVENT_HARD_STOP.equals(e.type())).findFirst().orElseThrow();
        assertThat(hardStop.payload())
                .containsEntry("reason", RunawayHook.REASON_STEPS)
                .containsEntry("limit", 3)
                .containsEntry("value", 4)
                .containsEntry("partialResultRef", "messageStore:sess-steps");
        // 最终回复含终止原因（可解释终止，非黑屏）
        assertThat(reply).contains("单轮步数").contains("3");
        // 部分结果保留：本轮已完成的工具结果随轮次落库（被终止 ≠ 前功尽弃）
        // 照搬 drainForceKillsAndFlushesExitTierBufferedWrites 形态：验证含工具结果（非仅用户消息）
        assertThat(stores.messageStore().load("sess-steps"))
                .as("硬顶后 messageStore 含本轮部分工具结果")
                .anySatisfy(m -> assertThat(m.role()).asString().contains("TOOL"));
    }

    @Test
    void hardStoppedSessionSurvivesNextTurn() {
        ScriptedChatModel model = new ScriptedChatModel();
        // 第一轮：step1 发起工具调用（proceed），step2 被硬顶掐停
        model.enqueue(toolCall("tc-1", "search"));
        // 第二轮：纯文本回复，1 步即完成（1<=max-steps=1，proceed）
        model.enqueueText("第二轮完成");

        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                new BuzhouRunawayProperties.PerTurn(1, null, null),
                null, null, null, null, null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", new AtomicInteger(), "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-survive");

        // 第一轮被硬顶掐停（max-steps=1：step1 工具调用 proceed，step2 被掐）
        String firstReply = session.chat("搜索");
        assertThat(firstReply).contains("单轮步数");

        // 第二轮：轮次计数已重置，会话不废，1 步正常响应
        String secondReply = session.chat("再搜一次");
        assertThat(secondReply).isEqualTo("第二轮完成");
        session.close();
    }

    @Test
    void noLimitByDefaultBehaviorUnchanged() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "search"));
        model.enqueueText("正常完成");

        BuzhouStores stores = Buzhou.inMemoryStores();
        // 所有阈值 null = 不限，等价现状
        BuzhouRunawayProperties props = BuzhouRunawayProperties.defaults();
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", new AtomicInteger(), "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-default");
        List<SessionEvent> events = listen(session);

        String reply = session.chat("搜索");
        session.close();

        // 行为与现状完全一致：模型调用 2 次，无硬顶事件
        assertThat(reply).isEqualTo("正常完成");
        assertThat(model.seenPrompts).hasSize(2);
        assertThat(events).noneMatch(e -> e.type().startsWith("runaway."));
    }

    @Test
    void disabledMechanismBypassesCompletely() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "search"));
        model.enqueueText("正常完成");

        BuzhouStores stores = Buzhou.inMemoryStores();
        // enabled=false：机制完全旁路
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(false,
                new BuzhouRunawayProperties.PerTurn(1, null, null),
                null, null, null, null, null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", new AtomicInteger(), "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-disabled");
        List<SessionEvent> events = listen(session);

        String reply = session.chat("搜索");
        session.close();

        // 即使配了 max-steps=1，enabled=false 也完全旁路（2 次调用都执行）
        assertThat(reply).isEqualTo("正常完成");
        assertThat(model.seenPrompts).hasSize(2);
        assertThat(events).noneMatch(e -> e.type().startsWith("runaway."));
    }

    // ---- Issue 02：软退出通道（soft-threshold 事件）----

    @Test
    void softThresholdEventFiresWhenRemainingBelowRatio() {
        ScriptedChatModel model = new ScriptedChatModel();
        // maxSteps=5, ratio=0.5 → 剩余 < 2.5（step>=3）时触发软阈值；step 6 触发硬顶
        for (int i = 1; i <= 6; i++) {
            model.enqueue(toolCall("tc-" + i, "search"));
        }
        model.enqueueText("完成");

        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                new BuzhouRunawayProperties.PerTurn(5, null, null),
                null, null, 0.5, null, null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", new AtomicInteger(), "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-soft");
        List<SessionEvent> events = listen(session);

        session.chat("反复搜索");
        session.close();

        // 软阈值事件出现：step=3 时 remaining=2 < 2.5 触发
        SessionEvent soft = events.stream()
                .filter(e -> RunawayHook.EVENT_SOFT_THRESHOLD.equals(e.type())).findFirst().orElseThrow();
        assertThat(soft.payload())
                .containsEntry("counter", 3)
                .containsEntry("limit", 5)
                .containsEntry("remaining", 2);
        // 每轮仅首次触发（step 4、5 不重复发）
        long softCount = events.stream()
                .filter(e -> RunawayHook.EVENT_SOFT_THRESHOLD.equals(e.type())).count();
        assertThat(softCount).isEqualTo(1);
        // 硬顶仍然触发（step 6）
        assertThat(events).anyMatch(e -> RunawayHook.EVENT_HARD_STOP.equals(e.type())
                && RunawayHook.REASON_STEPS.equals(e.payload().get("reason")));
        // 事件序：soft-threshold 在 hard-stop 之前
        int softIdx = indexOf(events, RunawayHook.EVENT_SOFT_THRESHOLD);
        int hardIdx = indexOf(events, RunawayHook.EVENT_HARD_STOP);
        assertThat(softIdx).isLessThan(hardIdx);
    }

    private static int indexOf(List<SessionEvent> events, String type) {
        for (int i = 0; i < events.size(); i++) {
            if (type.equals(events.get(i).type())) {
                return i;
            }
        }
        throw new AssertionError("事件 " + type + " 未出现：" + events);
    }

    // ---- Issue 04：单轮 wall-clock 超时（步边界）----

    @Test
    void wallClockTimeoutFiresAtStepBoundary() {
        ScriptedChatModel model = new ScriptedChatModel();
        // 第 1 步发起慢工具（工具内部 sleep，测试线程不 sleep）；第 2 步 beforeModel 发现超时
        model.enqueue(toolCall("tc-1", "slow_op"));
        model.enqueueText("收尾");

        BuzhouStores stores = Buzhou.inMemoryStores();
        // wall-clock=50ms；慢工具 sleep 100ms → 第 2 步步边界必然超时
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                new BuzhouRunawayProperties.PerTurn(null, null, java.time.Duration.ofMillis(50)),
                null, null, null, null, null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                slowTool("slow_op", 100));
        AgentSession session = rt.spawn("app", "agent", "sess-wc");
        List<SessionEvent> events = listen(session);

        String reply = session.chat("跑慢任务");
        session.close();

        // wall-clock 硬顶触发（reason=wall-clock），步边界生效
        SessionEvent hardStop = events.stream()
                .filter(e -> RunawayHook.EVENT_HARD_STOP.equals(e.type())).findFirst().orElseThrow();
        assertThat(hardStop.payload())
                .containsEntry("reason", RunawayHook.REASON_WALL_CLOCK)
                .containsEntry("limit", 50L);
        assertThat((long) hardStop.payload().get("value")).isGreaterThanOrEqualTo(100L);
        // 终止原因回复含 wall-clock 维度
        assertThat(reply).contains("wall-clock").contains("50");
    }

    /** 慢工具：call 内部 sleep 指定毫秒（模拟高延迟工具；测试线程不 sleep）。 */
    private static ToolCallback slowTool(String name, int sleepMillis) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\"}").build();
            }

            @Override
            public String call(String toolInput) {
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "slow-done";
            }
        };
    }

    // ---- Issue 03：轮次级工具调用硬顶 + 按工具单独限额 ----

    @Test
    void perTurnToolCallHardcapBlocksExcess() {
        ScriptedChatModel model = new ScriptedChatModel();
        // 4 次工具调用请求；max-tool-calls=3 → 第 4 次 beforeTool Block
        model.enqueue(toolCall("tc-1", "search"));
        model.enqueue(toolCall("tc-2", "search"));
        model.enqueue(toolCall("tc-3", "search"));
        model.enqueue(toolCall("tc-4", "search"));
        model.enqueueText("收尾");

        BuzhouStores stores = Buzhou.inMemoryStores();
        AtomicInteger toolCalls = new AtomicInteger();
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                new BuzhouRunawayProperties.PerTurn(null, 3, null),
                null, null, null, null, null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", toolCalls, "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-tc");
        List<SessionEvent> events = listen(session);

        String reply = session.chat("搜索");
        session.close();

        // 工具实际执行 3 次（第 4 次 beforeTool Block 未执行）
        assertThat(toolCalls.get()).isEqualTo(3);
        // 硬顶事件 reason=tool-calls
        SessionEvent hardStop = events.stream()
                .filter(e -> RunawayHook.EVENT_HARD_STOP.equals(e.type())).findFirst().orElseThrow();
        assertThat(hardStop.payload())
                .containsEntry("reason", RunawayHook.REASON_TOOL_CALLS)
                .containsEntry("limit", 3)
                .containsEntry("value", 4);
        // 模型看到 Block 文本后收尾
        assertThat(reply).isEqualTo("收尾");
    }

    @Test
    void perToolLimitBlocksExcessViaGlobWildcard() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "expensive_search"));
        model.enqueue(toolCall("tc-2", "expensive_search"));
        model.enqueue(toolCall("tc-3", "expensive_search"));
        model.enqueueText("收尾");

        BuzhouStores stores = Buzhou.inMemoryStores();
        AtomicInteger expensiveCalls = new AtomicInteger();
        // per-tool.expensive_*.max-calls=2（通配匹配）
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                new BuzhouRunawayProperties.PerTurn(null, null, null),
                null,
                java.util.Map.of("expensive_*", new BuzhouRunawayProperties.PerToolLimit(2)),
                null, null, null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("expensive_search", expensiveCalls, "result"));
        AgentSession session = rt.spawn("app", "agent", "sess-per-tool");
        List<SessionEvent> events = listen(session);

        String reply = session.chat("搜索");
        session.close();

        // 昂贵工具执行 2 次（第 3 次被按工具限额 Block）
        assertThat(expensiveCalls.get()).isEqualTo(2);
        // per-tool-exceeded 事件
        SessionEvent exceeded = events.stream()
                .filter(e -> RunawayHook.EVENT_PER_TOOL_EXCEEDED.equals(e.type())).findFirst().orElseThrow();
        assertThat(exceeded.payload())
                .containsEntry("toolName", "expensive_search")
                .containsEntry("limit", 2)
                .containsEntry("value", 3);
        assertThat(reply).isEqualTo("收尾");
    }

    @Test
    void perToolLimitExactMatchTakesPrecedence() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "search"));
        model.enqueue(toolCall("tc-2", "search"));
        model.enqueueText("收尾");

        BuzhouStores stores = Buzhou.inMemoryStores();
        AtomicInteger calls = new AtomicInteger();
        // exact "search".max-calls=1 优先于通配 "s*".max-calls=99
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                new BuzhouRunawayProperties.PerTurn(null, null, null),
                null,
                java.util.Map.of(
                        "search", new BuzhouRunawayProperties.PerToolLimit(1),
                        "s*", new BuzhouRunawayProperties.PerToolLimit(99)),
                null, null, null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", calls, "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-exact");
        List<SessionEvent> events = listen(session);

        session.chat("搜索");
        session.close();

        // exact 限额=1 生效（而非通配的 99）：第 2 次被 Block
        assertThat(calls.get()).isEqualTo(1);
        assertThat(events).anyMatch(e -> RunawayHook.EVENT_PER_TOOL_EXCEEDED.equals(e.type())
                && "search".equals(e.payload().get("toolName"))
                && Integer.valueOf(1).equals(e.payload().get("limit")));
    }

    // ---- Issue 05：会话级累计双窗口（跨崩溃持久化）----

    @Test
    void sessionStepHardcapWithPreSeededCounter() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "search"));
        model.enqueueText("收尾");

        BuzhouStores stores = Buzhou.inMemoryStores();
        // 预置会话级步数计数接近上限（照搬 crashloopHardCapStopsRepeatedAutoResume 形态）
        stores.sessionStateStore().put("sess-cum", new StateEntry(
                RunawayHook.KEY_SESSION_STEPS, "4", "runaway", 0, null, Instant.now()));

        // per-session.max-steps=5 → 预置 4，第 1 步到 5（proceed），第 2 步到 6 > 5（Block）
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                null, new BuzhouRunawayProperties.PerSession(5, null),
                null, null, null, null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", new AtomicInteger(), "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-cum");
        List<SessionEvent> events = listen(session);

        String reply = session.chat("搜索");
        session.close();

        // 仅 1 次模型调用（第 2 次会话级步数 6 > 5 被掐）
        assertThat(model.seenPrompts).hasSize(1);
        SessionEvent hardStop = events.stream()
                .filter(e -> RunawayHook.EVENT_HARD_STOP.equals(e.type())).findFirst().orElseThrow();
        assertThat(hardStop.payload())
                .containsEntry("reason", RunawayHook.REASON_SESSION_STEPS)
                .containsEntry("limit", 5)
                .containsEntry("value", 6);
        assertThat(reply).contains("会话累计步数");
    }

    @Test
    void sessionCounterSurvivesAcrossRespawn() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                null, new BuzhouRunawayProperties.PerSession(5, null),
                null, null, null, null);

        // 第一个 session：4 轮（每轮 1 步纯文本回复）→ 会话累计步数 4
        ScriptedChatModel model1 = new ScriptedChatModel();
        model1.enqueueText("r1");
        model1.enqueueText("r2");
        model1.enqueueText("r3");
        model1.enqueueText("r4");
        AgentRuntime rt1 = runtime(model1, stores, props, new RunawayCounters());
        AgentSession first = rt1.spawn("app", "agent", "sess-crash");
        first.chat("t1");
        first.chat("t2");
        first.chat("t3");
        first.chat("t4");
        first.close();

        // 模拟「崩溃-恢复」：同一 stores + 同一 sid 重 spawn（AUTO_RESUME 语义）
        ScriptedChatModel model2 = new ScriptedChatModel();
        model2.enqueueText("r5"); // 第 5 步（proceed，5<=5）
        AgentRuntime rt2 = runtime(model2, stores, props, new RunawayCounters());
        AgentSession resumed = rt2.spawn("app", "agent", "sess-crash");
        List<SessionEvent> events = listen(resumed);

        // 计数未重置：累计已 4，第 5 步 proceed
        assertThat(resumed.chat("t5")).isEqualTo("r5");
        // 第 6 步 → 累计 6 > 5 被掐（若重置则此处仍 proceed）
        String blocked = resumed.chat("t6");
        assertThat(blocked).contains("会话累计步数");
        assertThat(events).anyMatch(e -> RunawayHook.EVENT_HARD_STOP.equals(e.type())
                && RunawayHook.REASON_SESSION_STEPS.equals(e.payload().get("reason")));
        resumed.close();
    }

    @Test
    void sessionToolCallHardcapWithPreSeededCounter() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "search"));
        model.enqueue(toolCall("tc-2", "search"));
        model.enqueueText("收尾");

        BuzhouStores stores = Buzhou.inMemoryStores();
        // 预置会话级工具调用计数 = 1，max=2 → 第 1 次工具到 2（proceed），第 2 次到 3 > 2（Block）
        stores.sessionStateStore().put("sess-tc-cum", new StateEntry(
                RunawayHook.KEY_SESSION_TOOL_CALLS, "1", "runaway", 0, null, Instant.now()));

        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                null, new BuzhouRunawayProperties.PerSession(null, 2),
                null, null, null, null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", new AtomicInteger(), "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-tc-cum");
        List<SessionEvent> events = listen(session);

        session.chat("搜索");
        session.close();

        assertThat(events).anyMatch(e -> RunawayHook.EVENT_HARD_STOP.equals(e.type())
                && RunawayHook.REASON_SESSION_TOOL_CALLS.equals(e.payload().get("reason"))
                && Integer.valueOf(2).equals(e.payload().get("limit"))
                && Integer.valueOf(3).equals(e.payload().get("value")));
    }

    // ---- Issue 06：确定性重复检测（M2）----

    @Test
    void repetitionDetectionBlocksConsecutiveSameArgs() {
        ScriptedChatModel model = new ScriptedChatModel();
        // 连续 3 次同工具同参数（参数 {} 相同）→ 第 3 次 beforeTool 命中重复检测
        model.enqueue(toolCall("tc-1", "search"));
        model.enqueue(toolCall("tc-2", "search"));
        model.enqueue(toolCall("tc-3", "search"));
        model.enqueueText("收尾");

        BuzhouStores stores = Buzhou.inMemoryStores();
        AtomicInteger calls = new AtomicInteger();
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                null, null, null, null,
                new BuzhouRunawayProperties.Repetition(3, "block"), null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", calls, "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-rep");
        List<SessionEvent> events = listen(session);

        String reply = session.chat("搜索");
        session.close();

        // 工具执行 2 次（第 3 次同指纹被重复检测 Block）
        assertThat(calls.get()).isEqualTo(2);
        SessionEvent repetition = events.stream()
                .filter(e -> RunawayHook.EVENT_REPETITION.equals(e.type())).findFirst().orElseThrow();
        assertThat(repetition.payload())
                .containsEntry("toolName", "search")
                .containsEntry("count", 3);
        // beforeTool Block 的 reason 回注为工具结果，模型看到后收尾
        assertThat(reply).isEqualTo("收尾");
    }

    @Test
    void repetitionDetectionDoesNotFlagVariableArgs() {
        ScriptedChatModel model = new ScriptedChatModel();
        // 合法的分页翻读：每次参数不同（模拟分页 offset 变化）——确定性同参数规则不误杀
        model.enqueue(toolCallWithArgs("tc-1", "search", "{\"page\":1}"));
        model.enqueue(toolCallWithArgs("tc-2", "search", "{\"page\":2}"));
        model.enqueue(toolCallWithArgs("tc-3", "search", "{\"page\":3}"));
        model.enqueueText("分页完成");

        BuzhouStores stores = Buzhou.inMemoryStores();
        AtomicInteger calls = new AtomicInteger();
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                null, null, null, null,
                new BuzhouRunawayProperties.Repetition(3, "block"), null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", calls, "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-page");
        List<SessionEvent> events = listen(session);

        String reply = session.chat("分页搜索");
        session.close();

        // 合法变参循环不误杀：3 次工具调用全部执行
        assertThat(calls.get()).isEqualTo(3);
        assertThat(reply).isEqualTo("分页完成");
        assertThat(events).noneMatch(e -> RunawayHook.EVENT_REPETITION.equals(e.type()));
    }

    @Test
    void repetitionFlagOnlyEmitsEventWithoutBlocking() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "search"));
        model.enqueue(toolCall("tc-2", "search"));
        model.enqueue(toolCall("tc-3", "search"));
        model.enqueue(toolCall("tc-4", "search"));
        model.enqueueText("收尾");

        BuzhouStores stores = Buzhou.inMemoryStores();
        AtomicInteger calls = new AtomicInteger();
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                null, null, null, null,
                new BuzhouRunawayProperties.Repetition(3, "flag-only"), null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", calls, "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-flag");
        List<SessionEvent> events = listen(session);

        session.chat("搜索");
        session.close();

        // flag-only：发事件但不阻断——4 次工具调用全部执行
        assertThat(calls.get()).isEqualTo(4);
        assertThat(events).anyMatch(e -> RunawayHook.EVENT_REPETITION.equals(e.type()));
    }

    @Test
    void repetitionDisabledByDefault() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "search"));
        model.enqueue(toolCall("tc-2", "search"));
        model.enqueue(toolCall("tc-3", "search"));
        model.enqueueText("完成");

        BuzhouStores stores = Buzhou.inMemoryStores();
        // repetition=null（默认关）
        BuzhouRunawayProperties props = BuzhouRunawayProperties.defaults();
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", new AtomicInteger(), "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-rep-off");
        List<SessionEvent> events = listen(session);

        session.chat("搜索");
        session.close();

        // 默认关：连续同参数不触发
        assertThat(events).noneMatch(e -> RunawayHook.EVENT_REPETITION.equals(e.type()));
    }

    /** 脚本化「助手发起一次带参数的工具调用」。 */
    private static AssistantMessage toolCallWithArgs(String id, String name, String arguments) {
        return AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, arguments)))
                .build();
    }

    // ---- Issue 07：事件落 ObservabilityStore + 流式对等 ----

    @Test
    void runawayEventsReachObservabilityStore() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "search"));
        model.enqueue(toolCall("tc-2", "search"));
        model.enqueue(toolCall("tc-3", "search"));
        model.enqueue(toolCall("tc-4", "search"));
        model.enqueueText("完成");

        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                new BuzhouRunawayProperties.PerTurn(3, null, null),
                null, null, null, null, null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", new AtomicInteger(), "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-obs");
        session.chat("搜索");
        session.close();

        // runaway.* 全族事件在 ObservabilityStore（eventsOfSession）可查
        List<io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord> records =
                stores.observabilityStore().eventsOfSession("sess-obs");
        assertThat(records).anyMatch(e -> RunawayHook.EVENT_HARD_STOP.equals(e.type())
                && "sess-obs".equals(e.sessionId()));
    }

    @Test
    void streamingParityStepHardcap() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueue(toolCall("tc-1", "search"));
        model.enqueue(toolCall("tc-2", "search"));
        model.enqueue(toolCall("tc-3", "search"));
        model.enqueueText("流式完成");

        BuzhouStores stores = Buzhou.inMemoryStores();
        BuzhouRunawayProperties props = new BuzhouRunawayProperties(true,
                new BuzhouRunawayProperties.PerTurn(2, null, null),
                null, null, null, null, null);
        AgentRuntime rt = runtime(model, stores, props, new RunawayCounters(),
                countingTool("search", new AtomicInteger(), "ok"));
        AgentSession session = rt.spawn("app", "agent", "sess-stream");
        List<SessionEvent> events = listen(session);

        // 流式调用下步数硬顶同样生效（对齐 drainWaitsForInFlightStreamTurn 形态）
        reactor.core.publisher.Flux<org.springframework.ai.chat.model.ChatResponse> flux = session.stream("搜索");
        flux.blockLast();
        session.close();

        // 流式下步数硬顶同样触发：仅 2 次模型调用（第 3 步被掐）
        assertThat(model.seenPrompts).hasSize(2);
        assertThat(events).anyMatch(e -> RunawayHook.EVENT_HARD_STOP.equals(e.type())
                && RunawayHook.REASON_STEPS.equals(e.payload().get("reason")));
    }
}
