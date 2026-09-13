package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.crypto.EnvelopeCipher;
import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptedSessionExportStatsTest {

    private static final String KEY_A = Base64.getEncoder().encodeToString(new byte[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31});
    private static final String KEY_B = Base64.getEncoder().encodeToString(new byte[]{
            31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16,
            15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0});

    private static SessionExport sample() {
        return SessionExport.of("sess-1", "app", "agent", List.of(), null, Map.of());
    }

    @Test
    void freshInstanceHasZeroCounts() {
        assertThat(new EncryptedSessionExport(new EnvelopeCipher(KEY_A, null)).stats())
                .isEqualTo(new EncryptedSessionExport.SealStats(0, 0, 0));
    }

    @Test
    void sealThenOpenCountsLifecycle() {
        EncryptedSessionExport sealer = new EncryptedSessionExport(new EnvelopeCipher(KEY_A, null));

        String sealed = sealer.seal(sample());
        sealer.open(sealed);

        assertThat(sealer.stats()).isEqualTo(new EncryptedSessionExport.SealStats(1, 1, 0));
    }

    @Test
    void plaintextOpenIsCountedRejected() {
        EncryptedSessionExport sealer = new EncryptedSessionExport(new EnvelopeCipher(KEY_A, null));

        assertThatThrownBy(() -> sealer.open("plain-text-not-sealed"))
                .isInstanceOf(BuzhouException.class);

        assertThat(sealer.stats().openRejected()).isEqualTo(1);
        assertThat(sealer.stats().sealed()).isZero();
    }

    @Test
    void wrongKeyOpenIsCountedRejected() {
        EncryptedSessionExport sealer = new EncryptedSessionExport(new EnvelopeCipher(KEY_A, null));
        EncryptedSessionExport stranger = new EncryptedSessionExport(new EnvelopeCipher(KEY_B, null));
        String sealed = sealer.seal(sample());

        assertThatThrownBy(() -> stranger.open(sealed)).isInstanceOf(BuzhouException.class);

        assertThat(stranger.stats().openRejected()).isEqualTo(1);
        assertThat(sealer.stats().sealed()).isEqualTo(1);
        assertThat(sealer.stats().openRejected()).isZero();
    }

    @Test
    void tamperedPayloadIsCountedRejected() {
        EncryptedSessionExport sealer = new EncryptedSessionExport(new EnvelopeCipher(KEY_A, null));
        String sealed = sealer.seal(sample());
        String tampered = sealed.substring(0, sealed.length() - 4) + "AAAA";

        assertThatThrownBy(() -> sealer.open(tampered)).isInstanceOf(BuzhouException.class);

        assertThat(sealer.stats().openRejected()).isEqualTo(1);
    }
}
