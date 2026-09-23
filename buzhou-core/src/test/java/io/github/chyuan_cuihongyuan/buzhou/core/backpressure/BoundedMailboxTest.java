package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5020 / T6142：Bounded Mailbox 合同——两策略分叉、FIFO、
 * 丢弃计数、畸形 fail-fast、确定性回放。
 */
class BoundedMailboxTest {

    @Test
    void dropNewestPolicyShouldRejectWhenFull() {
        BoundedMailbox<String> mailbox = new BoundedMailbox<>(2, BoundedMailbox.OverflowPolicy.DROP_NEWEST);
        assertThat(mailbox.offer("a")).isTrue();
        assertThat(mailbox.offer("b")).isTrue();
        assertThat(mailbox.offer("c")).isFalse();   // 满拒新
        assertThat(mailbox.droppedCount()).isEqualTo(1L);   // 丢弃可见
        assertThat(mailbox.poll()).isEqualTo("a");
        assertThat(mailbox.poll()).isEqualTo("b");
        assertThat(mailbox.poll()).isNull();
    }

    @Test
    void dropOldestPolicyShouldAdmitNewest() {
        BoundedMailbox<String> mailbox = new BoundedMailbox<>(2, BoundedMailbox.OverflowPolicy.DROP_OLDEST);
        assertThat(mailbox.offer("a")).isTrue();
        assertThat(mailbox.offer("b")).isTrue();
        assertThat(mailbox.offer("c")).isTrue();    // 逐 a 纳 c
        assertThat(mailbox.droppedCount()).isEqualTo(1L);
        assertThat(mailbox.poll()).isEqualTo("b");
        assertThat(mailbox.poll()).isEqualTo("c");
        assertThat(mailbox.poll()).isNull();
    }

    @Test
    void fifoOrderShouldHoldUnderCapacity() {
        BoundedMailbox<Integer> mailbox = new BoundedMailbox<>(5, BoundedMailbox.OverflowPolicy.DROP_NEWEST);
        for (int i = 0; i < 5; i++) {
            mailbox.offer(i);
        }
        for (int i = 0; i < 5; i++) {
            assertThat(mailbox.poll()).isEqualTo(i);
        }
        assertThat(mailbox.size()).isZero();
        assertThat(mailbox.droppedCount()).isZero();
    }

    @Test
    void invalidInputsShouldFailFast() {
        assertThatThrownBy(() -> new BoundedMailbox<>(0, BoundedMailbox.OverflowPolicy.DROP_NEWEST))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BoundedMailbox<>(2, null))
                .isInstanceOf(IllegalArgumentException.class);
        BoundedMailbox<String> mailbox = new BoundedMailbox<>(2, BoundedMailbox.OverflowPolicy.DROP_NEWEST);
        assertThatThrownBy(() -> mailbox.offer(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
