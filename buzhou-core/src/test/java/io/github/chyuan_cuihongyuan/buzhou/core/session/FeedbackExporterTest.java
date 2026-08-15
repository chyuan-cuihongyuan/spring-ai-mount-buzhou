package io.github.chyuan_cuihongyuan.buzhou.core.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.session.DefaultAgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 48 §A / T174 / impl-143：反馈导出与评估衔接端到端。
 */
class FeedbackExporterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void exportCarriesEntriesAndNegativeTurns() throws Exception {
        BuzhouStores stores = Buzhou.inMemoryStores();
        FeedbackExporter exporter = new FeedbackExporter(stores.sessionStateStore());

        // 空：段缺席（既有导出零变化）
        assertThat(exporter.exportSegment("sess-x")).isNull();

        // 混合反馈：turn2 boolean=false（负）、turn3 numeric=-1（负）、turn3 numeric=4（正）、
        // turn1 categorical=good（无极性）
        putFeedback(stores, "sess-x", "buzhou.feedback.2.100-1", "boolean", "false", "差", "user", 2);
        putFeedback(stores, "sess-x", "buzhou.feedback.3.101-2", "numeric", "-1", null, "implicit", 3);
        putFeedback(stores, "sess-x", "buzhou.feedback.3.102-3", "numeric", "4", null, "user", 3);
        putFeedback(stores, "sess-x", "buzhou.feedback.1.103-4", "categorical", "good", null, "user", 1);

        String json = exporter.exportSegment("sess-x");
        Map<String, Object> segment = MAPPER.readValue(json, Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) segment.get("entries");
        assertThat(entries).hasSize(4);
        // 键序 = 字典序（turnSeq 前缀，再毫秒-序号）：1.103-4 < 2.100-1 < 3.101-2 < 3.102-3
        assertThat(entries.get(1)).containsEntry("key", "buzhou.feedback.2.100-1")
                .containsEntry("turnSeq", 2).containsEntry("type", "boolean")
                .containsEntry("value", "false").containsEntry("negative", true);
        assertThat(entries.get(0)).containsEntry("type", "categorical")
                .containsEntry("negative", false);

        // 负反馈轮汇总：去重升序 [2, 3]；categorical 不参与
        assertThat(segment.get("negativeTurnSeqs")).isEqualTo(List.of(2, 3));
    }

    @Test
    void importReplaysEntriesAndRoundTrips() throws Exception {
        BuzhouStores stores = Buzhou.inMemoryStores();
        FeedbackExporter exporter = new FeedbackExporter(stores.sessionStateStore());
        putFeedback(stores, "sess-src", "buzhou.feedback.1.100-1", "boolean", "true", "好", "user", 1);
        putFeedback(stores, "sess-src", "buzhou.feedback.1.101-2", "numeric", "5", null, "user", 1);
        String json = exporter.exportSegment("sess-src");
        assertThat(json).isNotNull();

        exporter.importSegment("sess-dst", json);

        Map<String, StateEntry> replayed = stores.sessionStateStore()
                .scanByPrefix("sess-dst", "buzhou.feedback.");
        assertThat(replayed).hasSize(2);
        assertThat(replayed.keySet()).contains("buzhou.feedback.1.100-1", "buzhou.feedback.1.101-2");
        assertThat(replayed.values()).allMatch(e -> "turn-feedback".equals(e.producer())
                && e.createdTurn() == 1);

        // 往返保真：导出 → 导入 → 再导出等价
        String json2 = exporter.exportSegment("sess-dst");
        assertThat(MAPPER.readTree(json2)).isEqualTo(MAPPER.readTree(json));
    }

    /** 端到端：会话 rateTurn → runtime 导出带 core.feedback 段。 */
    @Test
    void runtimeExportIncludesFeedbackSegment() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores);
        ((DefaultAgentRuntime) runtime).setExportExtensions(
                List.of(new FeedbackExporter(stores.sessionStateStore())));

        AgentSession session = runtime.spawn("app", "agent", "sess-e2e");
        session.chat("问");
        session.rateTurn(1, "boolean", "false", "不好", null);

        SessionExport export = runtime.exportSession("sess-e2e");
        assertThat(export.extensions()).containsKey(FeedbackExporter.NAME);
        session.close();
    }

    private static void putFeedback(BuzhouStores stores, String sessionId, String key,
                                    String type, String value, String comment,
                                    String source, int turnSeq) {
        stores.sessionStateStore().put(sessionId, new StateEntry(key,
                FeedbackExporterTest.encoded(type, value, comment, source),
                "turn-feedback", turnSeq, null, java.time.Instant.now()));
    }

    private static String encoded(String type, String value, String comment, String source) {
        // 与 DefaultAgentSession.encodeFeedback 同构（k=v& URLEncoded）
        return "type=" + type + "&value=" + value + "&comment="
                + (comment == null ? "" : comment) + "&source=" + source
                + "&at=2026-08-16T00:00:00Z";
    }

    static class ScriptedChatModel implements ChatModel {
        private final Queue<ChatResponse> script = new ConcurrentLinkedQueue<>();

        @Override
        public org.springframework.ai.chat.prompt.ChatOptions getOptions() {
            return org.springframework.ai.model.tool.ToolCallingChatOptions.builder().build();
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("default"))));
        }

        @Override
        public reactor.core.publisher.Flux<ChatResponse> stream(Prompt prompt) {
            return reactor.core.publisher.Flux.just(call(prompt));
        }
    }
}
