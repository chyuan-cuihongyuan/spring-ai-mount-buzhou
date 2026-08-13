package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.Spotlighting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T18×T20 交互回归（评审修复）：spotlighting 开启时，包裹后的工具输出仍能被溢出链路
 * 正确处理——数组形状识别、落盘存干净原文（回读无标记污染）、占位符自描述形状正确。
 */
class SpotlightSpillInteractionTest {

    private static final int THRESHOLD = 100;

    @TempDir
    Path rootDir;

    @Test
    void wrappedJsonArrayStillOffloadsPerItemWithCleanStoredContent() {
        DiskSpillStore store = new DiskSpillStore(rootDir);
        SessionReadOnlyRegistry registry = new SessionReadOnlyRegistry();
        SpillOffloadHook hook = new SpillOffloadHook(new SpillService(store, 64, 3), registry,
                store::dataPathOf, THRESHOLD, Map.of());
        // spotlight 包裹的大 JSON 数组（guard 注入防御开启时 afterTool 先被包裹）
        String payload = "[{\"id\":1,\"blob\":\"" + "x".repeat(THRESHOLD) + "\"}]";
        String wrapped = Spotlighting.wrap("abcd1234", Spotlighting.DEFAULT_MARK_CHAR, 1, payload);

        var env = new io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment(
                "s1", "agent", new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore());
        var ctx = new io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext(
                env, "tc-1", "mcp_query", Map.of());
        ctx.markExecuted(wrapped, null);

        var result = hook.afterTool(ctx);

        // 数组 per-item 溢出仍生效（解包裹后识别 JSON 数组）
        assertThat(result).isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.Replace.class);
        String replaced = String.valueOf(((io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.Replace) result).payload());
        // 替换结果保持 spotlight 包裹（剩余内联项不脱防）
        assertThat(replaced).contains(Spotlighting.BEGIN_HEAD);
        // SpillStore 存的是干净原文（无标记字符污染，回读可用）
        String stored = store.load(SpillUri.parse("spill://agent/s1/tc-1-0")).orElse("");
        assertThat(stored).contains("\"blob\"");
        assertThat(stored).doesNotContain(String.valueOf(Spotlighting.DEFAULT_MARK_CHAR));
    }

    @Test
    void wrappedWholeStringOffloadsWithCorrectShapeHint() {
        DiskSpillStore store = new DiskSpillStore(rootDir);
        SessionReadOnlyRegistry registry = new SessionReadOnlyRegistry();
        SpillOffloadHook hook = new SpillOffloadHook(new SpillService(store, 64, 3), registry,
                store::dataPathOf, THRESHOLD, Map.of());
        String payload = "{\"orderId\":\"ORD-7\",\"lines\":\"" + "y".repeat(THRESHOLD) + "\"}";
        String wrapped = Spotlighting.wrap("abcd1234", Spotlighting.DEFAULT_MARK_CHAR, 1, payload);

        var env = new io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment(
                "s1", "agent", new io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore());
        var ctx = new io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultToolCallContext(
                env, "tc-2", "mcp_query", Map.of());
        ctx.markExecuted(wrapped, null);

        var result = hook.afterTool(ctx);

        assertThat(result).isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.Replace.class);
        String placeholder = String.valueOf(((io.github.chyuan_cuihongyuan.buzhou.core.hook.HookResult.Replace) result).payload());
        // 自描述形状按解包裹后的真实数据判定（JSON 对象，而非「文本」）
        assertThat(placeholder).contains("JSON 对象（顶层字段：orderId, lines");
        // 回读干净切片
        String readBack = store.readRange(SpillUri.parse("spill://agent/s1/tc-2"),
                RangeReadRequest.bytes(0, 11)).content();
        assertThat(readBack).startsWith("{\"orderId\"");
    }
}
