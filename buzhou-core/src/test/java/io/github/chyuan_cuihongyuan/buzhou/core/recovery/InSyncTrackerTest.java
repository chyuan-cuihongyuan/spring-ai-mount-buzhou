package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2037 / T3176：ISR 追踪合同——追上即同步、滞后剔除、追平回归、
 * 收缩计数（扩张不计）、注册序稳定、畸形 fail-fast。
 */
class InSyncTrackerTest {

    @Test
    void caughtUpMemberShouldStayInSyncWithinThreshold() {
        InSyncTracker tracker = new InSyncTracker(10_000L);
        tracker.register("a");
        tracker.caughtUp("a", 100_000);
        assertThat(tracker.isInSync("a", 105_000)).isTrue(); // 距追上 5s < 10s
        assertThat(tracker.isInSync("a", 110_000)).isFalse(); // 恰 10s 界外（< 语义）
    }

    @Test
    void laggingMemberShouldDropFromIsrAndReturn() {
        InSyncTracker tracker = new InSyncTracker(10_000L);
        tracker.register("a");
        tracker.register("b");
        tracker.caughtUp("a", 0);
        tracker.caughtUp("b", 0);
        // 双同步快照
        assertThat(tracker.inSyncSet(5_000)).containsExactlyInAnyOrder("a", "b");
        // a 滞后剔除、b 仍追
        tracker.caughtUp("b", 60_000);
        Set<String> shrunk = tracker.inSyncSet(60_000);
        assertThat(shrunk).containsExactly("b"); // a 掉出
        assertThat(tracker.shrinkEvents()).isEqualTo(1L); // 收缩计一次
        // a 追平回归（扩张不计缩）
        tracker.caughtUp("a", 61_000);
        assertThat(tracker.inSyncSet(61_000)).containsExactlyInAnyOrder("a", "b");
        assertThat(tracker.shrinkEvents()).isEqualTo(1L); // 回归不增缩计数
    }

    @Test
    void freshlyRegisteredMemberShouldBeInSyncAtStart() {
        InSyncTracker tracker = new InSyncTracker(10_000L);
        tracker.register("fresh");
        assertThat(tracker.isInSync("fresh", 5_000)).isTrue(); // lastCaughtUp=0 起算
        assertThat(tracker.isInSync("fresh", 15_000)).isFalse(); // 从未追上滞后即掉
    }

    @Test
    void caughtUpShouldNeverGoBackwards() {
        InSyncTracker tracker = new InSyncTracker(10_000L);
        tracker.register("a");
        tracker.caughtUp("a", 100_000);
        tracker.caughtUp("a", 50_000); // 迟到旧追平——不回拨
        assertThat(tracker.isInSync("a", 105_000)).isTrue(); // 仍以 100k 为准
    }

    @Test
    void unregisteredMemberShouldNotBeInSync() {
        InSyncTracker tracker = new InSyncTracker(10_000L);
        assertThat(tracker.isInSync("ghost", 0)).isFalse();
    }

    @Test
    void repeatedShrinksShouldAccumulate() {
        InSyncTracker tracker = new InSyncTracker(1_000L);
        tracker.register("a");
        tracker.register("b");
        tracker.inSyncSet(0); // 初始全同步（size 2）
        tracker.caughtUp("b", 0);
        tracker.inSyncSet(2_000); // a 掉出（1）——size 1
        tracker.caughtUp("a", 2_500);
        tracker.caughtUp("b", 2_500);
        tracker.inSyncSet(2_500); // 双回归 size 2
        tracker.inSyncSet(10_000); // 双掉出（2）——size 0
        assertThat(tracker.shrinkEvents()).isEqualTo(2L);
    }

    @Test
    void malformedInputsShouldFailFast() {
        InSyncTracker tracker = new InSyncTracker(1_000L);
        assertThatThrownBy(() -> new InSyncTracker(0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tracker.register(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tracker.register(" "))
                .isInstanceOf(IllegalArgumentException.class);
        tracker.register("a");
        assertThatThrownBy(() -> tracker.register("a"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已注册");
        assertThatThrownBy(() -> tracker.caughtUp("ghost", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tracker.caughtUp("a", -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tracker.isInSync("a", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
