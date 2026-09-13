package io.github.chyuan_cuihongyuan.buzhou.core.token;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TableContextWindowResolver 直测（spec 1200 / T1801 / K 会话 R1 补测——此前仅被
 * buzhou-memory 跨模块执行，core 本模块零覆盖）。
 *
 * <p>重点锁定三条微妙语义：override 是<b>精确键</b>匹配（不走前缀）；内置表前缀匹配
 * <b>大小写不敏感</b>；未知模型回退 32K。另锁内置表关键值，防误改静默改变预算行为。
 */
class TableContextWindowResolverTest {

    private static final int FALLBACK_WINDOW = 32768;

    @Test
    void overrideIsExactKeyMatchNotPrefix() {
        Map<String, Integer> overrides = new HashMap<>();
        overrides.put("gpt-4", 8);
        TableContextWindowResolver resolver = new TableContextWindowResolver(overrides);
        assertThat(resolver.resolveWindow("gpt-4")).isEqualTo(8);
        // 精确键未命中 → 走内置表前缀，而非把 override 前缀化套用
        assertThat(resolver.resolveWindow("gpt-4o")).isEqualTo(128000);
        assertThat(resolver.resolveWindow("gpt-4x-mini")).isEqualTo(128000);
    }

    @Test
    void builtInPrefixMatchIsCaseInsensitive() {
        TableContextWindowResolver resolver = new TableContextWindowResolver(Map.of());
        assertThat(resolver.resolveWindow("GPT-4o")).isEqualTo(128000);
        assertThat(resolver.resolveWindow("Claude-Sonnet-4")).isEqualTo(200000);
        assertThat(resolver.resolveWindow("DeepSeek-V3")).isEqualTo(65536);
        assertThat(resolver.resolveWindow("Gemini-2.5-Pro")).isEqualTo(1000000);
    }

    @Test
    void unknownModelAndNullFallBackTo32K() {
        TableContextWindowResolver resolver = new TableContextWindowResolver(Map.of());
        assertThat(resolver.resolveWindow("mystery-model-x")).isEqualTo(FALLBACK_WINDOW);
        assertThat(resolver.resolveWindow(null)).isEqualTo(FALLBACK_WINDOW);
    }

    @Test
    void nullOverridesMapBehavesAsEmpty() {
        TableContextWindowResolver resolver = new TableContextWindowResolver(null);
        assertThat(resolver.resolveWindow("gpt-5")).isEqualTo(400000);
    }

    @Test
    void builtInTableKeyValuesArePinned() {
        TableContextWindowResolver resolver = new TableContextWindowResolver(Map.of());
        assertThat(resolver.resolveWindow("gpt-5")).isEqualTo(400000);
        assertThat(resolver.resolveWindow("o1-mini")).isEqualTo(200000);
        assertThat(resolver.resolveWindow("o3")).isEqualTo(200000);
        assertThat(resolver.resolveWindow("glm-4.6")).isEqualTo(131072);
        assertThat(resolver.resolveWindow("kimi-k2")).isEqualTo(131072);
        assertThat(resolver.resolveWindow("qwen3-max")).isEqualTo(131072);
        assertThat(resolver.resolveWindow("qwq-32b")).isEqualTo(131072);
    }

    @Test
    void prefixMatchingStopsAtLongestTableInsertionOrderPrefix() {
        // 内置表按插入序匹配：gpt-5 先于 gpt-4，"gpt-5x" 命中 gpt-5 档
        TableContextWindowResolver resolver = new TableContextWindowResolver(Map.of());
        assertThat(resolver.resolveWindow("gpt-5-turbo")).isEqualTo(400000);
    }
}
