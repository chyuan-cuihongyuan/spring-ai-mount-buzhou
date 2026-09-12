package io.github.chyuan_cuihongyuan.buzhou.guard;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.guard.moderation.ContentModerationHook;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 549 / T855：guard 装配摘要读数——assemblySummary 列出装配的 hook
 * 名（装配序），支持包/排障「guard 到底挂了哪些钩子」一屏可读。
 */
class GuardAssemblySummaryTest {

    @Test
    void assemblySummaryListsHookNamesInAssemblyOrder() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        GuardModule module = GuardModule.builder(stores)
                .piiRedaction()
                .contentModeration(List.of("违禁词"), ContentModerationHook.Action.BLOCK)
                .build();
        List<String> summary = module.assemblySummary();
        // builder.enabled 默认 true → DangerousToolGuardHook 恒在首位
        assertThat(summary).contains("DangerousToolGuardHook", "PiiRedactionHook",
                "ContentModerationHook");
    }

    @Test
    void disabledModuleSummaryIsEmpty() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores())
                .enabled(false)
                .build();
        assertThat(module.assemblySummary()).isEmpty();
    }

    @Test
    void unknownYmlKeysIgnoredGracefully() {
        // 未知键不炸装配（宽容语义——错键治理归 config doctor 面）
        GuardModule module = GuardModule.fromYml(Buzhou.inMemoryStores(),
                Map.of("unknown-future-key", "whatever"));
        assertThat(module.assemblySummary()).isNotNull();
    }
}
