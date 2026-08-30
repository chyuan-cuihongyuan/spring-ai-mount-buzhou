package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 201 / T574：目录漂移看门狗回归——首拍建基线 / 增删改事件带三分类 /
 * 无变静默 / 再变再发基线推进。
 */
class CatalogDriftWatcherTest {

    private static ToolDefinition tool(String name, String schema) {
        return ToolDefinition.builder().name(name).description("d").inputSchema(schema).build();
    }

    @Test
    void firstCheckEstablishesBaselineWithoutEvent() {
        List<Map<String, Object>> events = new CopyOnWriteArrayList<>();
        CatalogDriftWatcher watcher = new CatalogDriftWatcher(events::add);

        assertThat(watcher.check(List.of(tool("a", "{}")))).isEmpty(); // 首拍建基线
        assertThat(events).isEmpty();
        assertThat(watcher.baselineSummary()).isNotBlank();
    }

    @Test
    void additionRemovalChangeEachEmitEventWithDiff() {
        List<Map<String, Object>> events = new CopyOnWriteArrayList<>();
        CatalogDriftWatcher watcher = new CatalogDriftWatcher(events::add);
        watcher.check(List.of(tool("keep", "{}"), tool("gone", "{}"),
                tool("mutated", "{\"v\":1}")));

        watcher.check(List.of(tool("keep", "{}"), tool("mutated", "{\"v\":2}"),
                tool("fresh", "{}")));

        assertThat(events).hasSize(1);
        Map<String, Object> payload = events.getFirst();
        assertThat(payload.get("added")).isEqualTo(List.of("fresh"));
        assertThat(payload.get("removed")).isEqualTo(List.of("gone"));
        assertThat(payload.get("changed")).isEqualTo(List.of("mutated"));
        assertThat((String) payload.get("oldSummary")).isNotBlank();
        assertThat((String) payload.get("newSummary")).isNotBlank();
    }

    @Test
    void unchangedCatalogStaysSilent() {
        List<Map<String, Object>> events = new CopyOnWriteArrayList<>();
        CatalogDriftWatcher watcher = new CatalogDriftWatcher(events::add);
        List<ToolDefinition> catalog = List.of(tool("a", "{}"), tool("b", "{\"x\":1}"));

        watcher.check(catalog);
        assertThat(watcher.check(catalog)).isEmpty();
        assertThat(watcher.check(catalog)).isEmpty();
        assertThat(events).isEmpty(); // 无变静默
    }

    @Test
    void consecutiveDriftsEachEmitBaselineAdvances() {
        List<Map<String, Object>> events = new CopyOnWriteArrayList<>();
        CatalogDriftWatcher watcher = new CatalogDriftWatcher(events::add);
        watcher.check(List.of(tool("a", "{}")));

        watcher.check(List.of(tool("a", "{}"), tool("b", "{}")));   // +b
        watcher.check(List.of(tool("a", "{}"), tool("b", "{}")));   // 无变
        watcher.check(List.of(tool("a", "{\"v\":2}"), tool("b", "{}"))); // a 改

        assertThat(events).hasSize(2); // 只报两次真变化
        assertThat(events.get(1).get("changed")).isEqualTo(List.of("a"));
    }

    @Test
    void emptyCatalogTransitionsAreDrifts() {
        List<Map<String, Object>> events = new CopyOnWriteArrayList<>();
        CatalogDriftWatcher watcher = new CatalogDriftWatcher(events::add);
        watcher.check(List.of(tool("a", "{}")));

        assertThat(watcher.check(List.of())).isPresent(); // 全删也是漂移
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().get("removed")).isEqualTo(List.of("a"));
    }
}
