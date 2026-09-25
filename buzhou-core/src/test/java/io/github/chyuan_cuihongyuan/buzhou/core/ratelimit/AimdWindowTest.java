package io.github.chyuan_cuihongyuan.buzhou.core.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 6041：AimdWindow 合同——成功加性增/失败乘性减锯齿。
 * 锯齿收敛形态钉住；钳制边界；参数校验 fail-fast。
 */
class AimdWindowTest {

    @Test
    void sawtoothConvergenceShape() {
        AimdWindow window = new AimdWindow(2, 20, 0.5);
        for (int i = 0; i < 18; i++) {
            window.onSuccess();
        }
        assertThat(window.current()).isEqualTo(20);
        window.onFailure();
        assertThat(window.current()).isEqualTo(10);
        for (int i = 0; i < 10; i++) {
            window.onSuccess();
        }
        assertThat(window.current()).isEqualTo(20);
        window.onFailure();
        assertThat(window.current()).isEqualTo(10);
    }

    @Test
    void decreaseRoundsUpAndClampsAtMin() {
        AimdWindow window = new AimdWindow(4, 100, 0.5);
        window.onFailure();
        assertThat(window.current()).as("3×0.5 向上取整钳 min").isEqualTo(4);
        window.onSuccess();
        assertThat(window.current()).isEqualTo(5);
        window.onFailure();
        assertThat(window.current()).as("2.5→ceil 3 被 min=4 钳住").isEqualTo(4);
    }

    @Test
    void increaseClampsAtMax() {
        AimdWindow window = new AimdWindow(1, 3, 0.5);
        window.onSuccess();
        window.onSuccess();
        window.onSuccess();
        window.onSuccess();
        window.onSuccess();
        assertThat(window.current()).isEqualTo(3);
    }

    @Test
    void deterministicSawtoothAcrossInstances() {
        AimdWindow a = new AimdWindow(2, 50, 0.5);
        AimdWindow b = new AimdWindow(2, 50, 0.5);
        boolean[] events = {true, true, false, true, true, true, false};
        for (boolean success : events) {
            if (success) {
                a.onSuccess();
                b.onSuccess();
            } else {
                a.onFailure();
                b.onFailure();
            }
            assertThat(b.current()).isEqualTo(a.current());
        }
        assertThat(a.decreaseFactor()).isEqualTo(0.5);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new AimdWindow(10, 1, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AimdWindow(0, 10, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AimdWindow(1, 10, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AimdWindow(1, 10, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
