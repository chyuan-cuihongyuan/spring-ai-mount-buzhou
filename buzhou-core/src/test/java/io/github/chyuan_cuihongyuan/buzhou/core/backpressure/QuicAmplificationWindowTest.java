package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4019 / T6040：QUIC 反放大窗合同——三倍信用、累积扣减、
 * 验证解除、初始授信、畸形 fail-fast。
 */
class QuicAmplificationWindowTest {

    @Test
    void creditShouldBeFactorTimesReceived() {
        QuicAmplificationWindow window = new QuicAmplificationWindow(3, 0);
        window.onReceived(1000);
        assertThat(window.credit()).isEqualTo(3000);
        assertThat(window.canSend(3000)).isTrue();
        assertThat(window.canSend(3001)).isFalse();   // 恰 3 倍为界
        window.onSent(2999);
        assertThat(window.credit()).isEqualTo(1);
    }

    @Test
    void creditShouldAccumulateAcrossReceives() {
        QuicAmplificationWindow window = new QuicAmplificationWindow(3, 0);
        window.onReceived(100);
        window.onReceived(50);
        assertThat(window.credit()).isEqualTo(450);
        window.onSent(200);
        assertThat(window.credit()).isEqualTo(250);
        assertThat(window.canSend(250)).isTrue();
        assertThat(window.canSend(251)).isFalse();
    }

    @Test
    void oversendShouldFailFast() {
        QuicAmplificationWindow window = new QuicAmplificationWindow(3, 0);
        window.onReceived(100);
        assertThatThrownBy(() -> window.onSent(301))
                .isInstanceOf(IllegalStateException.class);   // MUST NOT 放大
        window.onSent(300);   // 恰尽
        assertThatThrownBy(() -> window.onSent(1))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validationShouldLiftWindow() {
        QuicAmplificationWindow window = new QuicAmplificationWindow(3, 0);
        assertThat(window.validated()).isFalse();
        window.validateAddress();
        assertThat(window.validated()).isTrue();
        assertThat(window.canSend(Long.MAX_VALUE / 2)).isTrue();   // 解除后全速
        window.onSent(1_000_000);   // 不再扣减受限
        assertThat(window.credit()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void initialCreditShouldCoverHandshakeAndFailFastOnBadInput() {
        QuicAmplificationWindow window = new QuicAmplificationWindow(3, 1200);   // 握手首包
        assertThat(window.canSend(1200)).isTrue();
        window.onSent(1200);
        assertThat(window.canSend(1)).isFalse();   // 未收包前不再可发
        assertThatThrownBy(() -> new QuicAmplificationWindow(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuicAmplificationWindow(3, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> window.onReceived(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> window.onSent(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
