package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-687 / spec 935：ExportManifest 规范化摘要——键序漂移不误报、verify 通过、
 * 旧 add 行为零变化。
 */
class ManifestCanonicalTest {

    @Test
    void keyOrderDriftDoesNotBreakCanonicalVerify() {
        ExportManifest manifest = new ExportManifest();
        String contentA = "{\"sessionId\":\"s1\",\"data\":{\"x\":1,\"y\":2}}";
        // 同内容、嵌套与顶层键序漂移的搬运副本
        String contentB = "{\"data\":{\"y\":2,\"x\":1},\"sessionId\":\"s1\"}";

        manifest.addCanonical("s1", contentA);
        ExportManifest.Verification verification = ExportManifest.verifyCanonical(
                manifest.manifestJson(), Map.of("s1", contentB));
        assertThat(verification.ok()).isTrue(); // 键序漂移不误报 mismatch
        assertThat(verification.mismatchedIds()).isEmpty();
    }

    @Test
    void legacyAddKeepsRawDigestSemantics() {
        ExportManifest manifest = new ExportManifest();
        String contentA = "{\"sessionId\":\"s1\",\"data\":{\"x\":1,\"y\":2}}";
        String contentB = "{\"data\":{\"y\":2,\"x\":1},\"sessionId\":\"s1\"}";

        manifest.add("s1", contentA);
        ExportManifest.Verification verification = ExportManifest.verify(
                manifest.manifestJson(), Map.of("s1", contentB));
        // 旧 add：原始文本摘要——键序漂移判 mismatch（既有语义零变化）
        assertThat(verification.ok()).isFalse();
    }

    @Test
    void tamperedContentStillDetected() {
        ExportManifest manifest = new ExportManifest();
        manifest.addCanonical("s1", "{\"sessionId\":\"s1\",\"v\":1}");
        String tampered = "{\"sessionId\":\"s1\",\"v\":2}";
        ExportManifest.Verification verification = ExportManifest.verify(
                manifest.manifestJson(), Map.of("s1", tampered));
        assertThat(verification.ok()).isFalse(); // 规范化不放过真篡改
    }
}
