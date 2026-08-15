package io.github.chyuan_cuihongyuan.buzhou.spill.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-42 / spec 13 §T68 默认值安全化（迁移注记落地为契约）：
 * spill root-dir 独立临时目录、hot-tail 内联预算 64KiB、redis 快照 7 天 TTL、
 * 越界配置启动即拒。
 */
class BuzhouDefaultsMigrationTest {

    @Test
    void spillRootDefaultsToIndependentTempDirectory() {
        SpillProperties properties = new SpillProperties(null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
        assertThat(properties.rootDir())
                .isEqualTo(System.getProperty("java.io.tmpdir") + "/buzhou-spill");
        // 显式配置不受迁移影响
        assertThat(new SpillProperties("/data/spill", null, null, null, null, null, null,
                null, null, null, null, null, null, null).rootDir()).isEqualTo("/data/spill");
    }

    @Test
    void negativeSpillValuesRejectedAtBinding() {
        // 负值不再被静默归一为默认——启动即拒（配置错而能立刻发现）
        assertThatThrownBy(() -> new SpillProperties(null, -5, null, null, null, null, null,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("preview-chars").hasMessageContaining("-5");
        assertThatThrownBy(() -> new SpillProperties(null, null, null, null, null, null, null,
                null, null, null, -100L, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-total-bytes");
        assertThatThrownBy(() -> new SpillProperties(null, null, null, null, null, null, null,
                null, null, null, null, null, Duration.ofMinutes(-1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retention-ttl");
    }

    @Test
    void hotTailInlineBudgetDefaultsTo64KiB() {
        // 迁移契约：默认内联预算 64KiB（原 0=不限）；显式 0/负数仍可恢复不限
        assertThat(io.github.chyuan_cuihongyuan.buzhou.spill.SpillGuardModule
                .DEFAULT_HOT_TAIL_MAX_INLINE_CHARS).isEqualTo(65536L);
    }
}
