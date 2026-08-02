package io.github.chyuan_cuihongyuan.buzhou.memory.tool;

import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryMessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceLookupToolTest {

    @Test
    void fetchesOriginalByEvidenceId() {
        InMemoryMessageStore store = new InMemoryMessageStore();
        BuzhouMessage original = new BuzhouMessage("ev-1", "s", 1, 2, Role.TOOL,
                "x".repeat(500), List.of(), "tc-1", null, null, Map.of(), Instant.now());
        store.append("s", List.of(original));

        EvidenceLookupTool tool = new EvidenceLookupTool(store);
        String result = tool.call("{\"evidenceId\":\"ev-1\"}");
        assertThat(result).isEqualTo("x".repeat(500));
    }

    @Test
    void rangeReadSlicesWithTruncationNotice() {
        InMemoryMessageStore store = new InMemoryMessageStore();
        store.append("s", List.of(new BuzhouMessage("ev-2", "s", 1, 2, Role.TOOL,
                "0123456789", List.of(), "tc-1", null, null, Map.of(), Instant.now())));

        EvidenceLookupTool tool = new EvidenceLookupTool(store);
        String result = tool.call("{\"evidenceId\":\"ev-2\",\"offset\":2,\"limit\":4}");
        assertThat(result).startsWith("2345");
        assertThat(result).contains("已截断");
    }

    @Test
    void unknownIdReturnsNotice() {
        EvidenceLookupTool tool = new EvidenceLookupTool(new InMemoryMessageStore());
        assertThat(tool.call("{\"evidenceId\":\"nope\"}")).contains("未找到");
    }
}
