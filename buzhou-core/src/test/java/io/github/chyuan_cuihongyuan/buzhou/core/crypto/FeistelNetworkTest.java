package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class FeistelNetworkTest {

    @Test
    void shouldBeSelfInvertingOnRandomBlocks() {
        Random random = new Random(8036);
        for (int round = 0; round < 500; round++) {
            long block = random.nextLong();
            long encrypted = FeistelNetwork.encrypt(block, 1L, 2L);
            assertThat(FeistelNetwork.decrypt(encrypted, 1L, 2L))
                    .as("round=%d", round).isEqualTo(block);
        }
        assertThat(FeistelNetwork.decrypt(FeistelNetwork.encrypt(0L, 9L, 9L), 9L, 9L)).isEqualTo(0L);
    }

    @Test
    void shouldAvalancheOnKeyAndBlockChanges() {
        Random random = new Random(8037);
        double totalFlipRatio = 0;
        int trials = 200;
        for (int trial = 0; trial < trials; trial++) {
            long block = random.nextLong();
            long key0 = random.nextLong();
            long flippedKey = key0 ^ 1L;
            long encrypted = FeistelNetwork.encrypt(block, key0, 7L);
            long flipped = FeistelNetwork.encrypt(block, flippedKey, 7L);
            totalFlipRatio += Long.bitCount(encrypted ^ flipped) / 64.0;
        }
        assertThat(totalFlipRatio / trials).isBetween(0.25, 0.75);
        long block = 123456789L;
        assertThat(FeistelNetwork.encrypt(block, 1L, 2L))
                .isNotEqualTo(FeistelNetwork.encrypt(block, 1L, 3L));
    }

    @Test
    void shouldBeDeterministic() {
        long block = 987654321L;
        assertThat(FeistelNetwork.encrypt(block, 5L, 6L))
                .isEqualTo(FeistelNetwork.encrypt(block, 5L, 6L));
        assertThat(FeistelNetwork.decrypt(block, 5L, 6L))
                .isEqualTo(FeistelNetwork.decrypt(block, 5L, 6L));
    }
}
