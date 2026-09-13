package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-674 / spec 921：TurnDeadline 软截止窗口读法——窗内判真、窗外/已到期/
 * 哨兵判假、softDeadlineAt 时刻正确且哨兵 empty、非法参数 fail-fast、既有零回归。
 */
class SoftWindowTest {

    @Test
    void withinSoftWindowBoundarySemantics() {
        // 预算 10 分钟、软窗 2 分钟：截止前 9 分钟（未进窗）/ 前 1 分钟（进窗）
        TurnDeadline far = TurnDeadline.at(Instant.now().plus(Duration.ofMinutes(9)));
        TurnDeadline near = TurnDeadline.at(Instant.now().plus(Duration.ofMinutes(1)));
        Duration softWindow = Duration.ofMinutes(2);

        assertThat(far.withinSoftWindow(softWindow)).isFalse();
        assertThat(near.withinSoftWindow(softWindow)).isTrue();
        // 已到期（remaining=0）不算软窗——硬截止语义归 isExpired
        TurnDeadline expired = TurnDeadline.at(Instant.now().minusSeconds(1));
        assertThat(expired.withinSoftWindow(softWindow)).isFalse();
        assertThat(expired.isExpired()).isTrue();
        // 哨兵恒 false
        assertThat(TurnDeadline.none().withinSoftWindow(softWindow)).isFalse();
    }

    @Test
    void softDeadlineAtComputesAndSentinelEmpty() {
        Instant deadline = Instant.now().plus(Duration.ofMinutes(10));
        TurnDeadline td = TurnDeadline.at(deadline);
        assertThat(td.softDeadlineAt(Duration.ofMinutes(2)))
                .contains(deadline.minus(Duration.ofMinutes(2)));
        assertThat(TurnDeadline.none().softDeadlineAt(Duration.ofMinutes(2)))
                .isEmpty();
    }

    @Test
    void argsValidated() {
        TurnDeadline td = TurnDeadline.in(Duration.ofMinutes(5));
        assertThatThrownBy(() -> td.withinSoftWindow(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> td.withinSoftWindow(Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> td.softDeadlineAt(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void existingSemanticsUnchanged() {
        // 既有面零回归：min 组合器取更紧预算、哨兵 remaining、isNone
        TurnDeadline bounded = TurnDeadline.in(Duration.ofMinutes(5));
        assertThat(bounded.min(TurnDeadline.none())).isEqualTo(bounded);
        assertThat(TurnDeadline.none().remaining()).isEqualTo(TurnDeadline.UNBOUNDED_REMAINING);
        assertThat(TurnDeadline.none().isNone()).isTrue();
    }
}
