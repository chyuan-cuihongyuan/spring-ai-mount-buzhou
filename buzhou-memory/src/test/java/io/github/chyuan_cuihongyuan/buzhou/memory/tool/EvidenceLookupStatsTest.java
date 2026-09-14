package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryMessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1073 / impl 825：evidence_lookup 证据回查读面——命中全文/切片、
 * 未找到、双守恒恒等式、resetForTest 归零。
 */
class EvidenceLookupStatsTest {

    private MessageStore store;
    private EvidenceLookupTool tool;
    private String evidenceId;

    @BeforeEach
    void setUp() {
        EvidenceLookupTool.resetForTest();
        store = new InMemoryMessageStore();
        tool = new EvidenceLookupTool(store);
        evidenceId = UUID.randomUUID().toString();
        store.append("s1", List.of(new BuzhouMessage(evidenceId, "s1", 1, 0, Role.TOOL,
                "0123456789abcdef", List.of(), "call-1", null, null, Map.of(), Instant.now())));
    }

    @Test
    void fullReadCountsComplete() {
        String out = tool.call("{\"evidenceId\":\"" + evidenceId + "\"}");
        assertThat(out).isEqualTo("0123456789abcdef");

        EvidenceLookupTool.EvidenceLookupStats stats = EvidenceLookupTool.stats();
        assertThat(stats.calls()).isEqualTo(1);
        assertThat(stats.hits()).isEqualTo(1);
        assertThat(stats.completeReads()).isEqualTo(1);
        assertThat(stats.slicedReads()).isZero();
    }

    @Test
    void slicedReadCountsItsBucket() {
        String out = tool.call("{\"evidenceId\":\"" + evidenceId
                + "\",\"offset\":0,\"limit\":4}");
        assertThat(out).contains("0123").contains("已截断");

        EvidenceLookupTool.EvidenceLookupStats stats = EvidenceLookupTool.stats();
        assertThat(stats.slicedReads()).isEqualTo(1);
        assertThat(stats.completeReads()).isZero();
    }

    @Test
    void unknownIdCountsMiss() {
        String out = tool.call("{\"evidenceId\":\"ghost\"}");
        assertThat(out).contains("未找到");

        assertThat(EvidenceLookupTool.stats().misses()).isEqualTo(1);
        assertThat(EvidenceLookupTool.stats().hits()).isZero();
    }

    @Test
    void dualConservationIdentitiesHold() {
        tool.call("{\"evidenceId\":\"" + evidenceId + "\"}");            // complete
        tool.call("{\"evidenceId\":\"" + evidenceId + "\",\"limit\":2}"); // sliced
        tool.call("{\"evidenceId\":\"ghost\"}");                          // miss

        EvidenceLookupTool.EvidenceLookupStats stats = EvidenceLookupTool.stats();
        assertThat(stats.calls()).isEqualTo(3);
        assertThat(stats.calls()).isEqualTo(stats.hits() + stats.misses());
        assertThat(stats.hits()).isEqualTo(stats.completeReads() + stats.slicedReads());
    }

    @Test
    void resetForTestZeroesCounters() {
        tool.call("{\"evidenceId\":\"" + evidenceId + "\"}");
        assertThat(EvidenceLookupTool.stats().calls()).isEqualTo(1);

        EvidenceLookupTool.resetForTest();

        EvidenceLookupTool.EvidenceLookupStats stats = EvidenceLookupTool.stats();
        assertThat(stats.calls()).isZero();
        assertThat(stats.hits()).isZero();
        assertThat(stats.misses()).isZero();
    }
}
