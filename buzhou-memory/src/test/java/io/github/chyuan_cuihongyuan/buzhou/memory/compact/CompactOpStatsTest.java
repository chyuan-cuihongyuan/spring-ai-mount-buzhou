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

class CompactOpStatsTest {

    static final class StubSummaryModel implements ChatModel {
        private final boolean boom;

        StubSummaryModel(boolean boom) {
            this.boom = boom;
        }

        StubSummaryModel() {
            this(false);
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            if (boom) {
                throw new IllegalStateException("摘要模型故障");
            }
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

    private static ManualCompactor compactor(BuzhouStores stores, boolean boomModel) {
        return new ManualCompactor(stores.messageStore(),
                new SummaryStoreBridge(stores.summaryStore()), new DefaultSummaryGenerator(),
                new StubSummaryModel(boomModel), 2);
    }

    @Test
    void freshInstanceHasZeroCounts() {
        ManualCompactor compactor = compactor(Buzhou.inMemoryStores(), false);

        assertThat(compactor.opStats()).isEqualTo(new ManualCompactor.CompactOpStats(0, 0, 0, 0, 0));
    }

    @Test
    void completionCountsAttemptCompletedAndFolded() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "compact-me";
        for (int turn = 1; turn <= 5; turn++) {
            stores.messageStore().append(sid, List.of(user(sid, turn, "第" + turn + "轮")));
        }
        ManualCompactor compactor = compactor(stores, false);

        ManualCompactor.CompactResult result = compactor.compact(sid);

        assertThat(result.skipped()).isFalse();
        ManualCompactor.CompactOpStats stats = compactor.opStats();
        assertThat(stats.attempts()).isEqualTo(1);
        assertThat(stats.completed()).isEqualTo(1);
        assertThat(stats.skipped()).isZero();
        assertThat(stats.failed()).isZero();
        assertThat(stats.foldedMessages()).isEqualTo(result.foldedMessages());
    }

    @Test
    void idempotentSecondCallCountsSkipped() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "compact-me";
        for (int turn = 1; turn <= 5; turn++) {
            stores.messageStore().append(sid, List.of(user(sid, turn, "第" + turn + "轮")));
        }
        ManualCompactor compactor = compactor(stores, false);
        compactor.compact(sid);

        ManualCompactor.CompactResult second = compactor.compact(sid);

        assertThat(second.skipped()).isTrue();
        ManualCompactor.CompactOpStats stats = compactor.opStats();
        assertThat(stats.attempts()).isEqualTo(2);
        assertThat(stats.completed()).isEqualTo(1);
        assertThat(stats.skipped()).isEqualTo(1);
        assertThat(stats.completed() + stats.skipped() + stats.failed()).isEqualTo(stats.attempts());
    }

    @Test
    void modelFailureCountsFailedButNotBrokenAttempt() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "compact-me";
        for (int turn = 1; turn <= 5; turn++) {
            stores.messageStore().append(sid, List.of(user(sid, turn, "第" + turn + "轮")));
        }
        ManualCompactor compactor = compactor(stores, true);

        ManualCompactor.CompactResult result = compactor.compact(sid);

        assertThat(result.error()).isNotNull();
        assertThat(compactor.opStats().failed()).isEqualTo(1);
        assertThat(compactor.opStats().attempts()).isEqualTo(1);
    }
}
