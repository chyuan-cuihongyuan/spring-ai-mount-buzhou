package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-09 / T34 time-travel fork：Completed-Turn 检查点枚举（指纹）+ 从检查点分叉
 * （新 sessionId 独立演进、原会话隔离不动）。
 */
class SessionForksTest {

    @Test
    void checkpointsEnumeratedWithFingerprintAndForkIsolates() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sessionId = "fork-" + UUID.randomUUID();
        stores.messageStore().append(sessionId, fourTurnHistory(sessionId));

        SessionForks forks = new SessionForks(stores.messageStore());
        var checkpoints = forks.listCheckpoints(sessionId);
        // 存档中每轮均已完结（有工具响应 + 收尾文本）→ 全部是可分叉检查点
        assertThat(checkpoints.stream().map(SessionForks.TurnCheckpoint::turnSeq))
                .containsExactly(1, 2, 3, 4);
        checkpoints.forEach(cp -> {
            assertThat(cp.fingerprint()).hasSize(64);
            assertThat(cp.messageCount()).isPositive();
        });
        // 检查点单调递增（消息数随 Turn 累积）
        assertThat(checkpoints.stream().map(SessionForks.TurnCheckpoint::messageCount).toList())
                .isSorted();

        // 从 turn 2 分叉：新会话只含 ≤2 的消息；原会话不动
        String forkId = forks.forkFrom(sessionId, 2);
        assertThat(stores.messageStore().load(forkId))
                .allSatisfy(m -> assertThat(m.turnSeq()).isLessThanOrEqualTo(2));
        assertThat(stores.messageStore().load(sessionId)).hasSize(16); // 原会话完整保留

        // 分叉会话独立演进（chat 正常；模型见到分叉历史）
        List<String> prompts = new CopyOnWriteArrayList<>();
        org.springframework.ai.chat.model.ChatModel model =
                new org.springframework.ai.chat.model.ChatModel() {
                    @Override
                    public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
                        return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
                    }

                    @Override
                    public org.springframework.ai.chat.model.ChatResponse call(
                            org.springframework.ai.chat.prompt.Prompt prompt) {
                        prompts.add(prompt.getInstructions().toString());
                        return new org.springframework.ai.chat.model.ChatResponse(java.util.List.of(
                                new org.springframework.ai.chat.model.Generation(
                                        new org.springframework.ai.chat.messages.AssistantMessage("分叉回复"))));
                    }
                };
        AgentRuntime runtime = Buzhou.runtime(model, stores);
        AgentSession forkSession = runtime.spawn("fork-app", "agent", forkId);
        forkSession.chat("分叉后第一问");
        forkSession.close();
        assertThat(prompts.getFirst()).contains("第 2 步").doesNotContain("第 4 步");
    }

    /** 4 轮历史（每轮 user/assistant/toolcall/toolresult/assistant 收尾 → 完结）。 */
    private static List<BuzhouMessage> fourTurnHistory(String sessionId) {
        List<BuzhouMessage> history = new ArrayList<>();
        for (int turn = 1; turn <= 4; turn++) {
            history.add(msg(sessionId, turn, 0, Role.USER, "第 " + turn + " 步"));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 1,
                    Role.ASSISTANT, "", List.of(new ToolCallRecord("tc-" + turn, "q", "{}")),
                    null, null, null, Map.of(), Instant.now()));
            history.add(new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 2,
                    Role.TOOL, "结果 " + turn, List.of(), "tc-" + turn, null, null,
                    Map.of("toolName", "q"), Instant.now()));
            history.add(msg(sessionId, turn, 3, Role.ASSISTANT, "完成 " + turn));
        }
        return history;
    }

    private static BuzhouMessage msg(String sessionId, int turn, int seq, Role role, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, seq, role,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }
}
