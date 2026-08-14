package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceStats;
import io.github.chyuan_cuihongyuan.buzhou.resilience.quota.SessionQuotaHook;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * per-session 日配额 e2e（spec 16 / T84 / impl-59）：turns/tokens 日窗超限 Block + 事件；
 * 限流器进程级共享（两会话合计触顶）；无配额零开销。
 */
class SessionQuotaEndToEndTest {

    /** 每调用附带 usage（60+40=100 tokens）的替身模型。 */
    static final class UsageChatModel extends ScriptedChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            ChatResponse base = super.call(prompt);
            return new ChatResponse(base.getResults(), ChatResponseMetadata.builder()
                    .usage(new DefaultUsage(60, 40))
                    .build());
        }
    }

    /** turns-per-day=2：前两轮正常，第三轮 beforeTurn 拦截（模型零调用），block 文本含配额说明。 */
    @Test
    void turnsPerDayBlocksThirdTurn() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        ResilienceProperties props = props(new ResilienceProperties.SessionQuota(2, null, null));

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        assertThat(session.chat("q1")).isEqualTo("r1");
        assertThat(session.chat("q2")).isEqualTo("r2");
        String blocked = session.chat("q3");

        assertThat(blocked).contains("配额上限");
        assertThat(model.seenPrompts).hasSize(2); // 第三轮未触达模型
        assertThat(events).anyMatch(e -> SessionQuotaHook.EVENT_QUOTA_EXCEEDED.equals(e.type())
                && "turns".equals(e.payload().get("dimension"))
                && Integer.valueOf(2).equals(e.payload().get("limit")));
        session.close();
    }

    /** tokens-per-day=250：每轮 100，q1/q2/q3 放行（累计 100/200/300），q4 前读 300 ≥ 250 拦截。 */
    @Test
    void tokensPerDayBlocksWhenDailyTotalReached() {
        UsageChatModel model = new UsageChatModel();
        for (int i = 0; i < 4; i++) {
            model.enqueueText("r" + (i + 1));
        }
        ResilienceProperties props = props(new ResilienceProperties.SessionQuota(null, null, 250L));

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);

        assertThat(session.chat("q1")).isEqualTo("r1"); // 累计 100
        assertThat(session.chat("q2")).isEqualTo("r2"); // 累计 200
        assertThat(session.chat("q3")).isEqualTo("r3"); // 读 200 < 250 放行 → 累计 300
        String blocked = session.chat("q4");            // 读 300 ≥ 250 → 拦截

        assertThat(blocked).contains("配额上限");
        assertThat(model.seenPrompts).hasSize(3);
        assertThat(events).anyMatch(e -> SessionQuotaHook.EVENT_QUOTA_EXCEEDED.equals(e.type())
                && "tokens".equals(e.payload().get("dimension")));
        session.close();
    }

    /** 配额彼此独立按会话计数：会话 A 触顶不影响会话 B（per-session 语义）。 */
    @Test
    void quotaIsPerSession() {
        UsageChatModel model = new UsageChatModel();
        model.enqueueText("a1");
        model.enqueueText("b1");
        ResilienceProperties props = props(new ResilienceProperties.SessionQuota(1, null, null));

        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                ResilienceModule.configure(props, "m", new ResilienceStats()));

        AgentSession a = runtime.spawn("app", "agent", "sess-a");
        assertThat(a.chat("q1")).isEqualTo("a1");
        assertThat(a.chat("q2")).contains("配额上限"); // A 触顶
        a.close();

        AgentSession b = runtime.spawn("app", "agent", "sess-b");
        assertThat(b.chat("q1")).isEqualTo("b1"); // B 独立计数，不受 A 影响
        b.close();
    }

    /** 限流器进程级共享（impl-59 修正）：rpm=1 下两会话合计第 2 次调用即被拒（旧行为是每会话各 1 次）。 */
    @Test
    void rateLimiterSharedAcrossSessionsOfSameRuntime() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        ResilienceProperties.RateLimit rl = new ResilienceProperties.RateLimit(
                1, null, Duration.ofMillis(50), "FAIL_FAST");
        ResilienceProperties props = new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, Duration.ofSeconds(5),
                rl, null, null, null);

        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                ResilienceModule.configure(props, "m", new ResilienceStats()));

        AgentSession a = runtime.spawn("app", "agent", "sess-a");
        assertThat(a.chat("q1")).isEqualTo("r1"); // 占掉进程级 RPM=1
        a.close();

        AgentSession b = runtime.spawn("app", "agent", "sess-b");
        assertThatThrownBy(() -> b.chat("q2"))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.resilience.ratelimit
                        .ModelRateLimitExceededException.class);
        assertThat(model.seenPrompts).hasSize(1); // 第二次调用未触达模型
        b.close();
    }

    /** 无配额配置零开销：不挂 Hook、行为与现状一致。 */
    @Test
    void noQuotaNoOverhead() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("r1");
        model.enqueueText("r2");
        ResilienceProperties props = props(null);

        AgentSession session = newRuntime(model, props);
        List<SessionEvent> events = listen(session);
        assertThat(session.chat("q1")).isEqualTo("r1");
        assertThat(session.chat("q2")).isEqualTo("r2");
        assertThat(events).noneMatch(e -> e.type().startsWith("quota."));
        session.close();
    }

    // ---- helpers ----

    private static ResilienceProperties props(ResilienceProperties.SessionQuota quota) {
        return new ResilienceProperties(true, 1, Duration.ofMillis(1), Duration.ofMillis(10),
                2.0, 0.0, null, Duration.ofSeconds(5), null, null, null, quota);
    }

    private static AgentSession newRuntime(ScriptedChatModel model, ResilienceProperties props) {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                ResilienceModule.configure(props, "m", new ResilienceStats()));
        return runtime.spawn("app", "agent", "sess-" + System.nanoTime());
    }

    private static List<SessionEvent> listen(AgentSession session) {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        session.addEventListener(events::add);
        return events;
    }
}
