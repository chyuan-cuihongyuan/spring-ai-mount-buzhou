package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.HarnessToolCallingManager;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.Spotlighting;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SectionContent;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummarySection;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.SummaryStoreBridge;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Instant;
import java.util.EnumMap;
import java.util.UUID;

/**
 * {@code revise_summary_section} 自愈记忆工具（wayfinder2 impl-12 / T38 / docs/spec/12）：
 * 模型可修正自己九段式摘要中的段落错误（memory-as-tools）——<b>精确匹配 + 唯一性检查 +
 * P0 只读锁</b>（Letta {@code core_memory_replace} 的防静默覆写机制），
 * 并带 <b>provenance + taint 防投毒</b>：
 *
 * <ul>
 *   <li>类型化错误 {@code EDIT_NOT_FOUND}（未命中）/ {@code EDIT_AMBIGUOUS}（多处命中）/
 *       {@code EDIT_LOCKED}（P0 段只读）；</li>
 *   <li><b>taint 门</b>：newText 携带 spotlighting 不可信标记（外部数据包裹符 / 交织标记字符 /
 *       canary 密语）即拒绝（{@code REJECTED_UNTRUSTED}）——untrusted 内容未经脱敏不得进摘要正文
 *       （堵 Unit 42「工具输出投毒进持久记忆」攻击面，超越其公开缓解建议）；</li>
 *   <li><b>全量审计</b>：每次修订（含被拒尝试）写会话 state 台账（provenance：来源工具、段落、
 *       旧/新内容指纹、taint 判定、裁决）。</li>
 * </ul>
 */
public class ReviseSummarySectionTool implements ToolCallback {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SummaryStoreBridge bridge;
    private final SessionStateStore sessionStateStore;

    public ReviseSummarySectionTool(SummaryStoreBridge bridge, SessionStateStore sessionStateStore) {
        this.bridge = bridge;
        this.sessionStateStore = sessionStateStore;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return ToolDefinition.builder()
                .name("revise_summary_section")
                .description("修订自己结构化摘要的指定段落（自愈压缩错误）：oldText 必须与段内现有文本"
                        + "精确匹配且唯一；P0 段（USER_INTENT/CURRENT_STATE/NEXT_STEP）只读。")
                .inputSchema("""
                        {"type":"object","properties":{
                          "sectionId":{"type":"string","enum":["USER_INTENT","CURRENT_STATE","NEXT_STEP","PENDING_TASKS","ERRORS_FIXES","KEY_ARTIFACTS","PROBLEM_SOLVING","TECHNICAL_CONCEPTS","USER_MESSAGES_LOG"]},
                          "oldText":{"type":"string","description":"段内要替换的现有文本（精确匹配）"},
                          "newText":{"type":"string","description":"替换后的文本"}
                        },"required":["sectionId","oldText","newText"]}
                        """)
                .build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String sessionId = HarnessToolCallingManager.sessionIdOf(toolContext);
        if (sessionId == null) {
            return "[修订失败] 缺少会话上下文（sessionId）";
        }
        try {
            JsonNode args = MAPPER.readTree(toolInput);
            String sectionId = args.path("sectionId").asText();
            String oldText = args.path("oldText").asText();
            String newText = args.path("newText").asText();
            return revise(sessionId, sectionId, oldText, newText);
        } catch (Exception e) {
            return "[修订失败] 入参解析错误：" + e.getMessage();
        }
    }

    private String revise(String sessionId, String sectionId, String oldText, String newText) {
        var latest = bridge.loadLatest(sessionId);
        if (latest.isEmpty()) {
            return "[修订未执行] EDIT_NO_SUMMARY：本会话尚无结构化摘要可修订。";
        }
        SummarySection section;
        try {
            section = SummarySection.valueOf(sectionId);
        } catch (IllegalArgumentException e) {
            return "[修订未执行] EDIT_UNKNOWN_SECTION：未知段落「" + sectionId + "」";
        }
        if (section.priority() == 0) {
            audit(sessionId, section, oldText, newText, false, "EDIT_LOCKED");
            return "[修订被拒] EDIT_LOCKED：段落 " + sectionId + " 为 P0 死保段（只读），不允许工具修订。";
        }
        // taint 门：newText 携带不可信标记 → 拒绝（防「工具输出原文」直写入持久摘要）
        if (isTainted(newText)) {
            audit(sessionId, section, oldText, newText, true, "REJECTED_UNTRUSTED");
            return "[修订被拒] REJECTED_UNTRUSTED：newText 含不可信数据标记"
                    + "（外部数据包裹符 / 交织标记 / canary 密语）——请先脱敏提炼为可信事实再写入。";
        }
        NineSectionSummary summary = latest.get();
        SectionContent content = summary.sections().get(section);
        String body = content == null ? "" : content.body();
        if (oldText == null || oldText.isBlank() || !body.contains(oldText)) {
            audit(sessionId, section, oldText, newText, false, "EDIT_NOT_FOUND");
            return "[修订未执行] EDIT_NOT_FOUND：oldText 未在段落 " + sectionId + " 中精确命中。";
        }
        if (countOccurrences(body, oldText) > 1) {
            audit(sessionId, section, oldText, newText, false, "EDIT_AMBIGUOUS");
            return "[修订未执行] EDIT_AMBIGUOUS：oldText 在段落中命中多处——请补充上下文使其唯一。";
        }

        EnumMap<SummarySection, SectionContent> updated = new EnumMap<>(summary.sections());
        updated.put(section, new SectionContent(body.replace(oldText, newText),
                content.form(), content.evidenceIds()));
        long oldGeneration = summary.generation();
        bridge.save(sessionId, summary.withSections(updated, summary.coversUpToTurn()));
        audit(sessionId, section, oldText, newText, false, "APPLIED");
        return "[摘要已修订] 段落 " + sectionId + " 已更新（generation " + oldGeneration
                + "→" + (oldGeneration + 1) + "）；审计已记录。";
    }

    /** taint 判定：spotlighting 包裹符 / 交织标记字符 / canary 前缀。 */
    static boolean isTainted(String text) {
        if (text == null) {
            return false;
        }
        return text.contains(Spotlighting.BEGIN_HEAD)
                || text.contains(Spotlighting.BANNER)
                || text.indexOf(Spotlighting.DEFAULT_MARK_CHAR) >= 0
                || text.contains("CANARY-");
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    /** 修订审计（含被拒尝试）：provenance + taint 判定 + 裁决，全会话 state 台账。 */
    private void audit(String sessionId, SummarySection section, String oldText, String newText,
                       boolean tainted, String decision) {
        if (sessionStateStore == null) {
            return;
        }
        try {
            String payload = MAPPER.writeValueAsString(java.util.Map.of(
                    "tool", "revise_summary_section",
                    "section", section.name(),
                    "oldFingerprint", Integer.toHexString(oldText == null ? 0 : oldText.hashCode()),
                    "newFingerprint", Integer.toHexString(newText == null ? 0 : newText.hashCode()),
                    "tainted", tainted,
                    "decision", decision,
                    "occurredAt", Instant.now().toString()));
            sessionStateStore.put(sessionId, new StateEntry(
                    "memory.revise." + section.name() + "." + UUID.randomUUID(),
                    payload, "revise_summary_section", 0, null, Instant.now()));
        } catch (Exception ignored) {
            // 审计写入失败不阻断主裁决（尽力而为；桥接层有各自的错误语义）
        }
    }
}
