package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * {@code recall_search} 内置工具（wayfinder2 impl-15 / T41 / docs/spec/12 §memory-13）：
 * 模糊召回<b>精确原文</b>（text / time / embedding / hybrid 四模）——与
 * {@code evidence_lookup}（确定性指针）互补：指针管精确回读、recall 管模糊召回。
 * EMBEDDING/HYBRID 需 EmbeddingProvider（经 MemoryModule 注入；未注入时显式降级提示）。
 */
public class RecallSearchTool implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MessageStore messageStore;
    private final io.github.chyuan_cuihongyuan.buzhou.memory.recall.RecallSearch engine;

    public RecallSearchTool(MessageStore messageStore,
                            io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider provider) {
        this.messageStore = messageStore;
        this.engine = new io.github.chyuan_cuihongyuan.buzhou.memory.recall.RecallSearch(provider);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("recall_search")
                .description("模糊召回历史原文（压缩前的精确内容）：mode=text 子串 / time 轮次范围倒序 / "
                        + "embedding 语义近邻 / hybrid 融合（embedding/hybrid 需部署侧注入向量提供者）。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "mode":{"type":"string","enum":["text","time","embedding","hybrid"]},
                          "query":{"type":"string","description":"text/embedding/hybrid 的检索词"},
                          "fromTurn":{"type":"integer"},
                          "toTurn":{"type":"integer"},
                          "limit":{"type":"integer"}
                        },"required":["mode"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            var mode = io.github.chyuan_cuihongyuan.buzhou.memory.recall.RecallSearch.Mode
                    .valueOf(args.path("mode").asText("text").toUpperCase());
            if ((mode == io.github.chyuan_cuihongyuan.buzhou.memory.recall.RecallSearch.Mode.EMBEDDING
                    || mode == io.github.chyuan_cuihongyuan.buzhou.memory.recall.RecallSearch.Mode.HYBRID)
                    && !engine.vectorReady()) {
                return "[recall_search 降级] " + mode.name()
                        + " 模式需 EmbeddingProvider（未注入）；请改用 text 或 time 模式。";
            }
            String sessionId = toolContext == null || toolContext.getContext() == null ? null
                    : String.valueOf(toolContext.getContext().getOrDefault(
                            io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager.SESSION_ID_KEY,
                            null));
            if (sessionId == null || "null".equals(sessionId)) {
                return "[recall_search 失败] 缺少会话上下文（sessionId）";
            }
            var query = new io.github.chyuan_cuihongyuan.buzhou.memory.recall.RecallSearch.Query(
                    mode,
                    args.hasNonNull("query") ? args.path("query").asText() : null,
                    args.hasNonNull("fromTurn") ? args.path("fromTurn").asInt() : null,
                    args.hasNonNull("toTurn") ? args.path("toTurn").asInt() : null,
                    args.hasNonNull("limit") ? args.path("limit").asInt() : 10);
            var hits = engine.search(messageStore.load(sessionId), query);
            if (hits.isEmpty()) {
                return "[recall_search 无命中] mode=" + mode.name().toLowerCase();
            }
            StringBuilder out = new StringBuilder("命中 " + hits.size() + " 条（mode="
                    + mode.name().toLowerCase() + "）：");
            for (var hit : hits) {
                String snippet = hit.message().content() == null ? ""
                        : hit.message().content().strip().replaceAll("\\s+", " ");
                if (snippet.length() > 160) {
                    snippet = snippet.substring(0, 160) + "…";
                }
                out.append("\n- turn=").append(hit.turn())
                        .append(" role=").append(hit.message().role())
                        .append(" score=").append(String.format("%.2f", hit.score()))
                        .append(" id=").append(hit.message().id())
                        .append("：").append(snippet);
            }
            return out.toString();
        } catch (Exception e) {
            return "[recall_search 失败] " + e.getMessage();
        }
    }
}
