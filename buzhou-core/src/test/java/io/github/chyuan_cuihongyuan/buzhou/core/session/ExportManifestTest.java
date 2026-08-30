package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 193 / T566：导出清单回归——摘要稳定顺序无关 / 篡改检出 / 缺失多出 /
 * 空边界 / 覆盖与容错。
 */
class ExportManifestTest {

    @Test
    void manifestStableRegardlessOfAddOrder() {
        ExportManifest one = new ExportManifest().add("b", "{\"v\":2}").add("a", "{\"v\":1}");
        ExportManifest two = new ExportManifest().add("a", "{\"v\":1}").add("b", "{\"v\":2}");

        assertThat(one.manifestJson()).isEqualTo(two.manifestJson()); // id 序归一
        assertThat(one.totalDigest()).isEqualTo(two.totalDigest());
        assertThat(ExportManifest.verify(one.manifestJson(),
                Map.of("a", "{\"v\":1}", "b", "{\"v\":2}")).ok()).isTrue();
    }

    @Test
    void tamperedContentIsDetectedById() {
        ExportManifest manifest = new ExportManifest()
                .add("s1", "{\"secret\":\"原\"}").add("s2", "{\"ok\":1}");
        String manifestJson = manifest.manifestJson();

        ExportManifest.Verification verdict = ExportManifest.verify(manifestJson,
                Map.of("s1", "{\"secret\":\"篡改\"}", "s2", "{\"ok\":1}"));

        assertThat(verdict.ok()).isFalse();
        assertThat(verdict.mismatchedIds()).containsExactly("s1"); // 逐 id 指认
        assertThat(verdict.missingIds()).isEmpty();
    }

    @Test
    void missingAndUnexpectedListed() {
        String manifestJson = new ExportManifest()
                .add("expected", "A").add("also-expected", "B").manifestJson();

        ExportManifest.Verification verdict = ExportManifest.verify(manifestJson,
                Map.of("expected", "A", "intruder", "X"));

        assertThat(verdict.ok()).isFalse();
        assertThat(verdict.missingIds()).containsExactly("also-expected");
        assertThat(verdict.unexpectedIds()).containsExactly("intruder");
    }

    @Test
    void emptyManifestAndNullContents() {
        ExportManifest empty = new ExportManifest();
        assertThat(empty.manifestJson()).contains("\"totalDigest\"");
        assertThat(ExportManifest.verify(empty.manifestJson(), null).ok()).isTrue();
        assertThat(ExportManifest.verify(empty.manifestJson(), Map.of()).ok()).isTrue();
    }

    @Test
    void reAddOverwritesAndBlankDifferenceEquivalent() {
        ExportManifest manifest = new ExportManifest()
                .add("s1", "旧").add("s1", "新内容");
        String manifestJson = manifest.manifestJson();

        assertThat(ExportManifest.verify(manifestJson, Map.of("s1", "新内容")).ok()).isTrue();
        assertThat(ExportManifest.verify(manifestJson, Map.of("s1", "  新内容  ")).ok()).isTrue(); // strip
    }

    @Test
    void corruptManifestAndBadArgumentsFailFast() {
        assertThatThrownBy(() -> ExportManifest.verify("不是 JSON", Map.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("凭证损坏");
        assertThatThrownBy(() -> new ExportManifest().add(" ", "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExportManifest().add("k", null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
