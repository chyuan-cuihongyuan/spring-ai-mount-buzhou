package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConstantTimeEqualsTest {

    @Test
    void shouldMatchEqualitySemanticsOnRandomPairs() {
        Random random = new Random(8037);
        for (int round = 0; round < 500; round++) {
            int length = random.nextInt(33);
            byte[] first = new byte[length];
            random.nextBytes(first);
            byte[] second;
            if (random.nextBoolean() && length > 0) {
                second = first.clone();
                second[random.nextInt(length)] ^= 1;
            } else {
                second = new byte[length];
                random.nextBytes(second);
            }
            assertThat(ConstantTimeEquals.equals(first, second))
                    .as("round=%d", round)
                    .isEqualTo(ConstantTimeEquals.referenceEquals(first, second));
        }
        byte[] same = {1, 2, 3};
        assertThat(ConstantTimeEquals.equals(same, same.clone())).isTrue();
        assertThat(ConstantTimeEquals.equals(new byte[0], new byte[0])).isTrue();
    }

    @Test
    void shouldReturnFalseOnLengthMismatch() {
        assertThat(ConstantTimeEquals.equals(new byte[]{1}, new byte[]{1, 2})).isFalse();
        assertThat(ConstantTimeEquals.equals(new byte[0], new byte[]{1})).isFalse();
    }

    @Test
    void shouldCompareHexAndFailFastOnNull() {
        assertThat(ConstantTimeEquals.equalsHex("DEADBEEF", "deadbeef")).isTrue();
        assertThat(ConstantTimeEquals.equalsHex("DEADBEEF", "DEADBEEC")).isFalse();
        assertThatThrownBy(() -> ConstantTimeEquals.equals(null, new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConstantTimeEquals.equals(new byte[0], null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConstantTimeEquals.equalsHex(null, "aa"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
