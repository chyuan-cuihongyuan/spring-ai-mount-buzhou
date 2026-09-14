package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1439 补位 / T2180：导出清单校验统计——受踪包装透传校验结果、
 * ok/failed 守恒、三明细桶累计、末次入口、reset 归零。
 */
class ExportManifestVerifyStatsTest {

    @BeforeEach
    void reset() {
        ExportManifestVerifyStats.resetForTest();
    }

    @AfterEach
    void resetAfter() {
        ExportManifestVerifyStats.resetForTest();
    }

    @Test
    void okVerificationCounted() {
        String body = "{\"entries\":[{\"id\":\"a\",\"digest\":\"" +
                "deadbeef" + "\"}]}";
        // 故意不匹配内容：mismatched 桶 + failed 计数
        ExportManifestVerifyStats.verify(body, Map.of("a", "内容"));
        var s = ExportManifestVerifyStats.stats();
        assertThat(s.verifies()).isEqualTo(1);
        assertThat(s.failed()).isEqualTo(1);
        assertThat(s.mismatchedTotal()).isEqualTo(1);
        assertThat(s.conserved()).isTrue();
        assertThat(s.lastEntryKind()).isEqualTo("full");
    }

    @Test
    void lastEntryKindTracksSeam() {
        ExportManifestVerifyStats.verifyCanonical("{}", java.util.Map.of());
        assertThat(ExportManifestVerifyStats.stats().lastEntryKind()).isEqualTo("canonical");
        ExportManifestVerifyStats.verifySubset("{\"entries\":[]}", java.util.Map.of("a", "{}"));
        assertThat(ExportManifestVerifyStats.stats().lastEntryKind()).isEqualTo("subset");
    }

    @Test
    void resetForTestClearsAll() {
        ExportManifestVerifyStats.verify("{}", java.util.Map.of());
        ExportManifestVerifyStats.resetForTest();
        var s = ExportManifestVerifyStats.stats();
        assertThat(s.verifies()).isZero();
        assertThat(s.conserved()).isTrue();
        assertThat(s.lastEntryKind()).isNull();
    }
}
