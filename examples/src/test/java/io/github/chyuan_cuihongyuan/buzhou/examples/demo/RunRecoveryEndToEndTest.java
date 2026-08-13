package io.github.chyuan_cuihongyuan.buzhou.examples.demo;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryRunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RecoverySupport;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRecoveryService;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStateSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAlreadyActiveException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.examples.support.FakeModelGuard;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-06+07 / T32+T33（docs/spec/12 §core-4/5）端到端：
 * Run 注册表以 Completed-Turn 为快照单元追踪；崩溃恢复时事件日志命中 COMPLETED 的
 * 悬空调用<b>按 id 回放、不重跑工具</b>（exactly-once）；restart 受 lease 门（拿不到即拒绝）。
 */
class RunRecoveryEndToEndTest {

    @Test
    void crashRecoveryReplaysCompletedToolFromEventLogWithoutReexecution() {
        InMemoryRunRegistry registry = new InMemoryRunRegistry();
        InMemoryToolCallLog log = new InMemoryToolCallLog();
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "crash-" + UUID.randomUUID();

        // 崩溃现场：turn 3 的 write_db 调用已派发、已执行完成（事件日志有 COMPLETED 结局），
        // 但 ToolResponse 未落库（dangling）
        String toolCallId = "tc-crash-1";
        List<BuzhouMessage> crashed = new ArrayList<>();
        crashed.add(msg(sessionId, 3, 0, Role.USER, "把配置写进数据库"));
        crashed.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 3, 1, Role.ASSISTANT,
                "", List.of(new ToolCallRecord(toolCallId, "write_db", "{}")),
                null, null, null, Map.of(), Instant.now()));
        stores.messageStore().append(sessionId, crashed);
        log.append(new ToolCallLogEntry(sessionId, toolCallId, "write_db",
                ToolCallLogEntry.argsHash("{}"), ToolCallOutcome.COMPLETED,
                "已写入行 42（事件日志回放）", null));

        // 恢复：重启后写型工具【绝不重复执行】，模型直接见到回放结果
        AtomicInteger executions = new AtomicInteger();
        FakeChatModel model = FakeChatModel.script(ScriptStep.text("恢复完成：写入已确认"));
        FakeModelGuard.requireTestDouble(model);
        ToolCallback writeDb = countingTool("write_db", "不应执行", executions);
        RuntimeConfig config = RecoverySupport.attach(RuntimeConfig.defaults(),
                registry, log, "recovery-app");
        AgentRuntime runtime = Buzhou.runtime(model, stores, config, writeDb);

        AgentSession session = runtime.spawn("recovery-app", "agent", sessionId);
        String reply = session.chat("确认一下刚才的写入");
        session.close();

        assertThat(reply).isEqualTo("恢复完成：写入已确认");
        // exactly-once：写型工具零重跑；模型看到的是事件日志回放结果而非「中断未知」
        assertThat(executions.get()).isZero();
        String prompt = model.seenPrompts.getFirst().getInstructions().toString();
        assertThat(prompt).contains("已写入行 42（事件日志回放）");
        assertThat(prompt).doesNotContain("执行被中断，结果未知");
        // run 快照推进 + 正常谢幕置 COMPLETED（turn 计数为 per-spawn 逻辑值：崩溃前无快照则从 1 起；
        // 跨 restart 时 completingTurn 取 Math.max 保留旧边界——内容真相以持久化历史为准）
        assertThat(registry.find(sessionId)).hasValueSatisfying(snap -> {
            assertThat(snap.status()).isEqualTo(RunStatus.COMPLETED);
            assertThat(snap.lastCompletedTurn()).isGreaterThanOrEqualTo(1);
        });
    }

    @Test
    void registryTracksTurnBoundariesDuringNormalRun() {
        InMemoryRunRegistry registry = new InMemoryRunRegistry();
        InMemoryToolCallLog log = new InMemoryToolCallLog();
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "track-" + UUID.randomUUID();

        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("ping", "{}"),
                ScriptStep.text("完成"));
        RuntimeConfig config = RecoverySupport.attach(RuntimeConfig.defaults(),
                registry, log, "track-app");
        AgentRuntime runtime = Buzhou.runtime(model, stores, config, countingTool("ping", "pong",
                new AtomicInteger()));
        AgentSession session = runtime.spawn("track-app", "agent", sessionId);
        session.chat("第一轮");
        // turn 完结后：RUNNING + lastCompletedTurn 推进 + 工具结局已入日志
        assertThat(registry.find(sessionId)).hasValueSatisfying(snap -> {
            assertThat(snap.status()).isEqualTo(RunStatus.RUNNING);
            assertThat(snap.lastCompletedTurn()).isEqualTo(1);
            assertThat(snap.currentTurn()).isEqualTo(1);
        });
        assertThat(log.find(sessionId, "tc-0-0")).hasValueSatisfying(entry ->
                assertThat(entry.outcome()).isEqualTo(ToolCallOutcome.COMPLETED));
        session.close();
        assertThat(registry.find(sessionId)).hasValueSatisfying(snap ->
                assertThat(snap.status()).isEqualTo(RunStatus.COMPLETED));
        assertThat(new RunRecoveryService(registry, runtime).runningRuns()).isEmpty();
    }

    @Test
    void restartRefusesWhenLeaseHeldByAnotherOwner() {
        InMemoryRunRegistry registry = new InMemoryRunRegistry();
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "lease-" + UUID.randomUUID();
        registry.save(new RunStateSnapshot(sessionId, "lease-app", "agent",
                RunStatus.RUNNING, 3, 2, "owner-a", null));

        // 实例 A 持有租约（会话存活）
        FakeChatModel modelA = FakeChatModel.script(ScriptStep.text("A"));
        AgentRuntime runtimeA = Buzhou.runtime(modelA, stores, RuntimeConfig.defaults());
        AgentSession heldByA = runtimeA.spawn("lease-app", "agent", sessionId);

        // 实例 B 的恢复服务：lease 门——拿不到租约即拒绝（不 steal）
        FakeChatModel modelB = FakeChatModel.script(ScriptStep.text("B"));
        AgentRuntime runtimeB = Buzhou.runtime(modelB, stores, RuntimeConfig.defaults());
        RunRecoveryService recovery = new RunRecoveryService(registry, runtimeB);
        assertThatThrownBy(() -> recovery.restart(sessionId, false))
                .isInstanceOf(SessionAlreadyActiveException.class);

        // A 释放后可恢复（steal=false 也成功）
        heldByA.close();
        assertThat(recovery.restart(sessionId, false)).isPresent();
    }

    private static BuzhouMessage msg(String sessionId, int turn, int seq, Role role, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, seq, role,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    private static ToolCallback countingTool(String name, String result, AtomicInteger executions) {
        List<String> sink = new CopyOnWriteArrayList<>();
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                executions.incrementAndGet();
                sink.add(name);
                return result;
            }
        };
    }
}
