package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySummaryStore;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T27 compact_now 语义边界压缩触发（docs/spec/11 memory，来源 LangChain Deep Agents）：
 * 模型在任务边界调用 compact_now → 未摘要完成轮折入摘要（保真、幂等）；
 * token 阈值安全网不受影响（双触发路径）。
 */
class CompactNowToolTest {

    private static BuzhouMessage user(String sessionId, int turn, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 0, Role.USER,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** 摘要模型 stub：合并请求回单段九段文本。 */
    static final class StubSummaryModel implements ChatModel {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            calls.incrementAndGet();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(
                    "## CURRENT_STATE（当前工作现场）\n压缩后现场：任务 A 已完成\n"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    @Test
    void compactNowFoldsPendingTurnsAndReportsStats() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "compact-sess";
        for (int turn = 1; turn <= 5; turn++) {
            stores.messageStore().append(sid, List.of(user(sid, turn, "第" + turn + "轮内容")));
        }
        StubSummaryModel summaryModel = new StubSummaryModel();
        SummaryStoreBridge bridge = new SummaryStoreBridge(stores.summaryStore());
        CompactNowTool tool = new CompactNowTool(stores.messageStore(), bridge,
                new DefaultSummaryGenerator(), summaryModel, 2);

        String result = tool.call("{}", new ToolContext(
                Map.of(HarnessToolCallingManager.SESSION_ID_KEY, sid)));

        // 压缩发生 + 统计回报（覆盖轮次/代际/保真提示）
        assertThat(result).contains("[compact_now] 压缩完成");
        assertThat(result).contains("新折入 3 条消息");
        NineSectionSummary saved = bridge.loadLatest(sid).orElseThrow();
        assertThat(saved.coversUpToTurn()).isEqualTo(3);
        assertThat(saved.summarizedMessageIds()).hasSize(3);
        // 幂等：再次调用（无新消息）→ 无需压缩，不再消耗摘要模型
        int callsBefore = summaryModel.calls.get();
        String again = tool.call("{}", new ToolContext(
                Map.of(HarnessToolCallingManager.SESSION_ID_KEY, sid)));
        assertThat(again).contains("无需压缩");
        assertThat(summaryModel.calls.get()).isEqualTo(callsBefore);
    }

    @Test
    void compactNowWithoutSessionBindingReturnsGuidance() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        CompactNowTool tool = new CompactNowTool(stores.messageStore(),
                new SummaryStoreBridge(stores.summaryStore()),
                new DefaultSummaryGenerator(), new StubSummaryModel(), 2);

        assertThat(tool.call("{}", null)).contains("未绑定会话");
    }

    @Test
    void modelCanTriggerCompactionAtTaskBoundaryEndToEnd() {
        // 会话接缝：反应式模型在任务边界调用 compact_now，下一轮注入视图携带最新摘要
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "compact-e2e";
        for (int turn = 1; turn <= 4; turn++) {
            stores.messageStore().append(sid, List.of(user(sid, turn, "历史第" + turn + "轮")));
        }
        StubSummaryModel summaryModel = new StubSummaryModel();
        BoundaryMockModel mainModel = new BoundaryMockModel();
        var config = MemoryModule.configure(Map.of(), stores, mainModel, summaryModel);
        AgentRuntime runtime = Buzhou.runtime(mainModel, stores, config);

        AgentSession session = runtime.spawn("compact-app", "boundary-agent", sid);
        String reply1 = session.chat("阶段一完成，整理一下上下文");
        session.close();

        assertThat(reply1).isEqualTo("已在任务边界完成压缩");
        // compact_now 真的把未摘要轮折入了摘要库
        assertThat(summaryModel.calls.get()).isGreaterThanOrEqualTo(1);
        assertThat(new SummaryStoreBridge(stores.summaryStore()).loadLatest(sid)).isPresent();
        // 模型收到了压缩完成的统计反馈（含 [compact_now]）
        assertThat(mainModel.compactResultSeen).contains("[compact_now]");
    }

    /** 任务边界 mock：先调 compact_now，收到统计反馈后收尾。 */
    static final class BoundaryMockModel implements ChatModel {
        String compactResultSeen;

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            List<Message> msgs = prompt.getInstructions();
            Message last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
            if (last instanceof ToolResponseMessage toolResponse) {
                compactResultSeen = toolResponse.getResponses().getFirst().responseData();
                return new ChatResponse(List.of(new Generation(
                        new AssistantMessage("已在任务边界完成压缩"))));
            }
            return new ChatResponse(List.of(new Generation(AssistantMessage.builder()
                    .content("").toolCalls(List.of(new AssistantMessage.ToolCall(
                            "tc", "function", "compact_now", "{}"))).build())));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }
}
