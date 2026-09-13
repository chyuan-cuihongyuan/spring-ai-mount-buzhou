package io.github.chyuan_cuihongyuan.buzhou.guard.audit;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyRingStatsTest {

    private static KeyPair generateKeyPair() {
        try {
            // Ed25519：现代签名算法，生成瞬时（测试确定性友好）
            KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void freshRingHasZeroCounters() {
        SigningKeyRing ring = new SigningKeyRing();

        SigningKeyRing.KeyRingStats stats = ring.stats();
        assertThat(stats.verifyAttempts()).isZero();
        assertThat(stats.verifyKeyMisses()).isZero();
        assertThat(stats.rotations()).isZero();
        assertThat(stats.activeVersion()).isZero();
    }

    @Test
    void rotationsCountedAndActiveVersionTracked() {
        SigningKeyRing ring = new SigningKeyRing();

        ring.rotate(1, generateKeyPair());
        ring.rotate(2, generateKeyPair());

        SigningKeyRing.KeyRingStats stats = ring.stats();
        assertThat(stats.rotations()).isEqualTo(2);
        assertThat(stats.activeVersion()).isEqualTo(2);
    }

    @Test
    void unknownVersionVerifyCountsMiss() {
        SigningKeyRing ring = new SigningKeyRing();

        assertThat(ring.verifyKey(7)).isNull();

        SigningKeyRing.KeyRingStats stats = ring.stats();
        assertThat(stats.verifyAttempts()).isEqualTo(1);
        assertThat(stats.verifyKeyMisses()).isEqualTo(1);
    }

    @Test
    void belowMinVerifyVersionCountsMiss() {
        KeyPair keyPair = generateKeyPair();
        SigningKeyRing ring = new SigningKeyRing(5,
                List.of(new SigningKeyProvider.VersionedSigningKey(5,
                        keyPair.getPrivate(), keyPair.getPublic())), null);

        assertThat(ring.verifyKey(3)).isNull();

        SigningKeyRing.KeyRingStats stats = ring.stats();
        assertThat(stats.verifyKeyMisses()).isEqualTo(1);
        assertThat(stats.verifyAttempts()).isEqualTo(1);
    }

    @Test
    void invalidRotationRejectedAndNotCounted() {
        SigningKeyRing ring = new SigningKeyRing();

        ring.rotate(1, generateKeyPair());
        assertThatThrownBy(() -> ring.rotate(1, generateKeyPair()))
                .isInstanceOf(IllegalArgumentException.class);

        SigningKeyRing.KeyRingStats stats = ring.stats();
        assertThat(stats.rotations()).isEqualTo(1);
    }
}
