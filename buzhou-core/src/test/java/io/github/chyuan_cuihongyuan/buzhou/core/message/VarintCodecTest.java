package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3039 / T5080：varint 合同——zigzag 手算映射、字节长度阶梯、
 * 千数与极值往返、流式游标连解、截断/残留/超宽 fail-fast。
 */
class VarintCodecTest {

    @Test
    void zigzagShouldMatchHandMapping() {
        assertThat(VarintCodec.zigzagEncode(0)).isZero();
        assertThat(VarintCodec.zigzagEncode(-1)).isEqualTo(1);
        assertThat(VarintCodec.zigzagEncode(1)).isEqualTo(2);
        assertThat(VarintCodec.zigzagEncode(-2)).isEqualTo(3);
        assertThat(VarintCodec.zigzagDecode(1)).isEqualTo(-1);
        assertThat(VarintCodec.zigzagDecode(2)).isEqualTo(1);
    }

    @Test
    void byteLengthShouldFollowLadder() {
        assertThat(VarintCodec.encode(0)).hasSize(1);
        assertThat(VarintCodec.encode(-1)).hasSize(1);   // zigzag 1——负小值不顶格
        assertThat(VarintCodec.encode(300)).hasSize(2);  // zigzag 600 < 2^14
        assertThat(VarintCodec.encode(1L << 40)).hasSize(6);  // zigzag 2^41 = 42 位恰 6×7
        assertThat(VarintCodec.encode(Long.MIN_VALUE)).hasSize(10);
        assertThat(VarintCodec.encode(Long.MAX_VALUE)).hasSize(10);
    }

    @Test
    void roundTripShouldHoldForRangeAndExtremes() {
        for (long v = -1_000; v <= 1_000; v++) {
            assertThat(VarintCodec.decode(VarintCodec.encode(v))).as("v=%d", v).isEqualTo(v);
        }
        long[] extremes = {Long.MIN_VALUE, Long.MIN_VALUE + 1, -1L << 40, 0, 1L << 40,
                Long.MAX_VALUE - 1, Long.MAX_VALUE};
        for (long v : extremes) {
            assertThat(VarintCodec.decode(VarintCodec.encode(v))).isEqualTo(v);
        }
    }

    @Test
    void streamDecodeShouldAdvanceCursor() {
        long[] values = {0, -1, 300, 1L << 40, Long.MAX_VALUE};
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        for (long v : values) {
            buffer.writeBytes(VarintCodec.encode(v));
        }
        byte[] stream = buffer.toByteArray();
        int offset = 0;
        for (long expected : values) {
            VarintCodec.Decoded decoded = VarintCodec.decodeAt(stream, offset);
            assertThat(decoded.value()).isEqualTo(expected);
            offset += decoded.bytesRead();
        }
        assertThat(offset).isEqualTo(stream.length);
    }

    @Test
    void invalidInputsShouldFailFast() {
        assertThatThrownBy(() -> VarintCodec.decodeAt(new byte[] {(byte) 0x80}, 0))
                .isInstanceOf(IllegalArgumentException.class);   // 截断
        assertThatThrownBy(() -> VarintCodec.decode(new byte[] {0x05, 0x01}))
                .isInstanceOf(IllegalArgumentException.class);   // 残留字节
        assertThatThrownBy(() -> VarintCodec.decodeAt(new byte[] {1}, 5))
                .isInstanceOf(IllegalArgumentException.class);   // 游标越界
        byte[] overflow = new byte[11];
        java.util.Arrays.fill(overflow, (byte) 0xFF);
        overflow[10] = 0x7F;
        assertThatThrownBy(() -> VarintCodec.decodeAt(overflow, 0))
                .isInstanceOf(IllegalArgumentException.class);   // 超 long 位宽
    }
}
