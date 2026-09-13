package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-664 / spec 911：JCS 规范化内容指纹——嵌套 Map 键序漂移不影响指纹、
 * List 元素保序（数组有序是语义）、前缀区分防混用、既有 contentFingerprint 零变化。
 */
class JcsFingerprintTest {

    private static SessionExport exportWithMetadata(Map<String, Object> metadata) {
        var message = new BuzhouMessage("m1", "s1", 1, 1,
                io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER,
                "hello", null, null, null, null, metadata, Instant.EPOCH);
        return SessionExport.of("s1", "app", "agent", List.of(message), null, Map.of());
    }

    private static final java.time.Instant Instant = java.time.Instant.EPOCH;

    @Test
    void nestedKeyOrderDriftDoesNotChangeCanonicalFingerprint() {
        Map<String, Object> metaA = new LinkedHashMap<>();
        metaA.put("alpha", "1");
        metaA.put("beta", "2");
        Map<String, Object> metaB = new LinkedHashMap<>();
        metaB.put("beta", "2");
        metaB.put("alpha", "1");

        String fpA = SessionExportChecksum.canonicalContentFingerprint(exportWithMetadata(metaA));
        String fpB = SessionExportChecksum.canonicalContentFingerprint(exportWithMetadata(metaB));
        assertThat(fpA).isEqualTo(fpB); // 键序漂移不变——规范化语义

        // 保序版（spec 710）对同数据对会判异——两口径语义不同，共同成立
        String plainA = SessionExportChecksum.contentFingerprint(exportWithMetadata(metaA));
        String plainB = SessionExportChecksum.contentFingerprint(exportWithMetadata(metaB));
        assertThat(fpA).startsWith("sha256-j:").isNotEqualTo(plainA);
    }

    @Test
    void listElementOrderStillMatters() {
        var ordered = SessionExport.of("s1", "app", "agent",
                List.of(msg("m1"), msg("m2")), null, Map.of());
        var swapped = SessionExport.of("s1", "app", "agent",
                List.of(msg("m2"), msg("m1")), null, Map.of());
        assertThat(SessionExportChecksum.canonicalContentFingerprint(ordered))
                .isNotEqualTo(SessionExportChecksum.canonicalContentFingerprint(swapped));
    }

    @Test
    void prefixesDistinctAndLegacyPathUntouched() {
        var export = exportWithMetadata(Map.of("k", "v"));
        assertThat(SessionExportChecksum.canonicalContentFingerprint(export))
                .startsWith(SessionExportChecksum.CANONICAL_PREFIX);
        assertThat(SessionExportChecksum.contentFingerprint(export))
                .startsWith(SessionExportChecksum.CONTENT_PREFIX);
        // null fail-fast 同口径
        assertThatThrownBy(() -> SessionExportChecksum.canonicalContentFingerprint(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static BuzhouMessage msg(String id) {
        return new BuzhouMessage(id, "s1", 1, 1,
                io.github.chyuan_cuihongyuan.buzhou.core.message.Role.USER,
                id, null, null, null, null, Map.of(), java.time.Instant.EPOCH);
    }
}
