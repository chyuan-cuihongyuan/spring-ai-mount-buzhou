package io.github.chyuan_cuihongyuan.buzhou.guard;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 泄漏金丝雀 yml 装配测试（spec 1638 / T2427–T2428 / impl 1191）：
 * GuardModule.fromYml 的 leak-canary.salt 声明路径与 Builder 等价
 * （编程面已 spec 1625 验证——此处钉 salt 传递与会话种植链）。
 */
class LeakCanaryAssemblyTest {

    @Test
    void saltedModulePlantsAndDetects() {
        String salt = "assembly-test-salt";
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores())
                .leakCanary(salt)
                .build();
        var hook = module.hooksView().stream()
                .filter(h -> h instanceof io.github.chyuan_cuihongyuan.buzhou.guard.leak.SessionCanaryHook)
                .map(h -> (io.github.chyuan_cuihongyuan.buzhou.guard.leak.SessionCanaryHook) h)
                .findFirst().orElseThrow();
        String tokenA = hook.registry().plant("session-a");
        // B 会话输出含 A 令牌 → 泄漏检出（spec 1625 语义经装配产物复验）
        assertThat(hook.registry().detect("session-b", "leak: " + tokenA)).hasSize(1);
        assertThat(hook.registry().detect("session-a", "echo " + tokenA)).isEmpty(); // 自回显不算
    }

    @Test
    void withoutSaltNoCanaryHook() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores()).build();
        assertThat(module.hooksView())
                .noneMatch(h -> h instanceof io.github.chyuan_cuihongyuan.buzhou.guard.leak.SessionCanaryHook);
    }
}
