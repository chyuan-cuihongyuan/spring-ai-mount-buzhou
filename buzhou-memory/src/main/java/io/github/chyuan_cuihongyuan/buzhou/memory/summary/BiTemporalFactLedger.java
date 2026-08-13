package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 双时序事实有效性台账（wayfinder T26 / docs/spec/11 memory，来源 Zep/Graphiti + Mem0g）：
 * 段正文被取代（UPDATE/DELETE）时<b>标记失效（valid_until）而非物理删除</b>，保留 valid_from——
 * 支持时序回查（「某时点以为的事实」）与排障（事实演变轨迹）。存于会话状态
 * {@code bitemp.summary.<SECTION>}（JSON 数组，≤{@link #MAX_RECORDS} 条/段）。
 */
public class BiTemporalFactLedger {

    /** 单条有效性记录：一段正文的一个版本及其生效区间。 */
    public record ValidityRecord(String section, String body, long fromGeneration,
                                 long toGeneration, Instant validFrom, Instant validUntil) {
        /** 该版本在给定时点是否生效（validFrom <= t < validUntil；validUntil 空 = 现行有效）。 */
        public boolean effectiveAt(Instant t) {
            if (validFrom != null && t.isBefore(validFrom)) {
                return false;
            }
            return validUntil == null || t.isBefore(validUntil);
        }
    }

    static final int MAX_RECORDS = 32;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String KEY_PREFIX = "bitemp.summary.";

    private final SessionStateStore stateStore;

    public BiTemporalFactLedger(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    /**
     * 记录一次取代：旧版本闭合 validity（validUntil=now），新版本开口生效。
     * 返回新版本的 ValidityRecord（供调用方留存）。
     */
    public ValidityRecord recordSuperseded(String sessionId, String section, String oldBody,
                                           long oldGeneration, long newGeneration) {
        Instant now = Instant.now();
        List<ValidityRecord> records = load(sessionId, section);
        // 闭合未闭合的旧版本（同代幂等：同 oldGeneration 已闭合则跳过）
        for (int i = 0; i < records.size(); i++) {
            ValidityRecord record = records.get(i);
            if (record.validUntil() == null && record.toGeneration() >= oldGeneration) {
                records.set(i, new ValidityRecord(record.section(), record.body(),
                        record.fromGeneration(), record.toGeneration(),
                        record.validFrom(), now));
            }
        }
        if (oldBody != null && !oldBody.isBlank()) {
            records.add(new ValidityRecord(section, oldBody, oldGeneration, newGeneration,
                    null, now));
        }
        // 新版本同步开口生效（body 留空——现行正文以摘要库为准；台账只记版本区间）
        records.add(new ValidityRecord(section, null, newGeneration, Long.MAX_VALUE,
                now, null));
        while (records.size() > MAX_RECORDS) {
            records.removeFirst();
        }
        save(sessionId, section, records);
        return new ValidityRecord(section, null, newGeneration, Long.MAX_VALUE, now, null);
    }

    /** 段的完整演变轨迹（时序升序）。 */
    public List<ValidityRecord> historyOf(String sessionId, String section) {
        return load(sessionId, section);
    }

    /** 时序回查：某时点生效的版本（无则 empty）。 */
    public Optional<ValidityRecord> validAt(String sessionId, String section, Instant at) {
        return load(sessionId, section).stream()
                .filter(record -> record.effectiveAt(at))
                .reduce((first, second) -> second); // 多版本命中取最新
    }

    private List<ValidityRecord> load(String sessionId, String section) {
        String json = stateStore.get(sessionId, keyOf(section)).map(io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry::value).orElse(null);
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<ValidityRecord> records = new ArrayList<>();
            for (var node : MAPPER.readTree(json)) {
                records.add(new ValidityRecord(
                        textOf(node, "section", section),
                        textOf(node, "body", null),
                        node.get("fromGeneration") != null ? node.get("fromGeneration").asLong() : 0,
                        node.get("toGeneration") != null ? node.get("toGeneration").asLong() : 0,
                        node.get("validFrom") != null ? Instant.parse(node.get("validFrom").asText()) : null,
                        node.get("validUntil") != null && !node.get("validUntil").isNull()
                                ? Instant.parse(node.get("validUntil").asText()) : null));
            }
            return records;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void save(String sessionId, String section, List<ValidityRecord> records) {
        try {
            var array = MAPPER.createArrayNode();
            for (ValidityRecord record : records) {
                var node = MAPPER.createObjectNode();
                node.put("section", record.section());
                node.put("body", record.body());
                node.put("fromGeneration", record.fromGeneration());
                node.put("toGeneration", record.toGeneration());
                if (record.validFrom() != null) {
                    node.put("validFrom", record.validFrom().toString());
                }
                if (record.validUntil() != null) {
                    node.put("validUntil", record.validUntil().toString());
                } else {
                    node.putNull("validUntil");
                }
                array.add(node);
            }
            stateStore.put(sessionId, new io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry(
                    keyOf(section), MAPPER.writeValueAsString(array), "buzhou-memory", 0, 0,
                    java.time.Instant.now()));
        } catch (Exception ignored) {
        }
    }

    private static String keyOf(String section) {
        return KEY_PREFIX + section;
    }

    private static String textOf(com.fasterxml.jackson.databind.JsonNode node, String field,
                                 String fallback) {
        return node.get(field) != null && !node.get(field).isNull()
                ? node.get(field).asText() : fallback;
    }
}
