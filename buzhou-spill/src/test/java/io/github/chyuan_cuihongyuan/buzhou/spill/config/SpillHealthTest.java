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
    void downWhenRootNotCreatable() throws java.io.IOException {
        // 根目录指向「文件之下」的子路径——createDirectories 双平台必败
        // （spec 329 跨平台修：原 /dev/null/x 在 Windows 解析为 E:\dev\null\x
        // 且可真创建——假红；文件下建目录 ENOTDIR 语义双平台成立）
        Path blocker = java.nio.file.Files.createTempFile(tempDir, "blocker", ".tmp");
        assertThat(new SpillHealth(true, blocker.resolve("buzhou")).status())
                .isEqualTo(BuzhouHealth.Status.DOWN);
    }

    @Test
    void unknownWhenDisabled() {
        assertThat(new SpillHealth(false, tempDir).status())
                .isEqualTo(BuzhouHealth.Status.UNKNOWN);
    }
}
