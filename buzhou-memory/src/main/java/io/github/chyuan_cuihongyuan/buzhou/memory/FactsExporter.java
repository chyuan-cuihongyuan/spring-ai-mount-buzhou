package io.github.chyuan_cuihongyuan.buzhou.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionExportExtension;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * facts 导出扩展（spec 36 §A / T121 / impl-96）：facts 建在 state 的
 * {@code fact.*} 命名空间（spec 07 事实模型）——导出 = {@code scanByPrefix("fact.")}
 * 的 StateEntry 原样 JSON 段；导入 = 按新 sessionId 逐条 put 回（键值无损往返，
 * producer/createdTurn/ttlTurns 语义字段全保留）。
 *
 * @since 1.0.0
 */
public final class FactsExporter implements SessionExportExtension {

    public static final String NAME = "memory.facts";
    static final String FACT_PREFIX = "fact.";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final System.Logger LOGGER =
            System.getLogger(FactsExporter.class.getName());

    private final SessionStateStore stateStore;

    public FactsExporter(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String exportSegment(String sessionId) {
        Map<String, StateEntry> facts = stateStore.scanByPrefix(sessionId, FACT_PREFIX);
        if (facts.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        facts.forEach((key, entry) -> {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("key", key);
            row.put("value", entry.value());
            row.put("producer", entry.producer());
            row.put("createdTurn", entry.createdTurn());
            row.put("ttlTurns", entry.ttlTurns());
            rows.add(row);
        });
        try {
            return MAPPER.writeValueAsString(rows);
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "facts 段导出失败（跳过）：" + e.getMessage());
            return null;
        }
    }

    @Override
    public void importSegment(String targetSessionId, String json) {
        try {
            List<Map<String, Object>> rows = MAPPER.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            for (Map<String, Object> row : rows) {
                stateStore.put(targetSessionId, new StateEntry(
                        String.valueOf(row.get("key")),
                        String.valueOf(row.get("value")),
                        row.get("producer") == null ? "facts-export" : String.valueOf(row.get("producer")),
                        row.get("createdTurn") instanceof Number n ? n.intValue() : 0,
                        row.get("ttlTurns") instanceof Number n ? n.intValue() : null,
                        Instant.now()));
            }
        } catch (Exception e) {
            throw new IllegalStateException("facts 段导入失败：" + e.getMessage(), e);
        }
    }
}
