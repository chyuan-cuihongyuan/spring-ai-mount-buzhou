package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-690 / spec 941：ExportManifest 子集校验——子集内逐项核对、manifest 未提供
 * 条目不计缺失、空 contents fail-fast、篡改仍检出。
 */
class ManifestSubsetTest {

    @Test
    void subsetVerificationIgnoresAbsentEntries() {
        ExportManifest manifest = new ExportManifest()
                .addCanonical("s1", "{\"sessionId\":\"s1\"}")
                .addCanonical("s2", "{\"sessionId\":\"s2\"}")
                .addCanonical("s3", "{\"sessionId\":\"s3\"}");

        // 只搬运了 s2——子集校验通过（s1/s3 不计缺失）
        var verification = ExportManifest.verifySubset(
                manifest.manifestJson(), Map.of("s2", "{\"sessionId\":\"s2\"}"));
        assertThat(verification.ok()).isTrue();
        assertThat(verification.mismatchedIds()).isEmpty();
    }

    @Test
    void tamperedSubsetEntryStillDetected() {
        ExportManifest manifest = new ExportManifest()
                .addCanonical("s1", "{\"sessionId\":\"s1\"}")
                .addCanonical("s2", "{\"sessionId\":\"s2\"}");

        var verification = ExportManifest.verifySubset(
                manifest.manifestJson(), Map.of("s2", "{\"sessionId\":\"s2\",\"extra\":true}"));
        assertThat(verification.ok()).isFalse();
        assertThat(verification.mismatchedIds()).containsExactly("s2");
    }

    @Test
    void subsetEntryNotInManifestFlagged() {
        ExportManifest manifest = new ExportManifest()
                .addCanonical("s1", "{\"sessionId\":\"s1\"}");

        var verification = ExportManifest.verifySubset(
                manifest.manifestJson(), Map.of("ghost", "{\"sessionId\":\"g\"}"));
        assertThat(verification.ok()).isFalse();
        assertThat(verification.mismatchedIds()).containsExactly("ghost");
    }

    @Test
    void emptyContentsFailsFast() {
        assertThatThrownBy(() -> ExportManifest.verifySubset(
                "{}", java.util.Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
