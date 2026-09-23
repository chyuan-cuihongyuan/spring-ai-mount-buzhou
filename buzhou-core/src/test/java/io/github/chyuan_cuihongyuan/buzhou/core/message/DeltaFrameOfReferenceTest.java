package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.message.DeltaFrameOfReference.Encoded;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4008 / T6018：增量基准帧合同——单调流压缩、跳变重置、
 * roundtrip 全域、畸形 fail-fast。
 */
class DeltaFrameOfReferenceTest {

    @Test
    void monotonicStreamShouldCompressAgainstRawVarint() {
        long[] timestamps = new long[1000];
        for (int i = 0; i < timestamps.length; i++) {
            timestamps[i] = 1_700_000_000_000L + i * 3L;   // 毫秒时间戳近单调
        }
        DeltaFrameOfReference codec = new DeltaFrameOfReference(128);
        Encoded encoded = codec.encode(timestamps);
        int rawVarintBytes = 0;
        for (long v : timestamps) {
            rawVarintBytes += VarintCodec.encode(v).length;
        }
        assertThat(encoded.bytes().length).isLessThan(rawVarintBytes / 3);   // 增量后 1–2 字节/值
        assertThat(codec.decode(encoded.bytes(), encoded.count())).isEqualTo(timestamps);
    }

    @Test
    void jumpShouldResetAtFrameBoundary() {
        // 两帧：0..4 与 10^12..10^12+4——跳变只付一次基准代价
        long[] values = {1, 2, 3, 4, 5, 1_000_000_000_000L, 1_000_000_000_001L};
        DeltaFrameOfReference codec = new DeltaFrameOfReference(5);
        Encoded encoded = codec.encode(values);
        assertThat(codec.decode(encoded.bytes(), encoded.count())).isEqualTo(values);
    }

    @Test
    void roundtripShouldCoverNegativeAndChaoticValues() {
        long[] values = {-5, 3, -2, 0, Long.MAX_VALUE / 2, -Long.MAX_VALUE / 3, 7, 7, 7};
        DeltaFrameOfReference codec = new DeltaFrameOfReference(4);
        Encoded encoded = codec.encode(values);
        assertThat(codec.decode(encoded.bytes(), encoded.count())).isEqualTo(values);
        assertThat(codec.frameSize()).isEqualTo(4);
        assertThat(Arrays.copyOfRange(codec.decode(encoded.bytes(), encoded.count()), 0, 2))
                .containsExactly(-5L, 3L);
    }

    @Test
    void degenerateAndEmptyShouldBehave() {
        DeltaFrameOfReference unit = new DeltaFrameOfReference(1);   // 全基准档
        Encoded single = unit.encode(new long[] {42, 43, 44});
        assertThat(unit.decode(single.bytes(), single.count())).containsExactly(42L, 43L, 44L);
        DeltaFrameOfReference codec = new DeltaFrameOfReference(8);
        Encoded empty = codec.encode(new long[0]);
        assertThat(empty.count()).isZero();
        assertThat(codec.decode(empty.bytes(), 0)).isEmpty();
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new DeltaFrameOfReference(0))
                .isInstanceOf(IllegalArgumentException.class);
        DeltaFrameOfReference codec = new DeltaFrameOfReference(4);
        assertThatThrownBy(() -> codec.encode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.decode(null, 1))
                .isInstanceOf(IllegalArgumentException.class);
        Encoded encoded = codec.encode(new long[] {1, 2, 3});
        assertThatThrownBy(() -> codec.decode(encoded.bytes(), 5))   // 计数超流（截断）
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.decode(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9}, 1))   // 残留
                .isInstanceOf(Exception.class);
    }
}
