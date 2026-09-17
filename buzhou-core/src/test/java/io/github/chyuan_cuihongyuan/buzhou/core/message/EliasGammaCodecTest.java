package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3036 / T5074：Elias gamma 合同——手算码字（1/2/3/8）、千数
 * 往返、流式游标连解、码长公式、幂律小数极短主张、参数 fail-fast。
 */
class EliasGammaCodecTest {

    @Test
    void knownCodewordsShouldMatchHandComputation() {
        assertThat(EliasGammaCodec.encode(1)).isEqualTo("1");
        assertThat(EliasGammaCodec.encode(2)).isEqualTo("010");
        assertThat(EliasGammaCodec.encode(3)).isEqualTo("011");
        assertThat(EliasGammaCodec.encode(4)).isEqualTo("00100");
        assertThat(EliasGammaCodec.encode(8)).isEqualTo("0001000");
    }

    @Test
    void roundTripShouldHoldForRange() {
        for (int n = 1; n <= 1_000; n++) {
            assertThat(EliasGammaCodec.decode(EliasGammaCodec.encode(n)))
                    .as("n=%d", n).isEqualTo(n);
            assertThat(EliasGammaCodec.bitLength(n))
                    .as("bitLength n=%d", n)
                    .isEqualTo(EliasGammaCodec.encode(n).length());
        }
    }

    @Test
    void streamDecodeShouldAdvanceCursor() {
        StringBuilder stream = new StringBuilder();
        int[] values = {5, 1, 12, 100};
        for (int v : values) {
            stream.append(EliasGammaCodec.encode(v));
        }
        int offset = 0;
        for (int expected : values) {
            EliasGammaCodec.Decoded decoded = EliasGammaCodec.decodeAt(stream.toString(), offset);
            assertThat(decoded.value()).isEqualTo(expected);
            offset = decoded.nextOffset();
        }
        assertThat(offset).isEqualTo(stream.length());
    }

    @Test
    void smallNumbersShouldStayCompact() {
        // 幂律主张：1 位起步，31→9 位、63→11 位（定长 int 的 32 位对照）
        assertThat(EliasGammaCodec.bitLength(1)).isEqualTo(1);
        assertThat(EliasGammaCodec.bitLength(31)).isEqualTo(9);
        assertThat(EliasGammaCodec.bitLength(63)).isEqualTo(11);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> EliasGammaCodec.encode(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EliasGammaCodec.encode(-3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EliasGammaCodec.decode("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EliasGammaCodec.decode("0101")).isInstanceOf(IllegalArgumentException.class);  // 残留位
        assertThatThrownBy(() -> EliasGammaCodec.decode("000")).isInstanceOf(IllegalArgumentException.class);   // 零串无界
        assertThatThrownBy(() -> EliasGammaCodec.decodeAt("010", 9)).isInstanceOf(IllegalArgumentException.class);
    }
}
