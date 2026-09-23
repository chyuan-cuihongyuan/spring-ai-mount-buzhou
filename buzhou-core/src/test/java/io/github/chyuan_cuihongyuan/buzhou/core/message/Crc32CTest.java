package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4007 / T6016：CRC-32C 合同——标准检验向量、区间一致性、
 * 到达校验、畸形 fail-fast。
 */
class Crc32CTest {

    @Test
    void knownVectorsShouldMatch() {
        assertThat(Crc32C.compute("123456789".getBytes(StandardCharsets.US_ASCII)))
                .isEqualTo(0xE3069283L);   // CRC-32C 标准检验值
        assertThat(Crc32C.compute("a".getBytes(StandardCharsets.US_ASCII)))
                .isEqualTo(0xC1D04330L);
        assertThat(Crc32C.compute("abc".getBytes(StandardCharsets.US_ASCII)))
                .isEqualTo(0x364B3FB7L);
        assertThat(Crc32C.compute(new byte[0])).isZero();   // 空 = init^xorout
        assertThat(Crc32C.compute(new byte[32])).isEqualTo(0x8A9136AAL);   // 32×0x00
        assertThat(Crc32C.compute(fill((byte) 0xFF, 32))).isEqualTo(0x62A8AB43L);   // 32×0xFF
    }

    @Test
    void sliceShouldEqualFullComputation() {
        byte[] data = new byte[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        long full = Crc32C.compute(data);
        assertThat(Crc32C.compute(data, 0, data.length)).isEqualTo(full);
        assertThat(Crc32C.compute(data, 10, 80))
                .isEqualTo(Crc32C.compute(java.util.Arrays.copyOfRange(data, 10, 90)));
    }

    @Test
    void singleBitFlipShouldChangeChecksum() {
        byte[] data = "transfer-payload".getBytes(StandardCharsets.US_ASCII);
        long expected = Crc32C.compute(data);
        assertThat(Crc32C.verify(data, expected)).isTrue();
        data[3] ^= 0x01;   // 单比特翻转
        assertThat(Crc32C.verify(data, expected)).isFalse();
        assertThat(Crc32C.compute(data)).isNotEqualTo(expected);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> Crc32C.compute(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Crc32C.compute(new byte[4], 1, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Crc32C.compute(new byte[4], -1, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] fill(byte v, int n) {
        byte[] out = new byte[n];
        java.util.Arrays.fill(out, v);
        return out;
    }
}
