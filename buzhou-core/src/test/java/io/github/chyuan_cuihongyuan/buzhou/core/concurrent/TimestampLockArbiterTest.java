package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.TimestampLockArbiter.Decision;
import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.TimestampLockArbiter.Mode;
import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.TimestampLockArbiter.Outcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3040 / T5082：时间戳锁合同——空闲即授、wait-die 老等少死、
 * wound-wait 老伤少等、释放再授、幂等重授、同钟并列 txnId 决、
 * 持有判定。
 */
class TimestampLockArbiterTest {

    @Test
    void freeResourceShouldGrantImmediately() {
        TimestampLockArbiter arbiter = new TimestampLockArbiter(Mode.WAIT_DIE);
        assertThat(arbiter.request(1, 100, "r").decision()).isEqualTo(Decision.GRANTED);
        assertThat(arbiter.holderOf("r")).isEqualTo(1);
        assertThat(arbiter.holds(1, "r")).isTrue();
    }

    @Test
    void waitDieYoungRequesterShouldDie() {
        TimestampLockArbiter arbiter = new TimestampLockArbiter(Mode.WAIT_DIE);
        arbiter.request(1, 100, "r");   // 老者 ts=100 持锁
        Outcome young = arbiter.request(2, 200, "r");   // 少者 ts=200
        assertThat(young.decision()).isEqualTo(Decision.REQUESTER_ABORTS);
        assertThat(young.woundedTxn()).isEqualTo(2);
        assertThat(arbiter.holderOf("r")).isEqualTo(1);   // 持有者不动
    }

    @Test
    void waitDieOldRequesterShouldWait() {
        TimestampLockArbiter arbiter = new TimestampLockArbiter(Mode.WAIT_DIE);
        arbiter.request(9, 200, "r");   // 少者先持
        Outcome old = arbiter.request(5, 100, "r");       // 老者请求
        assertThat(old.decision()).isEqualTo(Decision.WAIT);
        assertThat(arbiter.holderOf("r")).isEqualTo(9);
    }

    @Test
    void woundWaitOldRequesterShouldWoundHolder() {
        TimestampLockArbiter arbiter = new TimestampLockArbiter(Mode.WOUND_WAIT);
        arbiter.request(9, 200, "r");   // 少者先持
        Outcome old = arbiter.request(5, 100, "r");       // 老者夺锁
        assertThat(old.decision()).isEqualTo(Decision.GRANTED);
        assertThat(old.woundedTxn()).isEqualTo(9);        // 持有者被伤
        assertThat(arbiter.holderOf("r")).isEqualTo(5);
        // 被伤者（视为重启后的新时间戳）再请求只能等
        assertThat(arbiter.request(9, 300, "r").decision()).isEqualTo(Decision.WAIT);
    }

    @Test
    void woundWaitYoungRequesterShouldWait() {
        TimestampLockArbiter arbiter = new TimestampLockArbiter(Mode.WOUND_WAIT);
        arbiter.request(1, 100, "r");
        Outcome young = arbiter.request(2, 200, "r");
        assertThat(young.decision()).isEqualTo(Decision.WAIT);
        assertThat(young.woundedTxn()).isEqualTo(-1);
        assertThat(arbiter.holderOf("r")).isEqualTo(1);
    }

    @Test
    void releaseShouldFreeAndRegrant() {
        TimestampLockArbiter arbiter = new TimestampLockArbiter(Mode.WAIT_DIE);
        arbiter.request(1, 100, "r");
        arbiter.release(2, "r");   // 非持有者释放无副作用
        assertThat(arbiter.holderOf("r")).isEqualTo(1);
        arbiter.release(1, "r");
        assertThat(arbiter.holderOf("r")).isEqualTo(-1);
        assertThat(arbiter.request(2, 300, "r").decision()).isEqualTo(Decision.GRANTED);
    }

    @Test
    void holderReRequestShouldBeIdempotent() {
        TimestampLockArbiter arbiter = new TimestampLockArbiter(Mode.WOUND_WAIT);
        arbiter.request(7, 50, "r");
        assertThat(arbiter.request(7, 50, "r").decision()).isEqualTo(Decision.GRANTED);
        assertThat(arbiter.request(7, 99, "r").decision()).isEqualTo(Decision.GRANTED);  // 同 txn 幂等
    }

    @Test
    void sameTimestampShouldBreakTieByTxnId() {
        TimestampLockArbiter arbiter = new TimestampLockArbiter(Mode.WAIT_DIE);
        arbiter.request(10, 100, "r");                     // 持有 txn=10
        assertThat(arbiter.request(3, 100, "r").decision()).isEqualTo(Decision.WAIT);     // txn 小者老
        assertThat(arbiter.request(20, 100, "r").decision()).isEqualTo(Decision.REQUESTER_ABORTS);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new TimestampLockArbiter(null))
                .isInstanceOf(IllegalArgumentException.class);
        TimestampLockArbiter arbiter = new TimestampLockArbiter(Mode.WAIT_DIE);
        assertThatThrownBy(() -> arbiter.request(1, 1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
