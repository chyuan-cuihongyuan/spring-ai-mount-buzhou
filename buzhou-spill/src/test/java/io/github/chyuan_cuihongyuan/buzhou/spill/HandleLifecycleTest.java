package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-16 / T44 句柄生命周期（context-clearing 双路径）：
 * 显式逐出（EvictHandleTool）与 TTL 自动逐出；回读复活；墓碑替换占位符。
 */
class HandleLifecycleTest {

    @Test
    void explicitEvictionTombstonesPlaceholder() {
        HandleLifecycleRegistry registry = new HandleLifecycleRegistry();
        String uri = "spill://agent/s-" + UUID.randomUUID() + "/tc-1";
        registry.track(uri, 10);

        // 模型主动逐出（EvictHandleTool 语义）
        String reply = new EvictHandleTool(registry).call(
                "{\"path\":\"" + uri + "\"}");
        assertThat(reply).contains("[已逐出]");
        assertThat(registry.isEvicted(uri, 11, 3)).isTrue();
        assertThat(registry.isEvicted("spill://agent/other/tc-9", 11, 3)).isFalse();
    }

    @Test
    void ttlEvictsColdHandlesAndReadRevives() {
        HandleLifecycleRegistry registry = new HandleLifecycleRegistry();
        String uri = "spill://agent/s-" + UUID.randomUUID() + "/tc-2";
        registry.track(uri, 5); // 第 5 轮溢出登记

        // TTL=3：第 7 轮还热（7-5=2 < 3），第 8 轮过期
        assertThat(registry.isEvicted(uri, 7, 3)).isFalse();
        assertThat(registry.isEvicted(uri, 8, 3)).isTrue();

        // 回读置位 → 视图吸收（第 9 轮）→ 引用刷新复活，不再逐出
        registry.markRead(uri);
        registry.absorbReads(9);
        assertThat(registry.isEvicted(uri, 9, 3)).isFalse();
        assertThat(registry.isEvicted(uri, 11, 3)).isFalse();
        assertThat(registry.isEvicted(uri, 12, 3)).isTrue();
    }

    @Test
    void hotTailTombstonesEvictedHandleInView() throws IOException {
        var root = Files.createTempDirectory("handle-lifecycle");
        SpillModule module = SpillModule.withDefaults(root);
        HandleLifecycleRegistry registry = new HandleLifecycleRegistry();
        HotTailViewProcessor processor = new HotTailViewProcessor(
                module.service(), 1, 0,
                uri -> java.nio.file.Path.of(uri.toString().replace("spill://", "")),
                new SessionReadOnlyRegistry(), java.util.Map.of(), 64);
        processor.setHandleLifecycle(registry, 2);

        // 两条大结果（第 1 轮），近期保留 1 条 → 旧的溢出为占位符并登记句柄
        String sessionId = "hl-" + UUID.randomUUID();
        var history = new java.util.ArrayList<io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage>();
        history.add(toolMessage(sessionId, 1, "tc-old", "old-tool", "旧数据：" + "o".repeat(200)));
        history.add(toolMessage(sessionId, 1, "tc-new", "new-tool", "新数据：" + "n".repeat(200)));
        var view = processor.process(sessionId, history, 1);
        var oldMessage = view.stream()
                .filter(m -> m.toolCallId() != null && m.toolCallId().equals("tc-old"))
                .findFirst().orElseThrow();
        assertThat(oldMessage.content()).contains("spill://");
        String uri = oldMessage.content().substring(
                oldMessage.content().indexOf("spill://")).split("[\\s\\]）]", 2)[0];

        // 显式逐出后下一轮视图：占位符收缩为极简墓碑（原句柄可辨、内容更省）
        registry.markEvicted(uri);
        var view2 = processor.process(sessionId, history, 2);
        var tombstoned = view2.stream()
                .filter(m -> m.toolCallId() != null && m.toolCallId().equals("tc-old"))
                .findFirst().orElseThrow();
        assertThat(tombstoned.content()).startsWith("[句柄已逐出：").contains(uri);
        assertThat(tombstoned.content().length()).isLessThan(oldMessage.content().length());
        // 近期句柄不受影响
        var kept = view2.stream()
                .filter(m -> m.toolCallId() != null && m.toolCallId().equals("tc-new"))
                .findFirst().orElseThrow();
        assertThat(kept.content()).contains("新数据：");
    }

    private static io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage toolMessage(
            String sessionId, int turn, String callId, String toolName, String content) {
        return new io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage(
                UUID.randomUUID().toString(), sessionId, turn, 2,
                io.github.chyuan_cuihongyuan.buzhou.core.message.Role.TOOL,
                content, java.util.List.of(), callId, null, null,
                java.util.Map.of("toolName", toolName), java.time.Instant.now());
    }
}
