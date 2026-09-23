package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4009 / T6020：Crockford Base32 合同——往返极值、形近归一、
 * 校验符号、畸形 fail-fast。
 */
class CrockfordBase32Test {

    @Test
    void roundtripShouldCoverAnchorsAndExtremes() {
        assertThat(CrockfordBase32.encode(0)).isEqualTo("0");
        assertThat(CrockfordBase32.encode(31)).isEqualTo("Z");
        assertThat(CrockfordBase32.encode(32)).isEqualTo("10");
        assertThat(CrockfordBase32.decode("C1S")).isEqualTo(12345);
        assertThat(CrockfordBase32.decode(CrockfordBase32.encode(12345))).isEqualTo(12345);
        assertThat(CrockfordBase32.decode(CrockfordBase32.encode(Long.MAX_VALUE)))
                .isEqualTo(Long.MAX_VALUE);
        assertThat(CrockfordBase32.encode(Long.MAX_VALUE)).hasSize(13);
    }

    @Test
    void lookalikeCharactersShouldNormalize() {
        assertThat(CrockfordBase32.decode("o")).isZero();   // O→0
        assertThat(CrockfordBase32.decode("i")).isEqualTo(1);   // I→1
        assertThat(CrockfordBase32.decode("l")).isEqualTo(1);   // L→1
        assertThat(CrockfordBase32.decode("c1s")).isEqualTo(12345);   // 小写不敏感
        assertThat(CrockfordBase32.decode("C-1-S")).isEqualTo(12345);   // 连字符忽略
        assertThat(CrockfordBase32.decode("ciS")).isEqualTo(12345);   // 形近混写归一
    }

    @Test
    void checkSymbolShouldRoundtripAndCatchTypos() {
        String withCheck = CrockfordBase32.encodeWithCheck(12345);
        assertThat(CrockfordBase32.decodeWithCheck(withCheck)).isEqualTo(12345);
        assertThat(withCheck).hasSize(4);   // C1S + 校验符
        assertThat(CrockfordBase32.decodeWithCheck(
                CrockfordBase32.encodeWithCheck(Long.MAX_VALUE))).isEqualTo(Long.MAX_VALUE);
        // 尾符篡改（换成另一校验符）必被捕获
        String tampered = withCheck.substring(0, 3)
                + (withCheck.charAt(3) == '0' ? '1' : '0');
        assertThatThrownBy(() -> CrockfordBase32.decodeWithCheck(tampered))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> CrockfordBase32.encode(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrockfordBase32.decode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrockfordBase32.decode(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrockfordBase32.decode("U"))   // U 不在字母表
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrockfordBase32.decode("#"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrockfordBase32.decode("ZZZZZZZZZZZZZ"))   // 13×Z 溢出
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CrockfordBase32.decodeWithCheck("C"))   // 不足两位
                .isInstanceOf(IllegalArgumentException.class);
    }
}
