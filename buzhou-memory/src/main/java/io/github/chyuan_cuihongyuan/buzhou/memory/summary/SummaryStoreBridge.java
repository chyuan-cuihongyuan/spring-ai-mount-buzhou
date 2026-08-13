package io.github.chyuan_cuihongyuan.buzhou.memory.summary;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SummaryStore;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SummaryStoreBridge {

    private final SummaryStore store;

    public SummaryStoreBridge(SummaryStore store) {
        this.store = store;
    }

    public Optional<NineSectionSummary> loadLatest(String sessionId) {
        return store.latest(sessionId).map(this::fromStored);
    }

    public void save(String sessionId, NineSectionSummary summary) {
        store.save(sessionId, new StructuredSummary(sessionId, 0, toStoredSections(summary),
                summary.render().length(), Instant.now()));
    }

    private Map<String, String> toStoredSections(NineSectionSummary summary) {
        Map<String, String> map = new HashMap<>();
        summary.sections().forEach((section, content) ->
                map.put(section.name() + "|" + content.form(), content.render()));
        map.put("__generation", String.valueOf(summary.generation()));
        map.put("__coversUpToTurn", String.valueOf(summary.coversUpToTurn()));
        // T24 增量摘要：消息级水位（逗号连接；截最近 MAX_PERSISTED_IDS 条防膨胀）
        if (!summary.summarizedMessageIds().isEmpty()) {
            List<String> ids = summary.summarizedMessageIds();
            List<String> capped = ids.size() > MAX_PERSISTED_IDS
                    ? ids.subList(ids.size() - MAX_PERSISTED_IDS, ids.size()) : ids;
            map.put("__summarizedMessageIds", String.join(",", capped));
        }
        return map;
    }

    private NineSectionSummary fromStored(StructuredSummary stored) {
        EnumMap<SummarySection, SectionContent> sections = new EnumMap<>(SummarySection.class);
        long generation = 0;
        int covers = 0;
        List<String> summarizedIds = new java.util.ArrayList<>();
        for (Map.Entry<String, String> entry : stored.sections().entrySet()) {
            String key = entry.getKey();
            if (key.equals("__generation")) {
                generation = Long.parseLong(entry.getValue());
                continue;
            }
            if (key.equals("__coversUpToTurn")) {
                covers = Integer.parseInt(entry.getValue());
                continue;
            }
            if (key.equals("__summarizedMessageIds")) {
                for (String id : entry.getValue().split(",")) {
                    if (!id.isBlank()) {
                        summarizedIds.add(id.trim());
                    }
                }
                continue;
            }
            int pipe = key.lastIndexOf('|');
            String name = pipe < 0 ? key : key.substring(0, pipe);
            String form = pipe < 0 ? "FULL" : key.substring(pipe + 1);
            try {
                sections.put(SummarySection.valueOf(name),
                        new SectionContent(entry.getValue(),
                                SectionContent.Form.valueOf(form), java.util.List.of()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return new NineSectionSummary(generation, covers, sections, summarizedIds);
    }

    /** 持久化的消息 id 水位上限（防无限膨胀；更早的由轮次水位兜底）。 */
    static final int MAX_PERSISTED_IDS = 400;
}
