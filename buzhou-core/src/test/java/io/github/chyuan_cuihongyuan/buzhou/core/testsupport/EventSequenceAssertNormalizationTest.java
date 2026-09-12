package io.github.chyuan_cuihongyuan.buzhou.core.testsupport;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 黄金轨迹 payload 归一化测试（spec 607 / T864–T865 / impl 460，approval tests 思想）：
 * UUID/ISO 时刻/ISO 时长/epoch 量级数字 → 稳定哨兵（深层递归），结构断言不 flaky。
 */
class EventSequenceAssertNormalizationTest {

    /** 单值归一化：五类易变值 → 哨兵；稳定值原样。 */
    @Test
    void normalizesVolatileValues() {
        assertThat(EventSequenceAssert.normalizeValue(UUID.randomUUID().toString())).isEqualTo("<uuid>");
        assertThat(EventSequenceAssert.normalizeValue(Instant.now().toString())).isEqualTo("<instant>");
        assertThat(EventSequenceAssert.normalizeValue("PT1.25S")).isEqualTo("<duration>");
        assertThat(EventSequenceAssert.normalizeValue(Instant.now().toEpochMilli())).isEqualTo("<epochMs>");
        assertThat(EventSequenceAssert.normalizeValue(Instant.now().getEpochSecond())).isEqualTo("<epochSec>");
        assertThat(EventSequenceAssert.normalizeValue("E429")).isEqualTo("E429");
        assertThat(EventSequenceAssert.normalizeValue(2100)).isEqualTo(2100);
        assertThat(EventSequenceAssert.normalizeValue(true)).isEqualTo(true);
    }

    /** 深层递归：嵌套 Map/List 内的易变值同样哨兵化，键保序。 */
    @Test
    void normalizesNestedContainers() {
        Map<String, Object> normalized = EventSequenceAssert.normalizePayload(Map.of(
                "runId", UUID.randomUUID().toString(),
                "stable", "value",
                "nested", Map.of("at", Instant.now().toString(), "count", 3),
                "list", List.of(UUID.randomUUID().toString(), "keep")));
        assertThat(normalized).containsEntry("runId", "<uuid>")
                .containsEntry("stable", "value")
                .containsEntry("nested", Map.of("at", "<instant>", "count", 3))
                .containsEntry("list", List.of("<uuid>", "keep"));
    }

    /** 端到端：会话发带易变值事件，assertPayloadNormalized 按哨兵结构断言通过。 */
    @Test
    void goldenPayloadAssertionEndToEnd() {
        ScriptedChatModel model = new ScriptedChatModel();
        BuzhouStores stores = Buzhou.inMemoryStores();
        AgentRuntime runtime = Buzhou.runtime(model, stores, io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig.defaults());
        AgentSession session = runtime.spawn("app", "agent", "s1");
        EventSequenceAssert events = EventSequenceAssert.attach(session);

        session.emitEvent("test.volatile", Map.of(
                "runId", UUID.randomUUID().toString(),
                "at", Instant.now().toString(),
                "elapsedMs", Instant.now().toEpochMilli(),
                "status", "ok"));

        events.assertPayloadNormalized("test.volatile", Map.of(
                "runId", "<uuid>",
                "at", "<instant>",
                "elapsedMs", "<epochMs>",
                "status", "ok"));
        session.close();
    }
}
