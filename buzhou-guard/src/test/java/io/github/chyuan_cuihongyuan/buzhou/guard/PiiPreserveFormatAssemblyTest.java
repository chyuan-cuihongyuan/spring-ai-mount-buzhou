package io.github.chyuan_cuihongyuan.buzhou.guard;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.guard.pii.PiiInputRedactionHook;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PII 假名化模式装配测试（spec 731 / T1013–T1014 / impl 534）：Builder
 * piiPreserveFormat → 输入缝假名化（同长度数字保持）；缺省 MASK 占位符。
 */
class PiiPreserveFormatAssemblyTest {

    private static String inputAfter(GuardModule module, String text) {
        HookEnvironment env = new HookEnvironment("s1", "agent",
                new InMemorySessionStateStore());
        DefaultTurnContext ctx = new DefaultTurnContext(env, text);
        module.hooksView().stream()
                .filter(h -> h instanceof PiiInputRedactionHook)
                .findFirst().orElseThrow()
                .beforeTurn(ctx);
        return ctx.input();
    }

    @Test
    void preserveFormatModePseudonymizesInput() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores())
                .piiInputRedaction()
                .piiPreserveFormat()
                .build();

        String out = inputAfter(module, "call 13812345678 now");

        assertThat(out).matches("call \\d{11} now"); // 形状保持——非占位符
        assertThat(out).doesNotContain("[PII:");
        assertThat(out).doesNotContain("13812345678"); // 原文不留痕
    }

    @Test
    void defaultModeKeepsMaskSemantics() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores())
                .piiInputRedaction()
                .build();

        String out = inputAfter(module, "call 13812345678 now");

        assertThat(out).isEqualTo("call [PII:CN_PHONE] now"); // 既有 MASK 语义
    }

    @Test
    void assemblySummaryListsBothHooksWhenBothEnabled() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores())
                .piiRedaction()
                .piiInputRedaction()
                .piiPreserveFormat()
                .build();

        assertThat(module.assemblySummary()).contains("PiiRedactionHook", "PiiInputRedactionHook");
    }
}
