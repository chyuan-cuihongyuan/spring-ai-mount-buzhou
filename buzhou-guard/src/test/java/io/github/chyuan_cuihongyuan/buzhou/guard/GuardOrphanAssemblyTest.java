package io.github.chyuan_cuihongyuan.buzhou.guard;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.guard.hook.InputFloodGuardHook;
import io.github.chyuan_cuihongyuan.buzhou.guard.policy.ToolPermissions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * guard 孤类装配面测试（spec 1612 / T2375–T2376 / impl 1165）：spec 141 角色权限
 * 与 spec 167 泛洪防护两 hook 自 Builder 声明即注册（此前是零装配路径孤类）；
 * 未声明零注册（默认零行为）。
 */
class GuardOrphanAssemblyTest {

    @Test
    void toolRoleGuardRegistersWhenPermissionsDeclared() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        GuardModule module = GuardModule.builder(stores)
                .toolRoleGuard(new ToolPermissions(Map.of(
                        "ops", List.of("run_command", "write_file"),
                        "viewer", List.of("read_file"))))
                .build();
        assertThat(module.hooksView())
                .anyMatch(h -> h instanceof io.github.chyuan_cuihongyuan.buzhou.guard.hook.ToolRoleGuardHook);
    }

    @Test
    void inputFloodGuardRegistersWithCustomConfig() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        GuardModule module = GuardModule.builder(stores)
                .inputFloodGuard(new InputFloodGuardHook.Config(3, Duration.ofSeconds(10)))
                .build();
        assertThat(module.hooksView())
                .anyMatch(h -> h instanceof io.github.chyuan_cuihongyuan.buzhou.guard.hook.InputFloodGuardHook);
    }

    @Test
    void defaultBuildRegistersNeitherOrphanHook() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        GuardModule module = GuardModule.builder(stores).build();
        assertThat(module.hooksView())
                .noneMatch(h -> h instanceof io.github.chyuan_cuihongyuan.buzhou.guard.hook.ToolRoleGuardHook)
                .noneMatch(h -> h instanceof io.github.chyuan_cuihongyuan.buzhou.guard.hook.InputFloodGuardHook);
    }
}
