package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1508 / T2267–T2268：进程级危险工具名注册表——并集幂等、快照不可变、reset 清零。
 */
class DangerousToolRegistryTest {

    @AfterEach
    void tearDown() {
        DangerousToolRegistry.reset();
    }

    @Test
    void registerShouldUnionIdempotently() {
        DangerousToolRegistry.register(Set.of("write_file", "run_command"));
        DangerousToolRegistry.register(Set.of("run_command", "http_request"));
        DangerousToolRegistry.register(null); // 防御：null 静默忽略

        assertThat(DangerousToolRegistry.registered())
                .containsExactlyInAnyOrder("write_file", "run_command", "http_request");
    }

    @Test
    void snapshotShouldBeImmutableAndResetShouldClear() {
        DangerousToolRegistry.register(Set.of("write_file"));
        Set<String> snapshot = DangerousToolRegistry.registered();
        assertThat(snapshot).containsExactly("write_file");

        DangerousToolRegistry.reset();
        assertThat(DangerousToolRegistry.registered()).isEmpty();
        // 快照是拷贝：reset 不影响已取快照
        assertThat(snapshot).containsExactly("write_file");
    }
}
