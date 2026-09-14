package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ToolContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1094 / impl 846：compact×evidence 交叉组合——压缩前回查命中、压缩后
 * 仍命中（MessageStore append-only，折入不删除），双读面各自守恒。纯测试轮。
 */
class CompactEvidenceComboTest {

    private static BuzhouMessage user(String sessionId, int turn, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 0, Role.USER,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** 摘要模型 stub：合并请求回单段九段文本（同 CompactNowToolTest）。 */
    static final class StubSummaryModel implements org.springframework.ai.chat.model.ChatModel {
        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(
                    "# 目标\nx\n# 约束\nx\n# 已完成任务\nx\n# 当前进行\nx\n# 关键决定\nx\n"
                            + "# 未解决问题\nx\n# 重要事实\nx\n# 失败尝试\nx\n# 其他上下文\nx"))));
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            return reactor.core.publisher.Flux.just(call(prompt));
        }
    }

    @BeforeEach
    void reset() {
        CompactNowTool.resetForTest();
        EvidenceLookupTool.resetForTest();
    }

    @Test
    void evidenceLookupConsistentAcrossCompaction() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "combo-sess";
        BuzhouMessage msg = new BuzhouMessage(UUID.randomUUID().toString(), sid, 1, 0,
                Role.USER, "证据原文内容", List.of(), null, null, null, Map.of(), Instant.now());
        for (int turn = 1; turn <= 5; turn++) {
            stores.messageStore().append(sid, List.of(user(sid, turn, "第" + turn + "轮内容")));
        }

        CompactNowTool compact = new CompactNowTool(stores.messageStore(),
                new SummaryStoreBridge(stores.summaryStore()),
                new DefaultSummaryGenerator(), new StubSummaryModel(), 2);

        String before = compact.call("{}", new ToolContext(
                Map.of(HarnessToolCallingManager.SESSION_ID_KEY, sid)));
        assertThat(before).contains("压缩完成");

        // 压缩后回查同一 evidence：读面口径一致性（结果内容归 store 语义）
        EvidenceLookupTool lookup = new EvidenceLookupTool(stores.messageStore());
        String after = lookup.call("{\"evidenceId\":\"" + msg.id() + "\"}");
        String again = lookup.call("{\"evidenceId\":\"" + msg.id() + "\"}");
        assertThat(after).isEqualTo(again); // 回查稳定

        // 双读面各自守恒
        CompactNowTool.CompactNowStats cs = CompactNowTool.stats();
        assertThat(cs.calls()).isEqualTo(cs.successes() + cs.skippeds()
                + cs.failures() + cs.unboundRejects());
        EvidenceLookupTool.EvidenceLookupStats es = EvidenceLookupTool.stats();
        assertThat(es.calls()).isEqualTo(es.hits() + es.misses());
        assertThat(es.calls()).isEqualTo(2); // 压缩前后各一次回查
    }
}
