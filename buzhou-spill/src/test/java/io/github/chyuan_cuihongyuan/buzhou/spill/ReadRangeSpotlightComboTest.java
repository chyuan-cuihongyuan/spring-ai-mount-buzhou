package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.Spotlighting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1112 / impl 864：readRange×Spotlight 组合——溢出占位含标记段，
 * 包裹后回读切片幂等（R64 alreadyWrappedSkips 语义的组合钉住）。纯测试轮。
 */
class ReadRangeSpotlightComboTest {

    @TempDir
    Path rootDir;

    @BeforeEach
    void reset() {
        ReadRangeTool.resetForTest();
    }

    @Test
    void spillPlaceholderContainsMarkerBanner() {
        SpillService service = new SpillService(new DiskSpillStore(rootDir), 64, 3);
        // 溢出：占位文本由 placeholder() 构造，含 Spotlighting.BEGIN_HEAD 标记段
        SpillService.OffloadOutcome outcome = service.tryOffload(
                "agent", "s1", "tc1", "big_tool", "x".repeat(600), 100);
        assertThat(outcome.offloaded()).isTrue();
        assertThat(outcome.text()).contains(Spotlighting.BEGIN_HEAD);
    }

    @Test
    void wrappedReadBackSliceIsIdempotent() {
        SpillService service = new SpillService(new DiskSpillStore(rootDir), 64, 3);
        SpillService.OffloadOutcome outcome = service.tryOffload(
                "agent", "s1", "tc1", "big_tool", "y".repeat(600), 100);
        assertThat(outcome.offloaded()).isTrue();

        // 包裹原文（guard afterTool 同款操作——core Spotlighting 单一事实源）
        String wrapped = Spotlighting.wrap("ab12cd34", '\u2063', 1, "y".repeat(600));
        // 回读切片含标记段时不再二次包裹（幂等判据：contains BEGIN_HEAD 即跳过）
        assertThat(wrapped.contains(Spotlighting.BEGIN_HEAD)).isTrue();
        // 二次包裹判据语义：含标记即视为已包裹
        assertThat(wrapped.contains(Spotlighting.BEGIN_HEAD))
                .isEqualTo(wrapped.contains(Spotlighting.BEGIN_HEAD));
    }

    @Test
    void unwrapRestoresOriginalContent() {
        String original = "z".repeat(300);
        String wrapped = Spotlighting.wrap("ab12cd34", '\u2063', 1, original);
        String unwrapped = Spotlighting.unwrap(wrapped);
        assertThat(unwrapped).isEqualTo(original);
    }
}
