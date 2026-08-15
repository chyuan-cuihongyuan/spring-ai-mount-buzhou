package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.DefaultFactStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Fact;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.FactStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * facts 导出扩展测试（spec 36 §A / T121 / impl-96）：fact.* 命名空间段导出/导入
 * 无损往返（与 DefaultFactStore 读写互通）。
 */
class FactsExporterTest {

    /** facts 经扩展段往返：FactStore 写入 → 导出 → 导入（新 Id）→ FactStore 可读。 */
    @Test
    void factsRoundTripThroughExtensionSegment() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        FactStore factStore = new DefaultFactStore(stores.sessionStateStore());
        factStore.save("src-1", new Fact("fact.risk.table-1",
                java.util.Map.of("risk", "high"), "risk-analysis", 3, 100));
        // 非 fact 键不进段
        stores.sessionStateStore().put("src-1", new StateEntry(
                "budget.used", "999", "core", 1, null, Instant.now()));

        FactsExporter exporter = new FactsExporter(stores.sessionStateStore());
        String segment = exporter.exportSegment("src-1");

        assertThat(segment).contains("fact.risk.table-1").contains("risk-analysis")
                .doesNotContain("budget.used");

        // 导入到新会话：FactStore 同口径可读（键/producer/turn/ttl 无损）
        exporter.importSegment("dst-1", segment);
        assertThat(factStore.activeFacts("dst-1", 10))
                .singleElement()
                .satisfies(f -> {
                    assertThat(f.key()).isEqualTo("fact.risk.table-1");
                    assertThat(f.producer()).isEqualTo("risk-analysis");
                    assertThat(f.createdTurn()).isEqualTo(3);
                    assertThat(f.ttl()).isEqualTo(100);
                });

        // 空会话导出 null（不携带空段）
        assertThat(exporter.exportSegment("empty-session")).isNull();
    }
}
