package io.github.chyuan_cuihongyuan.buzhou.core.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话可移植导出文档（spec 28 / T107 / impl-82）：单 JSON 承载 messages + 最新 Summary +
 * State entries，供跨环境迁移/备份恢复/bug 复现包。
 *
 * <p><b>格式</b>：{@code format="buzhou.session-export"}、{@code version=1}；时间统一
 * epoch millis（不假定 jackson-jsr310 在 classpath）。{@link #toJson()} /
 * {@link #fromJson(String)} 是可移植边界；记录组件（BuzhouMessage 等）在 JVM 内直接可用。
 *
 * <p><b>边界（诚实声明）</b>：spill 证据**不内嵌**——引用随消息 metadata 原样导出，
 * 证据内容由 spill 侧另行导出（运维操作，见 runbook）；悬垂引用读路径由 spec 26
 * EVIDENCE_GONE 容错。facts 属 memory 模块内部存储，不在 core 导出面（fog）。
 * appId/agentName 尽力携带（消息存储不含时为 null，导入不依赖）。
 *
 * @since 1.0.0
 */
public record SessionExport(
        String format,
        int version,
        String sessionId,
        String appId,
        String agentName,
        long exportedAtEpochMs,
        List<BuzhouMessage> messages,
        StructuredSummary summary,
        Map<String, StateEntry> state,
        Map<String, String> extensions) {

    public static final String FORMAT = "buzhou.session-export";
    public static final int CURRENT_VERSION = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public SessionExport {
        if (!FORMAT.equals(format)) {
            throw new IllegalArgumentException("非本格式导出文档（format=" + format + "）");
        }
        if (version != CURRENT_VERSION) {
            throw new IllegalArgumentException("不支持的导出版本（version=" + version
                    + "，当前支持 " + CURRENT_VERSION + "）");
        }
        messages = messages == null ? List.of() : List.copyOf(messages);
        state = state == null ? Map.of() : Map.copyOf(state);
        extensions = extensions == null ? Map.of() : Map.copyOf(extensions);
    }

    /** impl-96 前的 8 槽形状（无扩展段）——保留委托，源 / 二进制兼容。 */
    public SessionExport(String format, int version, String sessionId, String appId,
            String agentName, long exportedAtEpochMs, List<BuzhouMessage> messages,
            StructuredSummary summary, Map<String, StateEntry> state) {
        this(format, version, sessionId, appId, agentName, exportedAtEpochMs, messages, summary,
                state, Map.of());
    }

    /** 便捷构造（format/version/exportedAt 自动填充）。 */
    public static SessionExport of(String sessionId, String appId, String agentName,
            List<BuzhouMessage> messages, StructuredSummary summary, Map<String, StateEntry> state) {
        return new SessionExport(FORMAT, CURRENT_VERSION, sessionId, appId, agentName,
                System.currentTimeMillis(), messages, summary, state, Map.of());
    }

    /** 带扩展段便捷构造（spec 36 §A / T121：模块自有数据段）。 */
    public static SessionExport of(String sessionId, String appId, String agentName,
            List<BuzhouMessage> messages, StructuredSummary summary, Map<String, StateEntry> state,
            Map<String, String> extensions) {
        return new SessionExport(FORMAT, CURRENT_VERSION, sessionId, appId, agentName,
                System.currentTimeMillis(), messages, summary, state, extensions);
    }

    /** spill 引用清单（evidenceId=消息 id + metadata 中的 spillUri；供消费方感知证据面）。 */
    public List<Map<String, String>> spillRefs() {
        List<Map<String, String>> refs = new ArrayList<>();
        for (BuzhouMessage message : messages) {
            Object spillUri = message.metadata().get("spillUri");
            if (spillUri instanceof String uri && !uri.isBlank()) {
                refs.add(Map.of("evidenceId", message.id(), "spillUri", uri));
            }
        }
        return refs;
    }

    /** 可移植 JSON（epoch millis 形态；导入端 {@link #fromJson} 反解）。 */
    public String toJson() {
        try {
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("format", format);
            doc.put("version", version);
            doc.put("sessionId", sessionId);
            doc.put("appId", appId);
            doc.put("agentName", agentName);
            doc.put("exportedAtEpochMs", exportedAtEpochMs);
            doc.put("messages", messages.stream().map(SessionExport::messageDto).toList());
            doc.put("summary", summary == null ? null : summaryDto(summary));
            doc.put("state", state.entrySet().stream()
                    .map(e -> Map.of("key", e.getKey(), "entry", entryDto(e.getValue())))
                    .toList());
            doc.put("extensions", extensions);
            return MAPPER.writeValueAsString(doc);
        } catch (Exception e) {
            throw new IllegalStateException("会话导出序列化失败（sessionId=" + sessionId + "）", e);
        }
    }

    /** 从可移植 JSON 反解（格式/版本不符 fail-fast；损坏抛 IllegalArgumentException）。 */
    public static SessionExport fromJson(String json) {
        try {
            Doc doc = MAPPER.readValue(json, Doc.class);
            if (doc == null || !FORMAT.equals(doc.format)) {
                throw new IllegalArgumentException("非本格式导出文档（format=" + (doc == null ? null : doc.format) + "）");
            }
            List<BuzhouMessage> messages = new ArrayList<>();
            if (doc.messages != null) {
                for (MessageDto m : doc.messages) {
                    messages.add(new BuzhouMessage(m.id, m.sessionId, m.turnSeq, m.seqInTurn,
                            io.github.chyuan_cuihongyuan.buzhou.core.message.Role.valueOf(m.role),
                            m.content,
                            m.toolCalls == null ? List.of() : m.toolCalls.stream()
                                    .map(tc -> new ToolCallRecord(tc.id(), tc.name(), tc.arguments())).toList(),
                            m.toolCallId, m.reasoningContent, m.reasoningSignature,
                            m.metadata == null ? Map.of() : m.metadata,
                            Instant.ofEpochMilli(m.createdAtEpochMs)));
                }
            }
            StructuredSummary summary = doc.summary == null ? null : new StructuredSummary(
                    doc.summary.sessionId, doc.summary.version,
                    doc.summary.sections == null ? Map.of() : doc.summary.sections,
                    doc.summary.tokenEstimate, Instant.ofEpochMilli(doc.summary.createdAtEpochMs));
            Map<String, StateEntry> state = new LinkedHashMap<>();
            if (doc.state != null) {
                for (StateDto s : doc.state) {
                    state.put(s.key, new StateEntry(s.entry.key, s.entry.value, s.entry.producer,
                            s.entry.createdTurn, s.entry.ttlTurns,
                            s.entry.updatedAtEpochMs == null ? null
                                    : Instant.ofEpochMilli(s.entry.updatedAtEpochMs)));
                }
            }
            return new SessionExport(doc.format, doc.version, doc.sessionId, doc.appId,
                    doc.agentName, doc.exportedAtEpochMs, messages, summary, state,
                    doc.extensions == null ? Map.of() : doc.extensions);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("会话导出文档损坏，无法反解", e);
        }
    }

    // ---- DTO 形态（Jackson 直转；全部 String/数字/Map，无 Instant） ----

    private static Map<String, Object> messageDto(BuzhouMessage m) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", m.id());
        dto.put("sessionId", m.sessionId());
        dto.put("turnSeq", m.turnSeq());
        dto.put("seqInTurn", m.seqInTurn());
        dto.put("role", m.role().name());
        dto.put("content", m.content());
        dto.put("toolCalls", m.toolCalls());
        dto.put("toolCallId", m.toolCallId());
        dto.put("reasoningContent", m.reasoningContent());
        dto.put("reasoningSignature", m.reasoningSignature());
        dto.put("metadata", m.metadata());
        dto.put("createdAtEpochMs", m.createdAt().toEpochMilli());
        return dto;
    }

    private static Map<String, Object> summaryDto(StructuredSummary s) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("sessionId", s.sessionId());
        dto.put("version", s.version());
        dto.put("sections", s.sections());
        dto.put("tokenEstimate", s.tokenEstimate());
        dto.put("createdAtEpochMs", s.createdAt().toEpochMilli());
        return dto;
    }

    private static Map<String, Object> entryDto(StateEntry e) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("key", e.key());
        dto.put("value", e.value());
        dto.put("producer", e.producer());
        dto.put("createdTurn", e.createdTurn());
        dto.put("ttlTurns", e.ttlTurns());
        dto.put("updatedAtEpochMs", e.updatedAt() == null ? null : e.updatedAt().toEpochMilli());
        return dto;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class Doc {
        public String format;
        public int version;
        public String sessionId;
        public String appId;
        public String agentName;
        public long exportedAtEpochMs;
        public List<MessageDto> messages;
        public SummaryDto summary;
        public List<StateDto> state;
        public Map<String, String> extensions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class MessageDto {
        public String id;
        public String sessionId;
        public int turnSeq;
        public int seqInTurn;
        public String role;
        public String content;
        public List<ToolCallRecord> toolCalls;
        public String toolCallId;
        public String reasoningContent;
        public String reasoningSignature;
        public Map<String, Object> metadata;
        public long createdAtEpochMs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class SummaryDto {
        public String sessionId;
        public long version;
        public Map<String, String> sections;
        public int tokenEstimate;
        public long createdAtEpochMs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class StateDto {
        public String key;
        public EntryDto entry;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class EntryDto {
        public String key;
        public String value;
        public String producer;
        public int createdTurn;
        public Integer ttlTurns;
        public Long updatedAtEpochMs;
    }
}
