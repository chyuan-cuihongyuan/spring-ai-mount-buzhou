package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HammingCodeTest {

    @Test
    void shouldRoundTripAllSixteenValues() {
        for (int data = 0; data < 16; data++) {
            int code = HammingCode.encode(data);
            int decoded = HammingCode.decode(code);
            assertThat(decoded & 0x0F).as("data %d", data).isEqualTo(data);
            assertThat(decoded >> 4).isEqualTo(HammingCode.CLEAN);
        }
    }

    @Test
    void shouldCorrectEverySingleBitFlip() {
        for (int data = 0; data < 16; data++) {
            int code = HammingCode.encode(data);
            for (int bit = 0; bit < 8; bit++) {
                int flipped = code ^ (1 << bit);
                int decoded = HammingCode.decode(flipped);
                assertThat(decoded >> 4).as("data %d flip %d", data, bit)
                        .isEqualTo(HammingCode.CORRECTED);
                assertThat(decoded & 0x0F).as("data %d flip %d", data, bit).isEqualTo(data);
            }
        }
    }

    @Test
    void shouldDetectEveryDoubleBitFlip() {
        for (int data = 0; data < 16; data++) {
            int code = HammingCode.encode(data);
            for (int first = 0; first < 8; first++) {
                for (int second = first + 1; second < 8; second++) {
                    int flipped = code ^ (1 << first) ^ (1 << second);
                    int decoded = HammingCode.decode(flipped);
                    assertThat(decoded >> 4).as("data %d flips %d/%d", data, first, second)
                            .isEqualTo(HammingCode.DOUBLE_ERROR);
                }
            }
        }
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        assertThat(HammingCode.encode(9)).isEqualTo(HammingCode.encode(9));
        assertThatThrownBy(() -> HammingCode.encode(16))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HammingCode.encode(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HammingCode.decode(256))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> HammingCode.decode(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
