package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Mem0 式事实对账（wayfinder T25 / docs/spec/11 memory）：九段摘要生成后，对<b>既有段</b>跑一次
 * 对账 pass——旧段正文与新段正文交由模型按四态裁决：
 * {@code ADD}（新增无语义等价）/ {@code UPDATE}（并入互补信息）/ {@code DELETE}（被新信息证伪）/
 * {@code NOOP}（无变化），并输出<b>对账后的段正文</b>（去重、矛盾以新证伪旧、未变事实保留）。
 *
 * <p>防重复/矛盾/陈旧累积；Tier-1 的「语义近邻」由模型裁决承担（向量 recall 为 Tier-2）。
 * <b>韧性</b>：任何解析失败一律 NOOP（正文保持合并结果、不落盘半成品），绝不抛出中断压缩链。
 * 四态裁决经 {@code memory.fact.reconciled} 事件可观测（section + 四态计数 + 是否应用）。
 */
public class SummaryFactReconciler {

    public static final String EVENT_RECONCILED = "memory.fact.reconciled";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final System.Logger LOG = System.getLogger(SummaryFactReconciler.class.getName());
    private static final ReconcileOutcome NOOP_OUTCOME =
            new ReconcileOutcome(0, 0, 0, 0, "", false);

    /**
     * 对 previous 与 merged 都非空的段跑对账；返回应用了对账正文的摘要（代际不变——
     * 对账是合并后的净化，不是新一轮压缩）。biTemporal 非空时，被取代的段正文录入双时序台账。
     */
    public NineSectionSummary reconcile(String sessionId, NineSectionSummary previous,
                                        NineSectionSummary merged, ChatModel model,
                                        Consumer<SessionEvent> eventSink,
                                        BiTemporalFactLedger biTemporal) {
        if (previous == null || merged == null || model == null) {
            return merged;
        }
        EnumMap<SummarySection, SectionContent> reconciled = new EnumMap<>(merged.sections());
        boolean changed = false;
        for (Map.Entry<SummarySection, SectionContent> entry : merged.sections().entrySet()) {
            SectionContent old = previous.sections().get(entry.getKey());
            SectionContent current = entry.getValue();
            if (old == null || old.body() == null || old.body().isBlank()
                    || current == null || current.body() == null || current.body().isBlank()) {
                continue; // 新段（全 ADD，无需对账）
            }
            ReconcileOutcome outcome = reconcileSection(model, sessionId, entry.getKey(),
                    old.body(), current.body());
            emit(eventSink, sessionId, entry.getKey(), outcome);
            if (outcome.applied() && !outcome.body().equals(current.body())) {
                if (biTemporal != null) {
                    biTemporal.recordSuperseded(sessionId, entry.getKey().name(),
                            old.body(), previous.generation(), merged.generation());
                }
                reconciled.put(entry.getKey(), new SectionContent(outcome.body(),
                        current.form(), current.evidenceIds()));
                changed = true;
            }
        }
        if (!changed) {
            return merged;
        }
        return new NineSectionSummary(merged.generation(), merged.coversUpToTurn(), reconciled,
                merged.summarizedMessageIds());
    }

    private ReconcileOutcome reconcileSection(ChatModel model, String sessionId,
                                              SummarySection section, String oldBody, String newBody) {
        try {
            String prompt = buildPrompt(section, oldBody, newBody);
            ChatResponse response = model.call(new Prompt(prompt));
            String text = response.getResults().isEmpty() ? ""
                    : response.getResults().get(0).getOutput().getText();
            return parse(text);
        } catch (RuntimeException e) {
            return NOOP_OUTCOME; // 韧性：失败 NOOP
        }
    }

    private String buildPrompt(SummarySection section, String oldBody, String newBody) {
        return "你是会话记忆的事实对账器。对下述「旧摘要段」与「新合并段」中的每条事实做四态裁决：\n"
                + "ADD=新增（旧段无语义等价）；UPDATE=更新（并入互补信息）；DELETE=删除（被新信息证伪）；NOOP=不变。\n"
                + "段名：" + section.name() + "\n"
                + "旧摘要段：\n" + truncate(oldBody) + "\n"
                + "新合并段：\n" + truncate(newBody) + "\n"
                + "先输出 <analysis> 简要比对，再输出一行 JSON（不要其他内容）：\n"
                + "{\"add\":数量,\"update\":数量,\"delete\":数量,\"noop\":数量,"
                + "\"body\":\"对账后的段正文（去重；矛盾以新证伪旧；保留未变事实）\"}";
    }

    private ReconcileOutcome parse(String text) {
        if (text == null || text.isBlank()) {
            return NOOP_OUTCOME;
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return NOOP_OUTCOME;
        }
        try {
            JsonNode node = MAPPER.readTree(text.substring(start, end + 1));
            JsonNode body = node.get("body");
            if (body == null || !body.isTextual() || body.asText().isBlank()) {
                return NOOP_OUTCOME;
            }
            return new ReconcileOutcome(
                    intOf(node, "add"), intOf(node, "update"), intOf(node, "delete"),
                    intOf(node, "noop"), body.asText(), true);
        } catch (Exception e) {
            return NOOP_OUTCOME;
        }
    }

    private int intOf(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.canConvertToInt() ? value.asInt() : 0;
    }

    private String truncate(String body) {
        return body.length() > 4000 ? body.substring(0, 4000) + "…" : body;
    }

    private void emit(Consumer<SessionEvent> sink, String sessionId, SummarySection section,
                      ReconcileOutcome outcome) {
        if (sink == null) {
            LOG.log(System.Logger.Level.DEBUG,
                    "memory.fact.reconciled section=" + section + " add=" + outcome.add()
                            + " update=" + outcome.update() + " delete=" + outcome.delete()
                            + " noop=" + outcome.noop() + " applied=" + outcome.applied());
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("section", section.name());
        payload.put("add", outcome.add());
        payload.put("update", outcome.update());
        payload.put("delete", outcome.delete());
        payload.put("noop", outcome.noop());
        payload.put("applied", outcome.applied());
        sink.accept(new SessionEvent(EVENT_RECONCILED, payload, Instant.now()));
    }

    /** 单段对账结果（四态计数 + 对账后正文 + 是否成功应用）。 */
    public record ReconcileOutcome(int add, int update, int delete, int noop,
                                   String body, boolean applied) {
    }

}
