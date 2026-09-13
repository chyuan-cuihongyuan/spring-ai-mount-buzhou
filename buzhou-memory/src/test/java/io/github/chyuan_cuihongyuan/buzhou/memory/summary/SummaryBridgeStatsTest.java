package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.memory.compact.ManualCompactor;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryBridgeStatsTest {

    static final class StubSummaryModel implements ChatModel {
        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(
                    "## CURRENT_STATE（当前工作现场）\n桥读面测试现场\n"))));
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

    private static NineSectionSummary summary(long generation) {
        var sections = new EnumMap<SummarySection, SectionContent>(SummarySection.class);
        sections.put(SummarySection.values()[0],
                new SectionContent("正文", SectionContent.Form.FULL, List.of()));
        return new NineSectionSummary(generation, generation == 0 ? 0 : (int) generation,
                sections, List.of());
    }

    @Test
    void freshBridgeHasZeroCounts() {
        SummaryStoreBridge bridge = new SummaryStoreBridge(
                Buzhou.inMemoryStores().summaryStore());

        assertThat(bridge.stats()).isEqualTo(new SummaryStoreBridge.SummaryStoreStats(0, 0, 0));
    }

    @Test
    void saveAndLoadCounted() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        String sid = "bridge-sess";
        for (int turn = 1; turn <= 4; turn++) {
            stores.messageStore().append(sid, List.of(user(sid, turn, "第" + turn + "轮")));
        }
        SummaryStoreBridge bridge = new SummaryStoreBridge(stores.summaryStore());
        ManualCompactor compactor = new ManualCompactor(stores.messageStore(),
                bridge, new DefaultSummaryGenerator(),
                new StubSummaryModel(), 2);

        compactor.compact(sid);
        compactor.compact(sid); // 幂等——不新增 save

        assertThat(bridge.stats().saves()).isEqualTo(1);
    }

    @Test
    void generationRegressionCounted() {
        SummaryStoreBridge bridge = new SummaryStoreBridge(
                Buzhou.inMemoryStores().summaryStore());

        bridge.save("s", summary(5));
        bridge.save("s", summary(3));

        assertThat(bridge.stats().generationRegressions()).isEqualTo(1);
        assertThat(bridge.stats().saves()).isEqualTo(2);
    }
}
