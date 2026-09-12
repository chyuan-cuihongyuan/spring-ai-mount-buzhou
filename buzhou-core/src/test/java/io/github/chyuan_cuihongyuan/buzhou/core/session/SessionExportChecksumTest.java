package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 547 / T853：导出校验和——of/verify 往返一致、内容变校验和变、
 * 格式不符 false、空 JSON fail-fast（511 密文封缄的明文通道对偶）。
 */
class SessionExportChecksumTest {

    private static SessionExport sample() {
        return SessionExport.of("sess-1", "app", "agent",
                List.of(), null, Map.of());
    }

    @Test
    void checksumMatchesIntactExport() {
        String json = sample().toJson();
        String checksum = SessionExportChecksum.of(json);
        assertThat(checksum).startsWith("sha256:");
        assertThat(SessionExportChecksum.verify(json, checksum)).isTrue();
    }

    @Test
    void modifiedExportFailsVerification() {
        String json = sample().toJson();
        String checksum = SessionExportChecksum.of(json);
        String tampered = json.replace("sess-1", "sess-2");
        assertThat(SessionExportChecksum.verify(tampered, checksum)).isFalse();
    }

    @Test
    void malformedChecksumAndInputsFailClosed() {
        String json = sample().toJson();
        assertThat(SessionExportChecksum.verify(json, "md5:abc")).isFalse();
        assertThat(SessionExportChecksum.verify(json, null)).isFalse();
        assertThat(SessionExportChecksum.verify(null, "sha256:abc")).isFalse();
        assertThatThrownBy(() -> SessionExportChecksum.of(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
