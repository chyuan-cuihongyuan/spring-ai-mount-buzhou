package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base58CodecTest {

    @Test
    void shouldMatchBitcoinVectors() {
        assertThat(Base58Codec.encode(new byte[0])).isEmpty();
        assertThat(Base58Codec.encode(new byte[]{0})).isEqualTo("1");
        assertThat(Base58Codec.encode(new byte[]{0, 0, 1})).isEqualTo("112");
        assertThat(Base58Codec.encode(new byte[]{0x61})).isEqualTo("2g");
        assertThat(Base58Codec.encode("hello".getBytes())).isEqualTo("Cn8eVZg");
        assertThat(Base58Codec.encode("hello world".getBytes())).isEqualTo("StV1DL6CwTryKyV");
        assertThat(Base58Codec.decode("1")).isEqualTo(new byte[]{0});
        assertThat(Base58Codec.decode("112")).isEqualTo(new byte[]{0, 0, 1});
        assertThat(Base58Codec.decode("2g")).isEqualTo(new byte[]{0x61});
        assertThat(Base58Codec.decode("")).isEmpty();
    }

    @Test
    void shouldRoundTripOnRandomBytes() {
        Random random = new Random(8016);
        for (int round = 0; round < 300; round++) {
            int length = random.nextInt(40);
            byte[] data = new byte[length];
            for (int i = 0; i < length; i++) {
                data[i] = (byte) random.nextInt(256);
            }
            for (int z = 0; z < Math.min(3, length); z++) {
                data[z] = 0;
            }
            String encoded = Base58Codec.encode(data);
            assertThat(Base58Codec.decode(encoded))
                    .as("round=%d", round)
                    .isEqualTo(data);
        }
    }

    @Test
    void shouldFailFastOnNullAndIllegalChar() {
        assertThatThrownBy(() -> Base58Codec.encode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Base58Codec.decode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Base58Codec.decode("2@g"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("位置 1");
        assertThatThrownBy(() -> Base58Codec.decode("0OIl"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
