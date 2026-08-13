package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 九段式结构化摘要（P0–P3 段内分级）。
 *
 * @param generation           摘要代际（每次合并 +1）
 * @param coversUpToTurn       已覆盖到的轮次水位（turnSeq <= 此值的消息已折入摘要）
 * @param sections             九段内容（段名 → 内容）
 * @param summarizedMessageIds 已折入摘要的消息 id 集合（T24 增量摘要：消息级水位，
 *                             与 {@code coversUpToTurn} 轮次水位互补——再次压缩只折入
 *                             <b>新消息</b>，避免全量重摘要的漂移累积与重复成本；
 *                             来源 LangMem RunningSummary）
 */
public record NineSectionSummary(long generation, int coversUpToTurn,
                                 EnumMap<SummarySection, SectionContent> sections,
                                 List<String> summarizedMessageIds) {

    public NineSectionSummary {
        sections = sections == null ? new EnumMap<>(SummarySection.class) : sections;
        summarizedMessageIds = summarizedMessageIds == null ? List.of() : List.copyOf(summarizedMessageIds);
    }

    /** 兼容旧形状（无消息 id 水位）。 */
    public NineSectionSummary(long generation, int coversUpToTurn,
                              EnumMap<SummarySection, SectionContent> sections) {
        this(generation, coversUpToTurn, sections, List.of());
    }

    public static NineSectionSummary empty() {
        return new NineSectionSummary(0, 0, new EnumMap<>(SummarySection.class), List.of());
    }

    public NineSectionSummary withSections(EnumMap<SummarySection, SectionContent> newSections,
                                           int newCoversUpToTurn) {
        // 消息 id 水位随代际携带（T24）；需要显式更新时用 withSummarizedMessageIds
        return new NineSectionSummary(generation + 1, newCoversUpToTurn, newSections,
                summarizedMessageIds);
    }

    /** 更新已摘要消息 id 集合（不 bump 代际）。 */
    public NineSectionSummary withSummarizedMessageIds(List<String> ids) {
        return new NineSectionSummary(generation, coversUpToTurn, sections, ids);
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<SummarySection, SectionContent> entry : sections.entrySet()) {
            if (entry.getValue() == null || entry.getValue().body().isBlank()) {
                continue;
            }
            sb.append("## ").append(entry.getKey().name())
                    .append("（").append(entry.getKey().displayName()).append("）\n")
                    .append(entry.getValue().render()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 把文本追加到 CURRENT_STATE 段（保证事实经摘要 P0 段保留，压缩不丢现场）。
     * 若 CURRENT_STATE 段不存在则新建。返回新实例（不可变）。
     */
    public NineSectionSummary appendCurrentState(String text) {
        EnumMap<SummarySection, SectionContent> copy = new EnumMap<>(sections);
        SectionContent existing = copy.get(SummarySection.CURRENT_STATE);
        String newBody = text == null || text.isBlank() ? ""
                : (existing == null || existing.body() == null || existing.body().isBlank()
                        ? text : existing.body() + "\n" + text);
        SectionContent form = existing == null ? SectionContent.full(newBody)
                : new SectionContent(newBody, existing.form(), existing.evidenceIds());
        copy.put(SummarySection.CURRENT_STATE, form);
        return new NineSectionSummary(generation, coversUpToTurn, copy, summarizedMessageIds);
    }

    /** 合并两组已摘要消息 id（去重、保序）。 */
    public static List<String> unionIds(List<String> existing, List<String> added) {
        List<String> result = new ArrayList<>(existing);
        for (String id : added) {
            if (id != null && !result.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }
}
