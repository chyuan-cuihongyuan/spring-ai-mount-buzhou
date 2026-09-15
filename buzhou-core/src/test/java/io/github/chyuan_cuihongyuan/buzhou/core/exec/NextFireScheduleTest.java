package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1870 / T2942：网格触发——含上、停机不重排、错过补账。 */
class NextFireScheduleTest {

    /** 网格语义：0 起 100 间隔，now=250 → 下次 300；恰在格点 200 → 即到期。 */
    @Test
    void nextFireSnapsToGridInclusive() {
        assertThat(NextFireSchedule.nextFireMillis(100, 0, 250)).isEqualTo(300L);
        assertThat(NextFireSchedule.nextFireMillis(100, 0, 200)).isEqualTo(200L);
        assertThat(NextFireSchedule.nextFireMillis(100, 0, 201)).isEqualTo(300L);
        // now 在 epoch 前或恰在 epoch → 首触发即 epoch
        assertThat(NextFireSchedule.nextFireMillis(100, 1000, 500)).isEqualTo(1000L);
        assertThat(NextFireSchedule.nextFireMillis(100, 1000, 1000)).isEqualTo(1000L);
    }

    /** 错过补账：上次确认 100，now 450 → (100,450] 内格点 200/300/400 = 3。 */
    @Test
    void missedFiresCountGridPoints() {
        assertThat(NextFireSchedule.missedFires(100, 0, 100, 450)).isEqualTo(3L);
        // 无错过：上次确认 400，now 450 → 0
        assertThat(NextFireSchedule.missedFires(100, 0, 400, 450)).isZero();
        // 首格起全算（lastAcked=0 前哨，epoch 300，now 550）：格点 300/400/500=3
        assertThat(NextFireSchedule.missedFires(100, 300, 0, 550)).isEqualTo(3L);
        // now 未到 epoch → 0
        assertThat(NextFireSchedule.missedFires(100, 300, 0, 100)).isZero();
    }

    /** 畸形入参 fail-fast：间隔 < 1、负时点、确认越界。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> NextFireSchedule.nextFireMillis(0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intervalMillis 不能小于 1");
        assertThatThrownBy(() -> NextFireSchedule.nextFireMillis(100, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NextFireSchedule.missedFires(100, 0, 500, 450))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法上次确认");
    }
}
