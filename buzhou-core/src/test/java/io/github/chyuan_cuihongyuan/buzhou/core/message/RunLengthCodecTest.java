package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7004：RunLengthCodec 合同——(count,value) 对行程折叠。
 * 随机行程列 roundtrip；超 255 切段；全相异诚实膨胀 2×；
 * 全重复压缩读数；fail-fast。
 */
class RunLengthCodecTest {

    @Test
    void roundTripRandomRunData() {
        Random rng = new Random(7004L);
        for (int round = 0; round < 300; round++) {
            byte[] data = new byte[1 + rng.nextInt(500)];
            int i = 0;
            while (i < data.length) {
                int run = 1 + rng.nextInt(40);
                byte v = (byte) ('a' + rng.nextInt(5));
                for (int j = 0; j < run && i < data.length; j++) {
                    data[i++] = v;
                }
            }
            byte[] encoded = RunLengthCodec.encode(data);
            assertThat(RunLengthCodec.decode(encoded))
                    .as("round %d", round).containsExactly(data);
            assertThat(encoded.length % 2).isZero();
        }
    }

    @Test
    void longRunSplitsAt255() {
        byte[] data = new byte[600];
        java.util.Arrays.fill(data, (byte) 'x');
        byte[] encoded = RunLengthCodec.encode(data);
        assertThat(RunLengthCodec.pairCount(encoded)).isEqualTo(3);
        assertThat(encoded[0]).isEqualTo((byte) 255);
        assertThat(encoded[2]).isEqualTo((byte) 255);
        assertThat(encoded[4]).isEqualTo((byte) 90);
        assertThat(RunLengthCodec.decode(encoded)).containsExactly(data);
    }

    @Test
    void distinctDataExpandsAndRepeatedCompresses() {
        byte[] distinct = {1, 2, 3, 4, 5};
        byte[] encoded = RunLengthCodec.encode(distinct);
        assertThat(RunLengthCodec.pairCount(encoded)).isEqualTo(5);
        assertThat(RunLengthCodec.decode(encoded)).containsExactly(distinct);

        byte[] repeated = new byte[300];
        java.util.Arrays.fill(repeated, (byte) 7);
        assertThat(RunLengthCodec.pairCount(RunLengthCodec.encode(repeated))).isEqualTo(2);
    }

    @Test
    void emptyAndFailFastContract() {
        assertThat(RunLengthCodec.encode(new byte[0])).isEmpty();
        assertThat(RunLengthCodec.decode(new byte[0])).isEmpty();
        assertThatThrownBy(() -> RunLengthCodec.encode(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RunLengthCodec.decode(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RunLengthCodec.decode(new byte[]{3})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RunLengthCodec.decode(new byte[]{0, 5})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RunLengthCodec.pairCount(new byte[]{1})).isInstanceOf(IllegalArgumentException.class);
    }
}
