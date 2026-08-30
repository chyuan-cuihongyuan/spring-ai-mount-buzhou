package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.memory.compact.MicroCompactionResult;
import io.github.chyuan_cuihongyuan.buzhou.memory.summary.NineSectionSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 171 / T538：摘要溯源回归——多轮微压缩合并落一代 / 两代序与 sourcesOf /
 * 无折入不落账 / LRU 有界。
 */
class SummaryProvenanceListenerTest {

    private static MicroCompactionResult folded(List<String> ids) {
        return new MicroCompactionResult(List.of(), ids, 100);
    }

    private static NineSectionSummary summaryAt(long generation) {
        return new NineSectionSummary(generation, (int) generation * 10, null);
    }

    @Test
    void multipleCompactionsMergeIntoOneGeneration() {
        SummaryProvenanceListener provenance = new SummaryProvenanceListener();
        provenance.onCompacted("s1", folded(List.of("m1", "m2")), 0.8);
        provenance.onCompacted("s1", folded(List.of("m3")), 0.8);
        provenance.onSummaryFolded("s1", summaryAt(3), "budget");

        List<SummaryProvenanceListener.Entry> lineage = provenance.lineage("s1");
        assertThat(lineage).hasSize(1);
        assertThat(lineage.getFirst().generation()).isEqualTo(3);
        assertThat(lineage.getFirst().trigger()).isEqualTo("budget");
        assertThat(lineage.getFirst().sourceMessageIds())
                .containsExactly("m1", "m2", "m3");
        assertThat(lineage.getFirst().foldedCount()).isEqualTo(3);
    }

    @Test
    void twoGenerationsKeepOrderAndSourcesOfPrecise() {
        SummaryProvenanceListener provenance = new SummaryProvenanceListener();
        provenance.onCompacted("s1", folded(List.of("a1")), 0.8);
        provenance.onSummaryFolded("s1", summaryAt(1), "backlog");
        provenance.onCompacted("s1", folded(List.of("b1", "b2")), 0.9);
        provenance.onSummaryFolded("s1", summaryAt(2), "drift");

        List<SummaryProvenanceListener.Entry> lineage = provenance.lineage("s1");
        assertThat(lineage).extracting(SummaryProvenanceListener.Entry::generation)
                .containsExactly(1L, 2L);
        assertThat(provenance.sourcesOf("s1", 1)).containsExactly("a1");
        assertThat(provenance.sourcesOf("s1", 2)).containsExactly("b1", "b2");
        assertThat(provenance.sourcesOf("s1", 99)).isEmpty();
    }

    @Test
    void compactionWithoutFoldStaysPendingNotEntry() {
        SummaryProvenanceListener provenance = new SummaryProvenanceListener();
        provenance.onCompacted("s1", folded(List.of("x1")), 0.8);
        assertThat(provenance.lineage("s1")).isEmpty(); // 未折入不落账

        provenance.onSummaryFolded("s1", summaryAt(1), "budget"); // 折入时兑现
        assertThat(provenance.sourcesOf("s1", 1)).containsExactly("x1");
    }

    @Test
    void foldWithoutPendingRecordsEmptySources() {
        SummaryProvenanceListener provenance = new SummaryProvenanceListener();
        provenance.onSummaryFolded("s1", summaryAt(5), "drift");
        // 直接摘要折入（无微压缩前置）——空源集诚实入账（零源不是无记录）
        assertThat(provenance.lineage("s1")).hasSize(1);
        assertThat(provenance.lineage("s1").getFirst().sourceMessageIds()).isEmpty();
    }

    @Test
    void boundedSessionsAndEntries() {
        SummaryProvenanceListener provenance = new SummaryProvenanceListener();
        for (int i = 0; i < 66; i++) {
            provenance.onSummaryFolded("s" + i, summaryAt(1), "budget");
        }
        assertThat(provenance.trackedSessions()).isEqualTo(64); // LRU 64 会话
        assertThat(provenance.lineage("s0")).isEmpty();      // 最久未活跃滑出
        assertThat(provenance.lineage("s65")).hasSize(1);

        for (long gen = 1; gen <= 34; gen++) {
            provenance.onSummaryFolded("s65", summaryAt(gen), "budget");
        }
        assertThat(provenance.lineage("s65")).hasSize(32); // 每会话 32 条窗
        assertThat(provenance.lineage("s65").getFirst().generation()).isEqualTo(3L); // 最早滑出
    }
}
