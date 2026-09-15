package io.github.chyuan_cuihongyuan.buzhou.memory.episodic;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryMessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.memory.tool.EvidenceLookupTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1111 / impl 863：evidence×episodic 独立性组合——回查与情景记忆
 * 交叉调用后双读面互不串账、reset 独立隔离。纯测试轮。
 */
class EvidenceEpisodicComboTest {

    private SessionStateStore stateStore;
    private MessageStore messageStore;
    private String evidenceId;

    @BeforeEach
    void setUp() {
        EvidenceLookupTool.resetForTest();
        EpisodeLedger.resetForTest();
        stateStore = new InMemorySessionStateStore();
        messageStore = new InMemoryMessageStore();
        evidenceId = UUID.randomUUID().toString();
        messageStore.append("s1", List.of(new BuzhouMessage(evidenceId, "s1", 1, 0,
                io.github.chyuan_cuihongyuan.buzhou.core.message.Role.TOOL, "证据内容",
                List.of(), "call-1", null, null, Map.of(), Instant.now())));
    }

    @Test
    void crossCallsKeepBothReadoutsIndependent() {
        EvidenceLookupTool lookup = new EvidenceLookupTool(messageStore);
        EpisodeLedger ledger = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});

        assertThat(lookup.call("{\"evidenceId\":\"" + evidenceId + "\"}")).isEqualTo("证据内容");
        ledger.record("s1", "任务", "", "success");
        ledger.recallExamples("s1", "任务", 5);

        EvidenceLookupTool.EvidenceLookupStats es = EvidenceLookupTool.stats();
        EpisodeLedger.EpisodicMemoryStats ls = EpisodeLedger.stats();

        // 互不串账：回查两次调用不计入情景侧；情景 record/recall 不影响回查侧
        assertThat(es.calls()).isEqualTo(1);
        assertThat(ls.recordCalls()).isEqualTo(1);
        assertThat(ls.recallCalls()).isEqualTo(1);
    }

    @Test
    void resetsAreIndependent() {
        EvidenceLookupTool lookup = new EvidenceLookupTool(messageStore);
        EpisodeLedger ledger = new EpisodeLedger(stateStore, text -> new float[]{1f, 0f});
        lookup.call("{\"evidenceId\":\"" + evidenceId + "\"}");
        ledger.record("s1", "任务", "", "success");

        EvidenceLookupTool.resetForTest();
        assertThat(EvidenceLookupTool.stats().calls()).isZero();
        assertThat(EpisodeLedger.stats().recordCalls()).isEqualTo(1);

        EpisodeLedger.resetForTest();
        assertThat(EpisodeLedger.stats().recordCalls()).isZero();
    }
}
