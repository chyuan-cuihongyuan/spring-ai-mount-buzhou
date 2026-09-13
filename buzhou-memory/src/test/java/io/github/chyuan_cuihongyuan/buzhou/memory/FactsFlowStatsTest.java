package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FactsFlowStatsTest {

    private InMemorySessionStateStore stateStore = new InMemorySessionStateStore();
    private FactsExporter exporter = new FactsExporter(stateStore);

    private void putFact(String sessionId, String name, String value) {
        stateStore.put(sessionId, new StateEntry("fact." + name, value,
                "test", 1, null, Instant.now()));
    }

    @Test
    void exportCountsFactsExported() {
        putFact("s1", "a", "1");
        putFact("s1", "b", "2");

        String json = exporter.exportSegment("s1");

        assertThat(json).isNotNull();
        assertThat(exporter.stats().factsExported()).isEqualTo(2);
    }

    @Test
    void emptySessionExportCountsNothing() {
        exporter.exportSegment("empty");

        assertThat(exporter.stats()).isEqualTo(new FactsExporter.FactsFlowStats(0, 0, 0));
    }

    @Test
    void importCountsRowsWritten() {
        exporter.importSegment("s1", "[{\"key\":\"fact.a\",\"value\":\"1\"},"
                + "{\"key\":\"fact.b\",\"value\":\"2\"}]");

        assertThat(exporter.stats().factsImported()).isEqualTo(2);
        assertThat(stateStore.get("s1", "fact.a").isPresent()).isTrue();
        assertThat(stateStore.get("s1", "fact.b").isPresent()).isTrue();
    }

    @Test
    void malformedImportCountsFailureAndStillThrows() {
        assertThatThrownBy(() -> exporter.importSegment("s1", "not-json"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(exporter.stats().importFailures()).isEqualTo(1);
        assertThat(exporter.stats().factsImported()).isZero();
    }
}
