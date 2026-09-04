package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 331 / impl-354：进程内选主——try 即 leader、续期纪元不变、
 * 让位即空位、空位再取纪元递增、inspect 观测、他人不动持有。
 */
class InMemoryLeaderElectorTest {

    @Test
    void tryAcquires_thenRenewsWithSameEpoch() {
        InMemoryLeaderElector elector = new InMemoryLeaderElector("a");
        LeaderElector.Leadership first = elector.tryAcquireOrRenew();
        assertThat(first.leader()).isTrue();
        assertThat(first.epoch()).isEqualTo(1L); // 首取纪元 1
        LeaderElector.Leadership renewed = elector.tryAcquireOrRenew();
        assertThat(renewed.leader()).isTrue();
        assertThat(renewed.epoch()).isEqualTo(1L); // 续期不增
    }

    @Test
    void resignVacates_thenReacquireBumpsEpoch() {
        InMemoryLeaderElector elector = new InMemoryLeaderElector("a");
        elector.tryAcquireOrRenew();
        elector.resign();
        LeaderElector.Leadership vacant = elector.inspect();
        assertThat(vacant.holder()).isNull();
        assertThat(vacant.leader()).isFalse();
        LeaderElector.Leadership reacquired = elector.tryAcquireOrRenew();
        assertThat(reacquired.leader()).isTrue();
        assertThat(reacquired.epoch()).isEqualTo(2L); // 空位再取递增
    }

    @Test
    void inspectReflectsHolderState() {
        InMemoryLeaderElector elector = new InMemoryLeaderElector("a");
        elector.tryAcquireOrRenew();
        LeaderElector.Leadership seen = elector.inspect();
        assertThat(seen.leader()).isTrue();
        assertThat(seen.holder()).isEqualTo("a");
        elector.resign();
        assertThat(elector.inspect().leader()).isFalse(); // 空位可观测
    }

    @Test
    void nonHolderResignIsNoOp() {
        InMemoryLeaderElector first = new InMemoryLeaderElector("a");
        InMemoryLeaderElector second = new InMemoryLeaderElector("b");
        first.tryAcquireOrRenew();
        second.resign(); // 非持有人让位无效
        assertThat(first.tryAcquireOrRenew().leader()).isTrue(); // 仍持有且纪元不变
        assertThat(first.tryAcquireOrRenew().epoch()).isEqualTo(1L);
    }

    @Test
    void blankHolderIdGetsGeneratedIdentity() {
        InMemoryLeaderElector elector = new InMemoryLeaderElector(null);
        assertThat(elector.inspect().holder()).isNull();
        LeaderElector.Leadership acquired = elector.tryAcquireOrRenew();
        assertThat(acquired.holder()).startsWith("in-memory-");
    }
}
