package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1119 / impl 867：compact_now×归档 生命周期组合——归档（级联删除活数据）
 * 后 compact_now 调用走 skipped 桶（消息已级联删除）+ 双读面各自守恒。纯测试轮。
 */
class ArchiveCompactComboTest {

    private BuzhouStores stores;
    private String sid;

    @BeforeEach
    void setUp() {
        CompactNowTool.resetForTest();
        SessionArchiver.resetForTest();
        stores = Buzhou.inMemoryStores();
        sid = "arch-compact-sess";
    }

    private CompactNowTool compactTool() {
        return new CompactNowTool(stores.messageStore(),
                new SummaryStoreBridge(stores.summaryStore()),
                new DefaultSummaryGenerator(), new StubSummaryModel(), 2);
    }

    /** 摘要模型 stub（同 CompactNowToolTest）。 */
    static final class StubSummaryModel implements org.springframework.ai.chat.model.ChatModel {
        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(
                    "# 目标\nx\n# 约束\nx\n# 已完成任务\nx\n# 当前进行\nx\n# 关键决定\nx\n"
                            + "# 未解决问题\nx\n# 重要事实\nx\n# 失败尝试\nx\n# 其他上下文\nx"))));
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    @Test
    void archiveThenCompactGoesToSkipped() {
        // 5 轮消息 → compact 成功折入 → 消息水位推进
        CompactNowTool tool = compactTool();
        for (int turn = 1; turn <= 5; turn++) {
            stores.messageStore().append(sid, List.of(new BuzhouMessage(
                    UUID.randomUUID().toString(), sid, turn, 0, Role.USER, "第" + turn + "轮",
                    List.of(), null, null, null, Map.of(), Instant.now())));
        }
        assertThat(tool.call("{}", new ToolContext(
                Map.of(HarnessToolCallingManager.SESSION_ID_KEY, sid)))).contains("压缩完成");

        // 归档会话（级联删除活数据）
        SessionArchiver archiver = new SessionArchiver(stores, new SessionCleaner(stores));
        assertThat(archiver.archive(sid)).isTrue();

        // 归档后 compact_now：消息已清 → skipped 桶
        String out = tool.call("{}", new ToolContext(
                Map.of(HarnessToolCallingManager.SESSION_ID_KEY, sid)));
        assertThat(out).contains("无需压缩");

        // 双读面各自守恒
        CompactNowTool.CompactNowStats cs = CompactNowTool.stats();
        assertThat(cs.calls()).isEqualTo(cs.successes() + cs.skippeds()
                + cs.failures() + cs.unboundRejects());
        SessionArchiver.ArchiveStats as = SessionArchiver.stats();
        assertThat(as.archiveCalls()).isEqualTo(1);
        assertThat(as.archived()).isEqualTo(1);
    }

    @Test
    void resetsAreIndependent() {
        CompactNowTool compact = compactTool();
        compact.call("{}", null); // unbound
        assertThat(CompactNowTool.stats().calls()).isEqualTo(1);

        CompactNowTool.resetForTest();
        assertThat(CompactNowTool.stats().calls()).isZero();
        assertThat(SessionArchiver.stats().archiveCalls()).isZero();
    }
}
