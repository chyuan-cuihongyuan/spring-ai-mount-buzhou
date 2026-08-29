package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouTokenBudgetProperties;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 148 §B / T501：key 级预算闸 e2e——扣减随 usage 入账（跨会话共享同一 key
 * 额度）；越限观测事件即刻发（本响应已生成）；下一次调用被 beforeModel 拦截
 * （模型零调用，block 文案为最终回复）；reset 后恢复。借鉴：LiteLLM virtual-key
 * budget 超限即 429 的闸位语义（拦截下一次而非撕毁本次）。
 */
class VirtualKeyBudgetGateTest {

    static final class UsageChatModel extends ScriptedChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            ChatResponse base = super.call(prompt);
            return new ChatResponse(base.getResults(), ChatResponseMetadata.builder()
                    .usage(new DefaultUsage(100, 50))
                    .build());
        }
    }

    private static AgentRuntime runtime(ScriptedChatModel model, BuzhouStores stores,
                                        VirtualKeys keys, String key) {
        TokenBudgetHook hook = new TokenBudgetHook(BuzhouTokenBudgetProperties.defaults(),
                "test-model", stores.observabilityStore(), keys, key);
        RuntimeConfig config = new RuntimeConfig(List.of(hook), Set.of(), Set.of(), null, List.of());
        return Buzhou.runtime(model, stores, config);
    }

    private static List<SessionEvent> listen(AgentSession session) {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        return events;
    }

    @Test
    void keySpendAccumulatesThenBlocksNextCallUntilReset() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        BuzhouStores stores = Buzhou.inMemoryStores();
        VirtualKeys keys = VirtualKeys.create();
        keys.register("app-key", 200); // 每调用 total=150：第一次入账，第二次越限
        AgentRuntime runtime = runtime(model, stores, keys, "app-key");

        AgentSession session = runtime.spawn("app", "agent", "sess");
        List<SessionEvent> events = listen(session);
        session.chat("q1"); // 150 入账（150/200）
        session.chat("q2"); // trySpend 150 → 300 > 200 越限：观测事件即刻发，响应已生成
        assertThat(keys.usage("app-key").usedTokens()).isEqualTo(150L);
        assertThat(keys.isExhausted("app-key")).isTrue();

        List<SessionEvent> keyStops = events.stream()
                .filter(e -> TokenBudgetHook.EVENT_KEY_HARD_STOP.equals(e.type())).toList();
        assertThat(keyStops).hasSize(1);
        Map<String, Object> payload = keyStops.get(0).payload();
        assertThat(payload.get("limit")).isEqualTo(200L);
        assertThat(payload.get("value")).isEqualTo(150L);
        session.close();

        // 耗尽后：新会话的第一个调用即被拦（模型零调用，block 文案为最终回复）
        model.enqueueText("never-used");
        AgentSession blocked = runtime.spawn("app", "agent", "sess-2");
        List<SessionEvent> events2 = listen(blocked);
        blocked.chat("q3");
        assertThat(events2.stream()
                .filter(e -> TokenBudgetHook.EVENT_KEY_HARD_STOP.equals(e.type()))).hasSize(1);
        assertThat(keys.usage("app-key").usedTokens()).isEqualTo(150L); // 未再入账
        blocked.close();

        // 窗口 reset：耗尽态与用量同清，恢复可用
        keys.reset("app-key");
        assertThat(keys.isExhausted("app-key")).isFalse();
        AgentSession recovered = runtime.spawn("app", "agent", "sess-3");
        recovered.chat("q4");
        assertThat(keys.usage("app-key").usedTokens()).isEqualTo(150L); // 新窗口重新入账
        recovered.close();
    }

    @Test
    void noKeyConfiguredMeansZeroChange() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = runtime(model, stores, null, null); // 既有行为零变化
        AgentSession session = runtime.spawn("app", "agent", "sess");
        List<SessionEvent> events = listen(session);
        session.chat("q1");
        assertThat(events).noneMatch(e ->
                TokenBudgetHook.EVENT_KEY_HARD_STOP.equals(e.type()));
        session.close();
    }
}
