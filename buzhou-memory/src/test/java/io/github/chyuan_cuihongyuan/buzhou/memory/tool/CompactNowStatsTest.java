package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1059 / impl 811：compact_now 手动压缩判定读面——压缩完成/无需压缩/
 * 未绑定三桶可见、四桶守恒恒等式、resetForTest 归零。
 * 骨架同 CompactNowToolTest（Buzhou.inMemoryStores + StubSummaryModel）。
 */
class CompactNowStatsTest {

    private static BuzhouMessage user(String sessionId, int turn, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 0, Role.USER,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** 摘要模型 stub：合并请求回单段九段文本。 */
    static final class StubSummaryModel implements ChatModel {
        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(
                    "# 目标\nx\n# 约束\nx\n# 已完成任务\nx\n# 当前进行\nx\n# 关键决定\nx\n"
                            + "# 未解决问题\nx\n# 重要事实\nx\n# 失败尝试\nx\n# 其他上下文\nx"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    @BeforeEach
    void reset() {
        CompactNowTool.resetForTest();
    }

    private CompactNowTool tool(BuzhouStores stores) {
        return new CompactNowTool(stores.messageStore(),
                new SummaryStoreBridge(stores.summaryStore()),
                new DefaultSummaryGenerator(), new StubSummaryModel(), 2);
    }

    @Test
    void successAndSkipAndUnboundFallIntoTheirBuckets() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "stats-sess";
        for (int turn = 1; turn <= 5; turn++) {
            stores.messageStore().append(sid, List.of(user(sid, turn, "第" + turn + "轮")));
        }
        CompactNowTool tool = tool(stores);

        assertThat(tool.call("{}", new ToolContext(
                Map.of(HarnessToolCallingManager.SESSION_ID_KEY, sid))))
                .contains("压缩完成");
        assertThat(tool.call("{}", new ToolContext(
                Map.of(HarnessToolCallingManager.SESSION_ID_KEY, sid))))
                .contains("无需压缩");
        assertThat(tool.call("{}", null)).contains("未绑定会话");

        CompactNowTool.CompactNowStats stats = CompactNowTool.stats();
        assertThat(stats.calls()).isEqualTo(3);
        assertThat(stats.successes()).isEqualTo(1);
        assertThat(stats.skippeds()).isEqualTo(1);
        assertThat(stats.unboundRejects()).isEqualTo(1);
        assertThat(stats.calls())
                .isEqualTo(stats.successes() + stats.skippeds() + stats.failures()
                        + stats.unboundRejects());
    }

    @Test
    void resetForTestZeroesCounters() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        CompactNowTool tool = tool(stores);
        tool.call("{}", null);
        assertThat(CompactNowTool.stats().calls()).isEqualTo(1);

        CompactNowTool.resetForTest();

        CompactNowTool.CompactNowStats stats = CompactNowTool.stats();
        assertThat(stats.calls()).isZero();
        assertThat(stats.unboundRejects()).isZero();
        assertThat(stats.successes()).isZero();
    }
}
