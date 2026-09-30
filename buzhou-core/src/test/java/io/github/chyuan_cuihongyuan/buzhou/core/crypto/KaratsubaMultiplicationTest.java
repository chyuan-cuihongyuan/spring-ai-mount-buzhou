package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KaratsubaMultiplicationTest {

    @Test
    void shouldMatchClassicAnchors() {
        assertThat(KaratsubaMultiplication.multiply(
                BigInteger.valueOf(1234), BigInteger.valueOf(5678))).isEqualTo(BigInteger.valueOf(7006652));
        assertThat(KaratsubaMultiplication.multiply(
                BigInteger.valueOf(999), BigInteger.valueOf(999))).isEqualTo(BigInteger.valueOf(998001));
        // 符号四象限
        long a = 987654321L;
        long b = 123456789L;
        assertThat(KaratsubaMultiplication.multiply(
                BigInteger.valueOf(-a), BigInteger.valueOf(b))).isEqualTo(BigInteger.valueOf(-a * b));
        assertThat(KaratsubaMultiplication.multiply(
                BigInteger.valueOf(-a), BigInteger.valueOf(-b))).isEqualTo(BigInteger.valueOf(a * b));
        // 零
        assertThat(KaratsubaMultiplication.multiply(BigInteger.ZERO, BigInteger.TEN)).isZero();
    }

    @Test
    void shouldAgreeWithBigIntegerOnLargeOperands() {
        // 互证圣像：2048/4096 bit 大数与 BigInteger.multiply 恒等（递归层实际触发）
        Random random = new Random(67);
        for (int bits : new int[]{2048, 4096, 8192}) {
            for (int t = 0; t < 10; t++) {
                BigInteger x = new BigInteger(bits, random).setBit(bits - 1);
                BigInteger y = new BigInteger(bits, random).setBit(bits - 1);
                assertThat(KaratsubaMultiplication.multiply(x, y))
                        .as("%d bit 第 %d 组", bits, t).isEqualTo(x.multiply(y));
            }
        }
        // 不对称位宽（分裂取 min 半位——交叉位宽圣像）
        BigInteger small = new BigInteger(1536, random);
        BigInteger large = new BigInteger(6000, random);
        assertThat(KaratsubaMultiplication.multiply(small, large)).isEqualTo(small.multiply(large));
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        BigInteger x = new BigInteger(3000, new Random(3));
        BigInteger y = new BigInteger(3000, new Random(4));
        assertThat(KaratsubaMultiplication.multiply(x, y))
                .isEqualTo(KaratsubaMultiplication.multiply(x, y));
        assertThatThrownBy(() -> KaratsubaMultiplication.multiply(null, y))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> KaratsubaMultiplication.multiply(x, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
