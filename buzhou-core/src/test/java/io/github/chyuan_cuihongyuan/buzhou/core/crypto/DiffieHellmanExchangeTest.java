package io.github.chyuan_cuihongyuan.buzhou.core.crypto;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiffieHellmanExchangeTest {

    @Test
    void shouldAgreeOnSharedSecretBothWays() {
        BigInteger p = DiffieHellmanExchange.TEACHING_SAFE_PRIME_128;
        BigInteger g = DiffieHellmanExchange.DEFAULT_G;
        Random random = new Random(21);
        BigInteger a = DiffieHellmanExchange.generatePrivateKey(p, random);
        BigInteger b = DiffieHellmanExchange.generatePrivateKey(p, random);
        BigInteger pubA = DiffieHellmanExchange.publicKey(p, g, a);
        BigInteger pubB = DiffieHellmanExchange.publicKey(p, g, b);
        // 公钥不泄私钥（值域分离圣像：公钥 ≠ 私钥、两侧公钥互异）
        assertThat(pubA).isNotEqualTo(a);
        assertThat(pubA).isNotEqualTo(pubB);
        // 双向同达 g^(ab)
        BigInteger secretAtA = DiffieHellmanExchange.sharedSecret(p, pubB, a);
        BigInteger secretAtB = DiffieHellmanExchange.sharedSecret(p, pubA, b);
        assertThat(secretAtA).isEqualTo(secretAtB);
        assertThat(secretAtA.signum()).isGreaterThan(0);
        assertThat(secretAtA.compareTo(p)).isLessThan(0);
        // 多轮交换各自独立成对
        for (int t = 0; t < 20; t++) {
            BigInteger ka = DiffieHellmanExchange.generatePrivateKey(p, random);
            BigInteger kb = DiffieHellmanExchange.generatePrivateKey(p, random);
            assertThat(DiffieHellmanExchange.sharedSecret(p,
                    DiffieHellmanExchange.publicKey(p, g, kb), ka))
                    .isEqualTo(DiffieHellmanExchange.sharedSecret(p,
                            DiffieHellmanExchange.publicKey(p, g, ka), kb));
        }
    }

    @Test
    void shouldCarrySmallPrimeAnchor() {
        // 小素数手锚：p=23, g=5, a=6, b=15（教科书经典例）——pubA=5^6 mod 23=8，pubB=5^15 mod 23=19，
        // secret=8^15 mod 23=19^6 mod 23=2
        BigInteger p = BigInteger.valueOf(23);
        BigInteger g = BigInteger.valueOf(5);
        assertThat(DiffieHellmanExchange.publicKey(p, g, BigInteger.valueOf(6)))
                .isEqualTo(BigInteger.valueOf(8));
        assertThat(DiffieHellmanExchange.publicKey(p, g, BigInteger.valueOf(15)))
                .isEqualTo(BigInteger.valueOf(19));
        assertThat(DiffieHellmanExchange.sharedSecret(p, BigInteger.valueOf(19), BigInteger.valueOf(6)))
                .isEqualTo(BigInteger.valueOf(2));
        assertThat(DiffieHellmanExchange.sharedSecret(p, BigInteger.valueOf(8), BigInteger.valueOf(15)))
                .isEqualTo(BigInteger.valueOf(2));
    }

    @Test
    void shouldBeFailFast() {
        BigInteger p = BigInteger.valueOf(23);
        BigInteger g = BigInteger.valueOf(5);
        // p 非素
        assertThatThrownBy(() -> DiffieHellmanExchange.publicKey(BigInteger.valueOf(21), g,
                BigInteger.valueOf(4))).hasMessageContaining("素数");
        // g 越域
        assertThatThrownBy(() -> DiffieHellmanExchange.publicKey(p, BigInteger.ONE,
                BigInteger.valueOf(4))).hasMessageContaining("生成元");
        assertThatThrownBy(() -> DiffieHellmanExchange.publicKey(p, p,
                BigInteger.valueOf(4))).hasMessageContaining("生成元");
        // 私钥越域
        assertThatThrownBy(() -> DiffieHellmanExchange.publicKey(p, g, BigInteger.ONE))
                .hasMessageContaining("私钥");
        // 对方公钥越域
        assertThatThrownBy(() -> DiffieHellmanExchange.sharedSecret(p, p, BigInteger.valueOf(4)))
                .hasMessageContaining("公钥");
        assertThatThrownBy(() -> DiffieHellmanExchange.sharedSecret(p, BigInteger.ZERO,
                BigInteger.valueOf(4))).hasMessageContaining("公钥");
        assertThatThrownBy(() -> DiffieHellmanExchange.generatePrivateKey(BigInteger.valueOf(4),
                new Random())).isInstanceOf(IllegalArgumentException.class);
    }
}
