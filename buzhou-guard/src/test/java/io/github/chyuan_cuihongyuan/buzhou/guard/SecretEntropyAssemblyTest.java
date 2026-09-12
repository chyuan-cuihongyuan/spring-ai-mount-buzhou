package io.github.chyuan_cuihongyuan.buzhou.guard;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.hook.HookChain;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.DefaultTurnContext;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.hook.HookEnvironment;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemorySessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.guard.secret.SecretScanHook;
import io.github.chyuan_cuihongyuan.buzhou.guard.secret.SecretScanner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 秘密熵阈值装配测试（spec 727 / T1005–T1006 / impl 530）：Builder
 * secretMinEntropy 直通扫描器——示例键被滤、熵关照常（beforeTurn 真缝驱动）。
 */
class SecretEntropyAssemblyTest {

    private static final String AWS_EXAMPLE = "AKIA" + "IOSFODNN" + "7EXAMPLE";

    private static SecretScanHook secretHookOf(GuardModule module) {
        return module.hooksView().stream()
                .filter(h -> h instanceof SecretScanHook)
                .map(h -> (SecretScanHook) h)
                .findFirst().orElseThrow();
    }

    private static String turnInputAfterHook(SecretScanHook hook) {
        HookEnvironment env = new HookEnvironment("s1", "agent",
                new InMemorySessionStateStore());
        DefaultTurnContext ctx = new DefaultTurnContext(env, "key=" + AWS_EXAMPLE);
        hook.beforeTurn(ctx);
        return ctx.input();
    }

    @Test
    void entropyGateFiltersExampleKeyAtInputSeam() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores())
                .secretScanning()
                .secretMinEntropy(SecretScanner.DEFAULT_MIN_ENTROPY)
                .build();

        SecretScanHook hook = secretHookOf(module);
        String input = turnInputAfterHook(hook);

        assertThat(input).isEqualTo("key=" + AWS_EXAMPLE); // 低熵示例——未被替换
    }

    @Test
    void legacyWithoutEntropyStillRedacts() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores())
                .secretScanning()
                .build();

        SecretScanHook hook = secretHookOf(module);
        String input = turnInputAfterHook(hook);

        assertThat(input).contains("[SECRET:AWS_ACCESS_KEY]"); // 既有语义
    }

    @Test
    void hookStillPartOfChainWhenEntropyDeclared() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores())
                .secretScanning()
                .secretMinEntropy(SecretScanner.DEFAULT_MIN_ENTROPY)
                .build();

        assertThat(HookChain.of(module.hooksView())).isNotNull(); // 链可构建
        assertThat(module.assemblySummary()).contains("SecretScanHook");
    }
}
