package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionExport;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 会话导出脱敏器（spec 518 / T787——28 导出面 × 86 检测器组合；Presidio
 * anonymize 同源）：消息正文/推理内容/摘要 sections/state 值四内容域
 * 占位符化（`[PII:TYPE]` + customRules 叠加），结构字段原样——不可变
 * 副本（原导出零改动，可继续走 510 密文封缄或直接使用）。
 *
 * <p>诚实边界：toolCalls 参数 JSON 与 metadata 值不脱敏（结构化参数域/
 * 框架元数据非内容域）；与 510 组合 = 先脱敏再封缄（对外分享全链）。
 */
public final class SessionExportSanitizer {

    private final PiiDetector detector;
    private final Set<PiiType> enabledTypes;
    private final CustomPiiRules customRules;

    public SessionExportSanitizer() {
        this(null, null);
    }

    public SessionExportSanitizer(Set<PiiType> types, CustomPiiRules customRules) {
        this.detector = new PiiDetector();
        this.enabledTypes = types == null || types.isEmpty()
                ? EnumSet.allOf(PiiType.class) : EnumSet.copyOf(types);
        this.customRules = customRules == null ? new CustomPiiRules(List.of()) : customRules;
    }

    /** 脱敏副本（原导出零改动）。 */
    public SessionExport sanitize(SessionExport export) {
        if (export == null) {
            throw new IllegalArgumentException("SessionExport 必须非空");
        }
        List<BuzhouMessage> messages = export.messages().stream()
                .map(this::sanitizeMessage).toList();
        StructuredSummary summary = export.summary() == null
                ? null : sanitizeSummary(export.summary());
        Map<String, StateEntry> states = new LinkedHashMap<>();
        export.state().forEach((key, entry) -> states.put(key,
                new StateEntry(entry.key(), redact(entry.value()), entry.producer(),
                        entry.createdTurn(), entry.ttlTurns(), entry.updatedAt())));
        return new SessionExport(export.format(), export.version(), export.sessionId(),
                export.appId(), export.agentName(), export.exportedAtEpochMs(),
                messages, summary, states, export.extensions());
    }

    private BuzhouMessage sanitizeMessage(BuzhouMessage message) {
        return new BuzhouMessage(message.id(), message.sessionId(), message.turnSeq(),
                message.seqInTurn(), message.role(), redact(message.content()),
                message.toolCalls(), message.toolCallId(), redact(message.reasoningContent()),
                message.reasoningSignature(), message.metadata(), message.createdAt());
    }

    private StructuredSummary sanitizeSummary(StructuredSummary summary) {
        Map<String, String> sections = new LinkedHashMap<>();
        summary.sections().forEach((name, text) -> sections.put(name, redact(text)));
        return new StructuredSummary(summary.sessionId(), summary.version(), sections,
                summary.tokenEstimate(), summary.createdAt());
    }

    private String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String redacted = detector.redact(text, enabledTypes);
        if (!customRules.isEmpty()) {
            redacted = customRules.redact(redacted);
        }
        return redacted;
    }
}
