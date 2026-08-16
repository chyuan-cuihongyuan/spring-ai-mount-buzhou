package io.github.chyuan_cuihongyuan.buzhou.core.session;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * turn 反馈导出扩展（spec 48 §A / T174 / impl-143）：T173 落地的反馈建在 state 的
 * {@value #FEEDBACK_PREFIX} 命名空间——导出 = scanByPrefix 解码为结构化行 + 负反馈极性标记
 * 与汇总（评估集筛选用）；导入 = 按原键回放（键含轮次归属，时序可排）。
 *
 * <p>极性口径：boolean 型 {@code false}、numeric 型负值为负（显式负信号）；categorical
 * 无极性假设。空反馈导出 null（段不携带，既有导出消费方零影响）。
 *
 * @since 1.0.0
 */
public final class FeedbackExporter implements SessionExportExtension {

    public static final String NAME = "core.feedback";
    /** 反馈 state store 键前缀（DefaultAgentSession.rateTurn 写入面；单一事实源）。 */
    public static final String FEEDBACK_PREFIX = "buzhou.feedback.";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final System.Logger LOGGER = System.getLogger(FeedbackExporter.class.getName());

    private final SessionStateStore stateStore;

    public FeedbackExporter(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String exportSegment(String sessionId) {
        Map<String, StateEntry> feedback = stateStore.scanByPrefix(sessionId, FEEDBACK_PREFIX);
        if (feedback.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> entries = new ArrayList<>();
        TreeSet<Number> negativeTurns = new TreeSet<>(
                (a, b) -> Integer.compare(a.intValue(), b.intValue()));
        feedback.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    Map<String, String> fields = decode(e.getValue().value());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("key", e.getKey());
                    row.put("turnSeq", e.getValue().createdTurn());
                    row.put("type", fields.get("type"));
                    row.put("value", fields.get("value"));
                    if (fields.get("comment") != null && !fields.get("comment").isBlank()) {
                        row.put("comment", fields.get("comment"));
                    }
                    row.put("source", fields.get("source"));
                    row.put("at", fields.get("at"));
                    boolean negative = isNegative(fields.get("type"), fields.get("value"));
                    row.put("negative", negative);
                    if (negative) {
                        negativeTurns.add(e.getValue().createdTurn());
                    }
                    entries.add(row);
                });
        Map<String, Object> segment = new LinkedHashMap<>();
        segment.put("entries", entries);
        segment.put("negativeTurnSeqs", new ArrayList<>(negativeTurns));
        try {
            return MAPPER.writeValueAsString(segment);
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "反馈段导出失败（跳过）：" + e.getMessage());
            return null;
        }
    }

    @Override
    public void importSegment(String targetSessionId, String json) {
        try {
            Map<String, Object> segment = MAPPER.readValue(json,
                    new TypeReference<Map<String, Object>>() {
                    });
            Object raw = segment.get("entries");
            if (!(raw instanceof List<?> rows)) {
                return;
            }
            for (Object r : rows) {
                if (!(r instanceof Map<?, ?> row)) {
                    continue;
                }
                String key = String.valueOf(row.get("key"));
                String encoded = reencode(row);
                stateStore.put(targetSessionId, new StateEntry(key, encoded, "turn-feedback",
                        row.get("turnSeq") instanceof Number n ? n.intValue() : 0,
                        null, java.time.Instant.now()));
            }
        } catch (Exception e) {
            throw new IllegalStateException("反馈段导入失败：" + e.getMessage(), e);
        }
    }

    /**
     * 极性判定（单一事实源）：boolean false / numeric 负值为负；categorical 无极性。
     * spec 52 §B / T191：FeedbackImporter 回流复用同口径（public 即为此）。
     */
    public static boolean isNegative(String type, String value) {
        if (type == null || value == null) {
            return false;
        }
        return switch (type) {
            case "boolean" -> "false".equalsIgnoreCase(value);
            case "numeric" -> {
                try {
                    yield Long.parseLong(value) < 0;
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            default -> false;
        };
    }

    /** 行字段重编码为 state store 形态（与 rateTurn 的 k=v& 编码一致）。 */
    private static String reencode(Map<?, ?> row) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("type", stringOr(row.get("type"), ""));
        fields.put("value", stringOr(row.get("value"), ""));
        fields.put("comment", row.get("comment") == null ? "" : String.valueOf(row.get("comment")));
        fields.put("source", stringOr(row.get("source"), "user"));
        fields.put("at", row.get("at") == null
                ? java.time.Instant.now().toString() : String.valueOf(row.get("at")));
        StringBuilder sb = new StringBuilder();
        fields.forEach((k, v) -> {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(k).append('=').append(encode(v));
        });
        return sb.toString();
    }

    private static String stringOr(Object o, String fallback) {
        return o == null ? fallback : String.valueOf(o);
    }

    private static String encode(String raw) {
        try {
            return java.net.URLEncoder.encode(raw == null ? "" : raw, StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return String.valueOf(raw);
        }
    }

    /** k=v& 解码（与 DefaultAgentSession.encodeFeedback 双向）。 */
    /** k=v& 行字段解码（spec 52 §B / T191：回流通道复用，public 即为此）。 */
    public static Map<String, String> decode(String encoded) {
        Map<String, String> out = new LinkedHashMap<>();
        if (encoded == null || encoded.isBlank()) {
            return out;
        }
        for (String pair : encoded.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            out.put(pair.substring(0, eq), decodeOne(pair.substring(eq + 1)));
        }
        return out;
    }

    private static String decodeOne(String encoded) {
        try {
            return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return encoded;
        }
    }
}
