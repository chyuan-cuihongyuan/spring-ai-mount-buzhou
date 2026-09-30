package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class MillerRabinPrimalityTest {

    @Test
    void shouldMatchKnownPrimesAndComposites() {
        // 小素数
        for (long p : new long[]{2, 3, 5, 7, 11, 13, 97, 7919, 104729, 2147483647L}) {
            assertThat(MillerRabinPrimality.isPrime(p)).as("素数 %d", p).isTrue();
        }
        // 边界与非素
        assertThat(MillerRabinPrimality.isPrime(0)).isFalse();
        assertThat(MillerRabinPrimality.isPrime(1)).isFalse();
        assertThat(MillerRabinPrimality.isPrime(-7)).isFalse();
        for (long c : new long[]{4, 6, 9, 15, 100, 1000000, 2147483648L}) {
            assertThat(MillerRabinPrimality.isPrime(c)).as("合数 %d", c).isFalse();
        }
        // 素数平方
        assertThat(MillerRabinPrimality.isPrime(1013L * 1013L)).isFalse();
    }

    @Test
    void shouldNotBeFooledByCarmichaelAndPseudoprimes() {
        // Carmichael 数：对任意基的 Fermat 检测全真——强伪素判定必识破
        for (long carmichael : new long[]{561, 1105, 1729, 2465, 2821, 6601, 41041, 825265, 512461}) {
            assertThat(MillerRabinPrimality.isPrime(carmichael))
                    .as("Carmichael %d 必判合数", carmichael).isFalse();
        }
        // 底 2 Fermat 伪素（341=11·31 是最小 2-PRP）
        assertThat(MillerRabinPrimality.isPrime(341)).isFalse();
        assertThat(MillerRabinPrimality.isPrime(2047)).isFalse();
        // 大 Carmichael（long 域内知名）
        assertThat(MillerRabinPrimality.isPrime(101101L)).isFalse(); // 101101 = 7·11·13·101
    }

    @Test
    void shouldAgreeWithBigIntegerOnRandomLongs() {
        // 互证圣像：随机正 long 与 BigInteger.isProbablePrime(50) 全等
        Random random = new Random(59);
        for (int t = 0; t < 400; t++) {
            long n;
            if (t % 4 == 0) {
                n = random.nextLong(1, 1_000_000); // 小域密集
            } else if (t % 4 == 1) {
                n = random.nextLong(1, Long.MAX_VALUE / 2); // 中域
            } else {
                n = random.nextLong(Long.MAX_VALUE / 2, Long.MAX_VALUE); // 大域贴近上界
            }
            boolean expected = BigInteger.valueOf(n).isProbablePrime(50);
            assertThat(MillerRabinPrimality.isPrime(n)).as("随机 %d（%d）", t, n).isEqualTo(expected);
        }
        // 确定性双跑
        assertThat(MillerRabinPrimality.isPrime(9223372036854775783L)).isTrue(); // long 域最大素数
        assertThat(MillerRabinPrimality.isPrime(9223372036854775783L)).isTrue();
    }
}
