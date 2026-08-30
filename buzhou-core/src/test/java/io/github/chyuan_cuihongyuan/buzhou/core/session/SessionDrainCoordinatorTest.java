package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouException;
import io.github.chyuan_cuihongyuan.buzhou.core.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 155 / T514：排水回归——未排不拒 / 排后拒新 / 排空即真 / 超时不死等 /
 * 多 Lease 全还才排空 / 名单与计数。
 */
class SessionDrainCoordinatorTest {

    @Test
    void enterSucceedsBeforeDrainAndRejectsAfter() {
        SessionDrainCoordinator drain = new SessionDrainCoordinator();
        try (SessionDrainCoordinator.Lease lease = drain.enter("s1")) {
            assertThat(drain.inFlightOf("s1")).isEqualTo(1);
        }
        assertThat(drain.inFlightOf("s1")).isZero();

        drain.beginDrain("s1");
        assertThat(drain.isDraining("s1")).isTrue();
        assertThatThrownBy(() -> drain.enter("s1"))
                .isInstanceOf(BuzhouException.class)
                .hasMessageContaining("排水维护中")
                .satisfies(e -> assertThat(((BuzhouException) e).errorCode())
                        .isEqualTo(ErrorCode.SESSION_DRAINING));
    }

    @Test
    void inFlightLeaseCompletingSignalsDrained() throws Exception {
        SessionDrainCoordinator drain = new SessionDrainCoordinator();
        SessionDrainCoordinator.Lease lease = drain.enter("s1");
        drain.beginDrain("s1");

        // 有在飞：短预算等不到
        assertThat(drain.awaitDrained("s1", Duration.ofMillis(50))).isFalse();

        lease.close();
        assertThat(drain.awaitDrained("s1", Duration.ofSeconds(2))).isTrue();
    }

    @Test
    void drainWithNoInFlightIsImmediatelyDrained() throws Exception {
        SessionDrainCoordinator drain = new SessionDrainCoordinator();
        drain.beginDrain("s1");
        assertThat(drain.awaitDrained("s1", Duration.ofMillis(10))).isTrue();
    }

    @Test
    void multipleLeasesAllMustClose() throws Exception {
        SessionDrainCoordinator drain = new SessionDrainCoordinator();
        SessionDrainCoordinator.Lease one = drain.enter("s1");
        SessionDrainCoordinator.Lease two = drain.enter("s1");
        drain.beginDrain("s1");

        one.close();
        assertThat(drain.awaitDrained("s1", Duration.ofMillis(50))).isFalse();
        two.close();
        assertThat(drain.awaitDrained("s1", Duration.ofSeconds(2))).isTrue();
    }

    @Test
    void beginDrainIsIdempotent() {
        SessionDrainCoordinator drain = new SessionDrainCoordinator();
        drain.beginDrain("s1");
        assertThatCode(() -> drain.beginDrain("s1")).doesNotThrowAnyException();
        assertThatThrownBy(() -> drain.enter("s1")).isInstanceOf(BuzhouException.class);
    }

    @Test
    void sessionsAreIsolatedAndListed() {
        SessionDrainCoordinator drain = new SessionDrainCoordinator();
        drain.beginDrain("a");
        try (SessionDrainCoordinator.Lease lease = drain.enter("b")) {
            assertThat(drain.isDraining("b")).isFalse();
            assertThat(drain.drainingSessions()).containsExactly("a");
        }
    }
}
