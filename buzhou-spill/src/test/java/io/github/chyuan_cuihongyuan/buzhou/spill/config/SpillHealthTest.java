package io.github.chyuan_cuihongyuan.buzhou.spill.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-41 / spec 13 §T66 spill 健康：UP（rootDir 写删往返）/ DOWN（不可写）/
 * UNKNOWN（禁用）。
 */
class SpillHealthTest {

    @TempDir
    Path tempDir;

    @Test
    void upWithWritableRoot() {
        assertThat(new SpillHealth(true, tempDir).status()).isEqualTo(BuzhouHealth.Status.UP);
        assertThat(new SpillHealth(true, tempDir).mechanism()).isEqualTo("spill");
    }

    @Test
    void downWhenRootNotCreatable() {
        // 根目录指向不可创建的路径（/dev/null/x —— ENOTDIR，跨平台成立）
        assertThat(new SpillHealth(true, Path.of("/dev/null/buzhou")).status())
                .isEqualTo(BuzhouHealth.Status.DOWN);
    }

    @Test
    void unknownWhenDisabled() {
        assertThat(new SpillHealth(false, tempDir).status())
                .isEqualTo(BuzhouHealth.Status.UNKNOWN);
    }
}
