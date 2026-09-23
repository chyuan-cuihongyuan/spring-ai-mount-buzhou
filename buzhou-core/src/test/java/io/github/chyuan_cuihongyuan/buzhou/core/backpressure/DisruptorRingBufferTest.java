package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5008 / T6118：Disruptor 环合同——两段式时序、跨 wrap
 * 窗口语义、连续推进、环满上抛、非 2 幂/越权/null fail-fast。
 */
class DisruptorRingBufferTest {

    @Test
    void twoPhaseClaimPublishShouldGateConsumption() {
        DisruptorRingBuffer<String> ring = new DisruptorRingBuffer<>(4);
        long sequence = ring.claim();
        assertThat(sequence).isZero();
        assertThat(ring.tryConsume(sequence)).isNull();   // 未发布不可消费
        ring.publish(sequence, "payload");
        assertThat(ring.tryConsume(sequence)).isEqualTo("payload");
        assertThat(ring.published()).isEqualTo(1L);
        assertThat(ring.claimed()).isEqualTo(1L);
    }

    @Test
    void outOfOrderPublishShouldBeRejected() {
        DisruptorRingBuffer<String> ring = new DisruptorRingBuffer<>(4);
        ring.claim();
        ring.claim();
        assertThatThrownBy(() -> ring.publish(1, "skip"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("发布序");
        ring.publish(0, "first");   // 按序放行
    }

    @Test
    void fullRingWithoutConsumerShouldRefusePublish() {
        DisruptorRingBuffer<Integer> ring = new DisruptorRingBuffer<>(4);
        for (int i = 0; i < 4; i++) {
            ring.offer(i);
        }
        long sequence = ring.claim();
        assertThatThrownBy(() -> ring.publish(sequence, 4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("环满");
    }

    @Test
    void offerShouldOverwriteOldestWindowHonest() {
        DisruptorRingBuffer<Integer> ring = new DisruptorRingBuffer<>(4);
        for (int i = 0; i < 10; i++) {
            ring.offer(i);
        }
        assertThat(ring.published()).isEqualTo(10L);
        assertThat(ring.earliestAvailable()).isEqualTo(6L);   // 最近 4 条窗口
        assertThat(ring.tryConsume(9)).isEqualTo(9);
        assertThat(ring.tryConsume(6)).isEqualTo(6);
        assertThat(ring.tryConsume(2)).isNull();   // 已被覆盖——诚实缺口
        assertThat(ring.tryConsume(20)).isNull();  // 未发布
    }

    @Test
    void singleConsumerSequenceShouldReplayInOrder() {
        DisruptorRingBuffer<Integer> ring = new DisruptorRingBuffer<>(8);
        for (int i = 0; i < 8; i++) {
            ring.offer(i * 11);
        }
        for (long sequence = 0; sequence < 8; sequence++) {
            assertThat(ring.tryConsume(sequence)).isEqualTo((int) (sequence * 11));
        }
        assertThat(ring.tryConsume(8)).isNull();
    }

    @Test
    void invalidCapacityAndNullItemShouldFailFast() {
        assertThatThrownBy(() -> new DisruptorRingBuffer<>(3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DisruptorRingBuffer<>(0))
                .isInstanceOf(IllegalArgumentException.class);
        DisruptorRingBuffer<String> ring = new DisruptorRingBuffer<>(4);
        assertThatThrownBy(() -> ring.publish(0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ring.offer(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ring.publish(5, "beyond"))   // 越权（发布序外）
                .isInstanceOf(IllegalArgumentException.class);
    }
}
