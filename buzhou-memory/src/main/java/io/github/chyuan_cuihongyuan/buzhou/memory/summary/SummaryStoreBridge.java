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
    /** impl-790 / spec 1038：操作与代数回退计数（回退=旧快照覆盖异常信号）。 */
    private final java.util.concurrent.atomic.AtomicLong saves =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong loads =
            new java.util.concurrent.atomic.AtomicLong();
    private final java.util.concurrent.atomic.AtomicLong generationRegressions =
            new java.util.concurrent.atomic.AtomicLong();
    /** per-session 最近写入代数（有界 LRU 1024——TurnTimingHook 同先例）。 */
    private final java.util.LinkedHashMap<String, Long> lastGeneration =
            new java.util.LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<String, Long> eldest) {
                    return size() > 1024;
                }
            };

    public SummaryStoreBridge(SummaryStore store) {
        this.store = store;
    }

    public Optional<NineSectionSummary> loadLatest(String sessionId) {
        loads.incrementAndGet(); // spec 1038：操作计数
        return store.latest(sessionId).map(this::fromStored);
    }

    public void save(String sessionId, NineSectionSummary summary) {
        saves.incrementAndGet(); // spec 1038：保存计数
        Long lastGen = lastGeneration.get(sessionId);
        if (lastGen != null && summary.generation() < lastGen) {
            generationRegressions.incrementAndGet(); // 代数回退——旧快照覆盖异常信号
        }
        lastGeneration.put(sessionId, summary.generation());
        store.save(sessionId, new StructuredSummary(sessionId, 0, toStoredSections(summary),
                summary.render().length(), Instant.now()));
    }

    /** 代数回退累计（spec 1038 读面）。 */
    public long generationRegressions() {
        return generationRegressions.get();
    }

    /** 操作与代数回退只读快照（spec 1038）。 */
    public SummaryStoreStats stats() {
        return new SummaryStoreStats(saves.get(), loads.get(), generationRegressions.get());
    }

    /** 操作计数行（不可变）。 */
    public record SummaryStoreStats(long saves, long loads, long generationRegressions) {
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
