package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 179 / T552：空闲水位回归——超阈进入（降序+通知）/ 活跃离开（通知）/
 * 无特征零误报 / 阈值边界 / 参数校验。
 */
class IdleSessionMonitorTest {

    @Test
    void idleBeyondThresholdListedDescendingWithFlipNotification() throws Exception {
        SessionFeatureStore features = new SessionFeatureStore();
        features.recordTurnStart("older");
        Thread.sleep(5);
        features.recordTurnStart("newer");

        List<String> flips = new CopyOnWriteArrayList<>();
        IdleSessionMonitor monitor = new IdleSessionMonitor(features, Duration.ofMinutes(30));
        monitor.onChange((session, entered) -> flips.add(session + ":" + entered));

        List<IdleSessionMonitor.IdleInfo> idle = monitor.sweep(
                Instant.now().plus(Duration.ofMinutes(31)));

        assertThat(idle).extracting(IdleSessionMonitor.IdleInfo::sessionId)
                .containsExactly("older", "newer"); // 空闲时长降序（older 更闲）
        assertThat(idle.get(0).idleMillis()).isGreaterThan(idle.get(1).idleMillis());
        assertThat(flips).containsExactly("older:true", "newer:true");
    }

    @Test
    void activityAfterIdleFlipsBackToActive() throws Exception {
        SessionFeatureStore features = new SessionFeatureStore();
        features.recordTurnStart("s1");

        List<String> flips = new CopyOnWriteArrayList<>();
        IdleSessionMonitor monitor = new IdleSessionMonitor(features, Duration.ofMinutes(30));
        monitor.onChange((session, entered) -> flips.add(session + ":" + entered));

        monitor.sweep(Instant.now().plus(Duration.ofMinutes(31))); // 进入空闲
        features.recordTurnStart("s1"); // 回来了
        List<IdleSessionMonitor.IdleInfo> after = monitor.sweep(Instant.now());

        assertThat(after).isEmpty();
        assertThat(flips).containsExactly("s1:true", "s1:false");
    }

    @Test
    void sessionsWithoutActivityFactsAreNotReported() {
        SessionFeatureStore features = new SessionFeatureStore();
        IdleSessionMonitor monitor = new IdleSessionMonitor(features, Duration.ofMinutes(1));

        // 从未见过任何活动的会话（不在特征仓）零误报
        assertThat(monitor.sweep(Instant.now().plus(Duration.ofHours(1)))).isEmpty();
        // 工具调用也是活动事实——只调过工具的会话同样可判空闲（lastActiveAt 口径统一）
        features.recordToolCall("toolOnly", false);
        assertThat(monitor.sweep(Instant.now().plus(Duration.ofHours(1))))
                .extracting(IdleSessionMonitor.IdleInfo::sessionId).containsExactly("toolOnly");
    }

    @Test
    void boundaryAtExactlyThresholdCountsAsIdle() {
        SessionFeatureStore features = new SessionFeatureStore();
        features.recordTurnStart("edge");
        Instant lastActive = features.features("edge").lastActiveAt();

        IdleSessionMonitor monitor = new IdleSessionMonitor(features, Duration.ofMinutes(30));
        assertThat(monitor.sweep(lastActive.plus(Duration.ofMinutes(30)))).hasSize(1); // =阈值即空闲
        assertThat(monitor.sweep(lastActive.plus(Duration.ofMinutes(29)))).isEmpty();
    }

    @Test
    void repeatedSweepWithoutChangeDoesNotRefire() {
        SessionFeatureStore features = new SessionFeatureStore();
        features.recordTurnStart("s1");

        List<String> flips = new CopyOnWriteArrayList<>();
        IdleSessionMonitor monitor = new IdleSessionMonitor(features, Duration.ofMinutes(1));
        monitor.onChange((session, entered) -> flips.add(session + ":" + entered));

        Instant far = Instant.now().plus(Duration.ofHours(1));
        monitor.sweep(far);
        monitor.sweep(far); // 仍空闲——不刷屏

        assertThat(flips).containsExactly("s1:true");
    }

    @Test
    void thresholdValidated() {
        assertThatThrownBy(() -> new IdleSessionMonitor(null, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IdleSessionMonitor(new SessionFeatureStore(),
                Duration.ofMinutes(-1))).isInstanceOf(IllegalArgumentException.class);
    }
}
