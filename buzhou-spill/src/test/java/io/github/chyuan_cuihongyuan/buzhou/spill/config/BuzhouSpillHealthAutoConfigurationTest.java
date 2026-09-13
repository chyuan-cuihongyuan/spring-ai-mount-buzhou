package io.github.chyuan_cuihongyuan.buzhou.spill.config;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BuzhouSpillHealthAutoConfiguration 直测（spec 1200 / T1801 / K 会话 R1 补测——此前零覆盖）。
 *
 * <p>ApplicationContextRunner 装配面 + SpillHealth 三态（UP=rootDir 临时文件写删探针通过 /
 * UNKNOWN=禁用 / DOWN=rootDir 不可写）与 SpillPairHealth 两态。
 */
class BuzhouSpillHealthAutoConfigurationTest {

    private static final String ENABLED_KEY = "buzhou.spill.enabled";
    private static final String ROOT_DIR_KEY = "buzhou.spill.root-dir";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouSpillHealthAutoConfiguration.class));

    @TempDir
    Path tempDir;

    @Test
    void defaultAssemblyIsUpWithTmpdirRoot() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(SpillHealth.class);
            assertThat(ctx).hasSingleBean(SpillPairHealth.class);
            SpillHealth spillHealth = ctx.getBean(SpillHealth.class);
            assertThat(spillHealth.status()).isEqualTo(BuzhouHealth.Status.UP);
            // 未配置 root-dir → 与 SpillProperties 同源回退 tmpdir（Path.of 归一化分隔符）
            assertThat(spillHealth.details()).containsEntry("rootDir",
                    Path.of(System.getProperty("java.io.tmpdir"), "buzhou-spill").toString());
            // spring-boot-health 在测试 classpath → indicator 装配
            assertThat(ctx).hasBean("spillHealthIndicator");
        });
    }

    @Test
    void configuredRootDirIsHonored() {
        Path root = tempDir.resolve("spill-root");
        runner.withPropertyValues(ROOT_DIR_KEY + "=" + root)
                .run(ctx -> {
                    SpillHealth spillHealth = ctx.getBean(SpillHealth.class);
                    assertThat(spillHealth.status()).isEqualTo(BuzhouHealth.Status.UP);
                    // 探针通过即建目录（目录不存在 ≠ 不可写）
                    assertThat(root).exists();
                    assertThat(spillHealth.details()).containsEntry("rootDir", root.toString());
                });
    }

    @Test
    void rootDirOccupiedByFileReportsDown() throws IOException {
        Path occupied = Files.createFile(tempDir.resolve("occupied"));
        runner.withPropertyValues(ROOT_DIR_KEY + "=" + occupied)
                .run(ctx -> {
                    SpillHealth spillHealth = ctx.getBean(SpillHealth.class);
                    // 路径被文件占用 → createDirectories 抛 → 核心职能不可用，DOWN
                    assertThat(spillHealth.status()).isEqualTo(BuzhouHealth.Status.DOWN);
                });
    }

    @Test
    void disabledReportsUnknownOnBothFaces() {
        runner.withPropertyValues(ENABLED_KEY + "=false")
                .run(ctx -> {
                    SpillHealth spillHealth = ctx.getBean(SpillHealth.class);
                    SpillPairHealth spillPairHealth = ctx.getBean(SpillPairHealth.class);
                    assertThat(spillHealth.status()).isEqualTo(BuzhouHealth.Status.UNKNOWN);
                    assertThat(spillHealth.details()).containsEntry("enabled", false);
                    assertThat(spillPairHealth.status()).isEqualTo(BuzhouHealth.Status.UNKNOWN);
                });
    }
}
