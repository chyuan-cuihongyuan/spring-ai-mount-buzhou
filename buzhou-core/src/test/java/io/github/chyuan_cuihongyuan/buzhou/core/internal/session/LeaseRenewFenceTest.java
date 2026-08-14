package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryMessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionLeaseStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryUnitOfWork;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.session.LeaseLostException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionObserver;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseAcquireResult;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaseInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionLeaseStore;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.FakeChatModel;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptStep;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.TestDoubleChatModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * impl-33 / spec 13 §core-3（租约续租 + LeaseLost + 写路径 fence）core 级测试：
 *
 * <ul>
 *   <li>续租双路径节奏——Turn 轮间（工具批跨过 TTL/3 时在下一轮开始续租；后台间隔拉长隔离）与
 *       后台 TTL/3（空闲会话也被续租；close 注销）；</li>
 *   <li>LeaseLost 中止语义——轮间/后台发现被 steal → Turn 中止、在飞工具结果不入 history、
 *       Turn 不入 Completed-Turn（onTurnEnd 不发生、onTurnError 收口）、后续 chat 明确拒绝；</li>
 *   <li>fence——fencingToken 不匹配时不落库（终局消息写入点也被 memory advisor 写 fence 拦截）。</li>
 * </ul>
 *
 * <p>计时断言沿用契约测试的真实时序风格（短 TTL + 宽裕间隔），两侧都留 ≥500ms 余量防 flake。
 */
class LeaseRenewFenceTest {

    /** 记录型租约 store：委托内存实现并计数续租（节奏断言）。 */
    static final class RecordingLeaseStore implements SessionLeaseStore {
        final SessionLeaseStore delegate = new InMemorySessionLeaseStore();
        final AtomicInteger renewCalls = new AtomicInteger();

        @Override
        public LeaseAcquireResult tryAcquire(String sessionId, String ownerId, Duration ttl) {
            return delegate.tryAcquire(sessionId, ownerId, ttl);
        }

        @Override
        public boolean renew(String sessionId, String ownerId, long fencingToken, Duration ttl) {
            renewCalls.incrementAndGet();
            return delegate.renew(sessionId, ownerId, fencingToken, ttl);
        }

        @Override
        public void release(String sessionId, String ownerId, long fencingToken) {
            delegate.release(sessionId, ownerId, fencingToken);
        }

        @Override
        public LeaseAcquireResult steal(String sessionId, String newOwnerId, Duration ttl) {
            return delegate.steal(sessionId, newOwnerId, ttl);
        }

        @Override
        public Optional<LeaseInfo> inspect(String sessionId) {
            return delegate.inspect(sessionId);
        }
    }

    /** 轮次收尾观察者：记录 onTurnEnd / onTurnError（Completed-Turn 语义断言）。 */
    static final class TurnLifecycleRecorder implements SessionObserver {
        final List<String> timeline = new CopyOnWriteArrayList<>();

        @Override
        public void onTurnEnd(int turnSeq, String finalReply) {
            timeline.add("end:" + turnSeq);
        }

        @Override
        public void onTurnError(int turnSeq, Throwable error) {
            timeline.add("error:" + turnSeq + ":" + error.getClass().getSimpleName());
        }
    }

    private static BuzhouStores storesWith(RecordingLeaseStore leaseStore) {
        return new BuzhouStores(new InMemoryMessageStore(), new InMemorySummaryStore(),
                new InMemorySessionStateStore(), leaseStore,
                new InMemoryObservabilityStore(), new InMemoryUnitOfWork());
    }

    private static ToolCallback fixedTool(String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                return name + "-result";
            }
        };
    }

    private static ToolCallback stealingTool(String name, SessionLeaseStore store, String sessionId) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                store.steal(sessionId, "external-thief", Duration.ofSeconds(90));
                return name + "-result";
            }
        };
    }

    private static ToolCallback sleepingTool(String name, long millis) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description(name)
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}").build();
            }

            @Override
            public String call(String toolInput) {
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return name + "-result";
            }
        };
    }

    // ---- 续租双路径 ----

    @Test
    void shouldRenewLeaseBetweenRounds_whenToolBatchOutlastsRenewThreshold() {
        // 后台间隔 60s（隔离后台路径）：唯一的续租者是 Turn 轮间检查
        // TTL 5s / 阈值 5/3≈1.67s：工具批睡 4s → 第 2 轮开始时剩余 ≤1s < 阈值 → 续租；
        // 第 1 轮开始时剩余 ≥4s > 阈值 → 不续（两侧各留 ≥500ms 余量）
        RecordingLeaseStore leaseStore = new RecordingLeaseStore();
        BuzhouStores stores = storesWith(leaseStore);
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("slow_tool", "{}"),
                ScriptStep.text("慢工具后正常收尾"));
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(),
                Duration.ofSeconds(5), Duration.ofSeconds(60), sleepingTool("slow_tool", 4_000));

        AgentSession session = runtime.spawn("lease-app", "agent", "inter-round");
        String reply = session.chat("开始");
        session.close();

        assertThat(reply).isEqualTo("慢工具后正常收尾"); // 续租对调用方透明
        assertThat(leaseStore.renewCalls.get()).isEqualTo(1); // 恰在轮间续租一次
    }

    @Test
    void shouldRenewLeaseInBackgroundAtConfiguredInterval_whileSessionActive() {
        // TTL 1.5s / 后台间隔 300ms：空闲会话 1s 内至少续租 2 次（剩余租期恒被推回 ~TTL）
        RecordingLeaseStore leaseStore = new RecordingLeaseStore();
        BuzhouStores stores = storesWith(leaseStore);
        FakeChatModel model = FakeChatModel.script(ScriptStep.text("ok"));
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(),
                Duration.ofMillis(1_500), Duration.ofMillis(300));

        AgentSession session = runtime.spawn("lease-app", "agent", "bg-renew");
        sleepQuietly(1_000);
        int renewedWhileActive = leaseStore.renewCalls.get();
        Optional<LeaseInfo> leaseBeforeClose = leaseStore.inspect("bg-renew"); // close 前取证
        session.close();

        assertThat(renewedWhileActive).isGreaterThanOrEqualTo(2);
        assertThat(leaseBeforeClose)
                .as("后台续租应持续推远过期时刻（租约未静默过期）")
                .isPresent();
    }

    @Test
    void shouldStopBackgroundRenewal_whenSessionClosed() {
        // close 注销续租：关会后即使 TTL 未到也不再续租（防泄漏/防复活）
        RecordingLeaseStore leaseStore = new RecordingLeaseStore();
        BuzhouStores stores = storesWith(leaseStore);
        FakeChatModel model = FakeChatModel.script(ScriptStep.text("ok"));
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(),
                Duration.ofSeconds(30), Duration.ofMillis(150));

        AgentSession session = runtime.spawn("lease-app", "agent", "bg-close");
        sleepQuietly(400); // 让后台先续租至少一次
        int renewedBeforeClose = leaseStore.renewCalls.get();
        session.close();
        sleepQuietly(600); // ≥ 3 个续租周期
        assertThat(renewedBeforeClose).isGreaterThanOrEqualTo(1); // 前置条件：close 前确有续租
        assertThat(leaseStore.renewCalls.get()).isEqualTo(renewedBeforeClose); // close 后冻结
    }

    // ---- LeaseLost 中止语义 ----

    @Test
    void shouldAbortTurnDropInFlightResultsAndStayRejected_whenLeaseStolenMidTurn() {
        RecordingLeaseStore leaseStore = new RecordingLeaseStore();
        BuzhouStores stores = storesWith(leaseStore);
        FakeChatModel model = FakeChatModel.script(
                ScriptStep.toolCall("sneaky_tool", "{}"),
                ScriptStep.text("不应出现的终局"));
        TurnLifecycleRecorder recorder = new TurnLifecycleRecorder();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                RuntimeConfig.assemblyCustomizers(java.util.List.of(ctx -> ctx.addObserver(recorder))),
                stealingTool("sneaky_tool", leaseStore, "mid-turn-steal"));

        AgentSession session = runtime.spawn("lease-app", "agent", "mid-turn-steal");
        List<SessionEvent> events = new ArrayList<>();
        session.addEventListener(events::add);

        Throwable thrown = catchThrowable(() -> session.chat("开始"));

        // LeaseLost 上抛（结构化错误码，NON_RETRYABLE）
        assertThat(thrown).isInstanceOf(LeaseLostException.class);
        assertThat(((LeaseLostException) thrown).errorCode()).isEqualTo(ErrorCode.LEASE_LOST);
        // Turn 不入 Completed-Turn：onTurnEnd 不发生、onTurnError 收口；后续调用明确拒绝
        assertThat(recorder.timeline).containsExactly("error:1:LeaseLostException");
        assertThat(catchThrowable(() -> session.chat("再来一次"))).isInstanceOf(LeaseLostException.class);
        assertThat(model.callCount()).isEqualTo(1); // 第 2 轮模型调用从未发生
        // 在飞工具结果丢弃：消息台账无 TOOL 结果、无终局 assistant 文本
        List<BuzhouMessage> history = stores.messageStore().load("mid-turn-steal");
        assertThat(history.stream().filter(m -> m.role() == Role.TOOL)).isEmpty();
        assertThat(history).extracting(BuzhouMessage::content)
                .noneMatch(c -> c != null && c.contains("不应出现的终局"));
        // 可观测：session.lease.lost 事件
        assertThat(events).anyMatch(e -> e.type().equals("session.lease.lost"));
        session.close();
    }

    @Test
    void shouldRejectChatWithZeroLedgerWrites_whenLeaseStolenBeforeTurn() {
        RecordingLeaseStore leaseStore = new RecordingLeaseStore();
        BuzhouStores stores = storesWith(leaseStore);
        FakeChatModel model = FakeChatModel.script(ScriptStep.text("不应到达"));
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults());

        AgentSession session = runtime.spawn("lease-app", "agent", "pre-turn-steal");
        leaseStore.steal("pre-turn-steal", "external-thief", Duration.ofSeconds(90));

        Throwable thrown = catchThrowable(() -> session.chat("你好"));

        assertThat(thrown).isInstanceOf(LeaseLostException.class);
        assertThat(model.callCount()).isZero(); // 模型调用从未发生
        assertThat(stores.messageStore().load("pre-turn-steal"))
                .as("Turn 入口 fence 拦截：消息台账零写入")
                .isEmpty();
        session.close();
    }

    @Test
    void shouldRejectNextChat_whenBackgroundRenewDetectsExternalSteal() {
        RecordingLeaseStore leaseStore = new RecordingLeaseStore();
        BuzhouStores stores = storesWith(leaseStore);
        FakeChatModel model = FakeChatModel.script(ScriptStep.text("ok"));
        AgentRuntime runtime = Buzhou.runtime(model, stores, RuntimeConfig.defaults(),
                Duration.ofSeconds(30), Duration.ofMillis(150));

        AgentSession session = runtime.spawn("lease-app", "agent", "bg-steal");
        leaseStore.steal("bg-steal", "external-thief", Duration.ofSeconds(90));
        sleepQuietly(500); // ≥ 2 个后台续租周期：第一个 tick 即发现并标记丢失

        assertThat(catchThrowable(() -> session.chat("继续")))
                .isInstanceOf(LeaseLostException.class)
                .hasMessageContaining("bg-steal");
        assertThat(model.callCount()).isZero();
        session.close();
    }

    // ---- 写路径 fence（fencingToken 不匹配不落库） ----

    @Test
    void shouldBlockLedgerWriteAtCommitPoint_whenFencingTokenChangesDuringFinalModelCall() {
        RecordingLeaseStore leaseStore = new RecordingLeaseStore();
        BuzhouStores stores = storesWith(leaseStore);
        StealOnSecondCallModel model = new StealOnSecondCallModel("final-turn", leaseStore);
        TurnLifecycleRecorder recorder = new TurnLifecycleRecorder();
        AgentRuntime runtime = Buzhou.runtime(model, stores,
                RuntimeConfig.assemblyCustomizers(java.util.List.of(ctx -> ctx.addObserver(recorder))),
                fixedTool("fence_probe"));

        AgentSession session = runtime.spawn("lease-app", "agent", "final-turn");
        Throwable thrown = catchThrowable(() -> session.chat("开始"));

        // 终局消息产出瞬间租约被抢走：memory advisor 写 fence 拦截 → 终局 assistant 不落库
        assertThat(thrown).isInstanceOf(LeaseLostException.class);
        assertThat(recorder.timeline).containsExactly("error:1:LeaseLostException");
        List<BuzhouMessage> history = stores.messageStore().load("final-turn");
        assertThat(history).extracting(BuzhouMessage::content)
                .noneMatch(c -> c != null && c.contains("final-answer"));
        assertThat(catchThrowable(() -> session.chat("再来"))).isInstanceOf(LeaseLostException.class);
        session.close();
    }

    /** 第 1 次调用要工具；第 2 次（终局）调用先从外部 steal 租约再返回终局文本。 */
    static final class StealOnSecondCallModel implements TestDoubleChatModel {
        private final String sessionId;
        private final SessionLeaseStore leaseStore;
        private final AtomicInteger calls = new AtomicInteger();

        StealOnSecondCallModel(String sessionId, SessionLeaseStore leaseStore) {
            this.sessionId = sessionId;
            this.leaseStore = leaseStore;
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            if (calls.incrementAndGet() == 1) {
                AssistantMessage toolCallMsg = AssistantMessage.builder().content("")
                        .toolCalls(List.of(new AssistantMessage.ToolCall(
                                "tc-0-0", "function", "fence_probe", "{}")))
                        .build();
                return new ChatResponse(List.of(new Generation(toolCallMsg)));
            }
            leaseStore.steal(sessionId, "external-thief", Duration.ofSeconds(90));
            return new ChatResponse(List.of(new Generation(new AssistantMessage("final-answer"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
