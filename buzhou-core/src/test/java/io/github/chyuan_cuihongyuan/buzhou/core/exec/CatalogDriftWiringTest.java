package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.AgentRuntime;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工具目录漂移看门狗接线测试（spec 1613 / T2377–T2378 / impl 1166）：
 * 会话构造节拍经 CatalogDriftHolder 拍指纹（spec 201 孤类接线——进程级基线
 * 跨会话）：目录变化 diff 非空 + 事件发出 + 基线推进；稳定目录无事件。
 */
class CatalogDriftWiringTest {

    private static ToolCallback tool(String name) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder().name(name).description("d")
                        .inputSchema("{}").build();
            }

            @Override
            public String call(String input) {
                return "ok";
            }
        };
    }

    @Test
    void sessionAssemblyBeatsCatalogFingerprint() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        AtomicInteger events = new AtomicInteger();
        List<Map<String, Object>> payloads = new ArrayList<>();
        CatalogDriftHolder.setWatcher(new CatalogDriftWatcher(payload -> {
            events.incrementAndGet();
            payloads.add(payload);
        }));
        try {
            // 首会话：目录 [a, b] 建基线（无事件）
            AgentRuntime r1 = Buzhou.runtime(
                    new io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel(),
                    stores, RuntimeConfig.defaults(),
                    new ToolCallback[]{tool("a"), tool("b")});
            try (var s = r1.spawn("app", "agent", "s1")) {
                assertThat(s).isNotNull();
            }
            assertThat(events.get()).isZero();
            assertThat(CatalogDriftHolder.watcher().baselineSummary()).isNotNull();

            // 次会话：目录 [a, c]（b 消失、c 新增）→ 漂移事件 + 基线推进
            AgentRuntime r2 = Buzhou.runtime(
                    new io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel(),
                    stores, RuntimeConfig.defaults(),
                    new ToolCallback[]{tool("a"), tool("c")});
            try (var s = r2.spawn("app", "agent", "s2")) {
                assertThat(s).isNotNull();
            }
            assertThat(events.get()).isEqualTo(1);
            assertThat(payloads.get(0).get("added")).isEqualTo(List.of("c"));
            assertThat(payloads.get(0).get("removed")).isEqualTo(List.of("b"));

            // 第三会话：同 [a, c] → 稳定无事件
            AgentRuntime r3 = Buzhou.runtime(
                    new io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel(),
                    stores, RuntimeConfig.defaults(),
                    new ToolCallback[]{tool("a"), tool("c")});
            try (var s = r3.spawn("app", "agent", "s3")) {
                assertThat(s).isNotNull();
            }
            assertThat(events.get()).isEqualTo(1);
        } finally {
            CatalogDriftHolder.setWatcher(null); // 复位默认
        }
    }

    @Test
    void holderSnapshotFeedsWatcherDirectly() {
        CatalogDriftHolder.setWatcher(null);
        CatalogDriftHolder.snapshot(List.of(tool("x").getToolDefinition()));
        Optional<ToolCatalogFingerprint.Diff> drift =
                CatalogDriftHolder.watcher().check(List.of(
                        tool("x").getToolDefinition(), tool("y").getToolDefinition()));
        assertThat(drift).isPresent();
        assertThat(drift.get().added()).isEqualTo(List.of("y"));
    }
}
