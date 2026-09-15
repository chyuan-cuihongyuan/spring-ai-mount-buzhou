package io.github.chyuan_cuihongyuan.buzhou.guard;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.guard.GuardModule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GuardModule Builder 开关组合矩阵补测（K 会话 R36 / spec 1227+ / T1873——
 * GuardModule$Builder 112 missed 集中区）：feature toggle 开→hook 注册、关→不注册、
 * 全关最小面、fromYml 等价——builder 装配合同的全真值矩阵。
 * 先例：GuardAssemblySummaryTest（assemblySummary 读数）。
 */
class GuardModuleBuilderBranchTest {

    private static List<String> hookNames(GuardModule module) {
        return module.assemblySummary();
    }

    @Test
    void allOffYieldsMinimalHooks() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores()).build();
        List<String> names = hookNames(module);
        // enabled 默认 true → DangerousToolGuardHook 恒在
        assertThat(names).contains("DangerousToolGuardHook");
        assertThat(names).doesNotContain("PiiRedactionHook", "SpotlightHook",
                "TaintTrackingHook", "CanaryGuardHook");
    }

    @Test
    void spotlightingRegistersHook() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores()).spotlighting().build();
        assertThat(hookNames(module)).contains("SpotlightHook");
    }

    @Test
    void injectionDefenseRegistersHook() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores()).injectionDefense().build();
        assertThat(hookNames(module)).contains("CanaryGuardHook");
    }

    @Test
    void taintTrackingRegistersHook() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores()).taintTracking().build();
        assertThat(hookNames(module)).contains("TaintTrackingHook");
    }

    @Test
    void piiRedactionRegistersHook() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores()).piiRedaction().build();
        assertThat(hookNames(module)).contains("PiiRedactionHook");
    }

    @Test
    void canaryGuardRegistersHook() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores())
                .canaryGuard().canaryToken("test-token").build();
        assertThat(hookNames(module)).contains("CanaryGuardHook");
    }

    @Test
    void disabledRemovesDangerousToolGuard() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores()).enabled(false).build();
        List<String> names = hookNames(module);
        assertThat(names).doesNotContain("DangerousToolGuardHook");
    }

    @Test
    void dangerousToolEntryRegisters() {
        GuardModule module = GuardModule.builder(Buzhou.inMemoryStores())
                .dangerousTool("deploy_prod", "PUBLISHED", "需审批")
                .build();
        assertThat(module.dangerousTools()).hasSize(1);
        assertThat(module.dangerousTools().get(0).name()).isEqualTo("deploy_prod");
    }

    @Test
    void fromYmlMatchesBuilderEquivalent() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        GuardModule module = GuardModule.fromYml(stores, Map.of(
                "pii-redaction", true,
                "spotlighting", true));
        List<String> names = hookNames(module);
        assertThat(names).contains("SpotlightHook");
    }

    @Test
    void allOnYieldsSupersetOfAllOff() {
        GuardModule allOff = GuardModule.builder(Buzhou.inMemoryStores()).build();
        GuardModule allOn = GuardModule.builder(Buzhou.inMemoryStores())
                .spotlighting().canaryGuard().injectionDefense()
                .piiRedaction().build();
        List<String> offNames = hookNames(allOff);
        List<String> onNames = hookNames(allOn);
        // 全开的 hook 集合 ⊇ 全关最小面
        assertThat(onNames).containsAll(offNames);
        assertThat(onNames.size()).isGreaterThan(offNames.size());
    }
}
