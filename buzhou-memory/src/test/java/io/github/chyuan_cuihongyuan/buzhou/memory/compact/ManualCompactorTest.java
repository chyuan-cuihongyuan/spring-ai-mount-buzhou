package io.github.chyuan_cuihongyuan.buzhou.memory.compact;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.DefaultSummaryGenerator;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
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
 * 宿主侧手动压缩与摘要导出测试（spec 20 / T90 / impl-65）：压缩统计/幂等、导出（类型化 +
 * Markdown + 空 empty）、MemoryModule.manualCompactor 装配。
 */
class ManualCompactorTest {

    /** 摘要模型 stub（与 CompactNowToolTest 同手法）。 */
    static final class StubSummaryModel implements ChatModel {
        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(
                    "## CURRENT_STATE（当前工作现场）\n手动压缩后现场\n"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }

    private static BuzhouMessage user(String sessionId, int turn, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 0, Role.USER,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** 手动压缩折入早前轮次 + 统计；再次调用幂等 skipped。 */
    @Test
    void hostSideCompactFoldsAndIsIdempotent() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "manual-sess";
        for (int turn = 1; turn <= 5; turn++) {
            stores.messageStore().append(sid, List.of(user(sid, turn, "第" + turn + "轮")));
        }
        ManualCompactor compactor = new ManualCompactor(stores.messageStore(),
                new SummaryStoreBridge(stores.summaryStore()), new DefaultSummaryGenerator(),
                new StubSummaryModel(), 2);

        ManualCompactor.CompactResult result = compactor.compact(sid);

        assertThat(result.skipped()).isFalse();
        assertThat(result.foldedMessages()).isEqualTo(3);
        assertThat(result.fromTurn()).isEqualTo(1);
        assertThat(result.toTurn()).isEqualTo(3);
        assertThat(result.generation()).isGreaterThanOrEqualTo(1);
        assertThat(result.error()).isNull();

        // 幂等：无新消息再次压缩 = skipped
        assertThat(compactor.compact(sid).skipped()).isTrue();
    }

    /** 导出：类型化 + Markdown；压缩前无摘要 = empty。 */
    @Test
    void exportSummaryTypedAndMarkdown() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "export-sess";
        stores.messageStore().append(sid, List.of(user(sid, 1, "内容"), user(sid, 2, "内容")));
        ManualCompactor compactor = new ManualCompactor(stores.messageStore(),
                new SummaryStoreBridge(stores.summaryStore()), new DefaultSummaryGenerator(),
                new StubSummaryModel(), 0);

        assertThat(compactor.exportSummary(sid)).isEmpty(); // 压缩前无摘要

        compactor.compact(sid);
        assertThat(compactor.exportSummary(sid)).isPresent();
        assertThat(compactor.exportSummaryMarkdown(sid))
                .hasValueSatisfying(markdown -> assertThat(markdown).contains("手动压缩后现场"));

        assertThat(compactor.exportSummary("no-such-session")).isEmpty();
    }

    /** 空历史 skipped；MemoryModule 装配（无摘要模型 = null）。 */
    @Test
    void emptyHistorySkippedAndModuleFactory() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        ManualCompactor compactor = new ManualCompactor(stores.messageStore(),
                new SummaryStoreBridge(stores.summaryStore()), new DefaultSummaryGenerator(),
                new StubSummaryModel(), 2);
        assertThat(compactor.compact("empty-sess").skipped()).isTrue();

        assertThat(io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule
                .manualCompactor(Buzhou.inMemoryStores(), null, Map.of())).isNull();
        assertThat(io.github.chyuan_cuihongyuan.buzhou.memory.MemoryModule
                .manualCompactor(stores, new StubSummaryModel(), Map.of())).isNotNull();
    }
}
