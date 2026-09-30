package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LzwCodecTest {

    @Test
    void shouldMatchClassicCodeAnchors() {
        // 经典锚：TOBEORNOTTOBE 首十码（T/O/B/E/O/R/N/O/T/TO=256——字典与数据同构生长）
        byte[] input = "TOBEORNOTTOBE".getBytes(StandardCharsets.US_ASCII);
        List<Integer> codes = LzwCodec.encode(input);
        assertThat(codes).startsWith(84, 79, 66, 69, 79, 82, 78, 79, 84, 256);
        // 双写重复压缩：ABABABABA...（20 字 A/B 交替）→ 远少于 20 码
        byte[] ab = new byte[20];
        for (int i = 0; i < ab.length; i++) {
            ab[i] = (byte) (i % 2 == 0 ? 'A' : 'B');
        }
        assertThat(LzwCodec.encode(ab).size()).isLessThan(10);
        // 同字符串压缩：aaaaaaaaaa（10 字）→ 4 码内
        assertThat(LzwCodec.encode("aaaaaaaaaa".getBytes(StandardCharsets.US_ASCII)).size())
                .isLessThanOrEqualTo(4);
    }

    @Test
    void shouldRoundTripExactly() {
        assertThat(LzwCodec.decode(LzwCodec.encode(new byte[0]))).isEmpty();
        assertThat(LzwCodec.decode(LzwCodec.encode(new byte[]{7}))).containsExactly(7);
        // KwKwK 特例锚：ABAAB——decode 侧首遇「码=即将入典」分支
        byte[] kwkwk = "ABAAB".getBytes(StandardCharsets.US_ASCII);
        assertThat(LzwCodec.decode(LzwCodec.encode(kwkwk))).containsExactly(kwkwk);
        // 随机二进制圣像：256 值域全谱、往返精确
        Random random = new Random(5);
        for (int t = 0; t < 60; t++) {
            int n = random.nextInt(600);
            byte[] data = new byte[n];
            random.nextBytes(data);
            byte[] roundTripped = LzwCodec.decode(LzwCodec.encode(data));
            assertThat(roundTripped).as("随机 %d（%d 字节）往返", t, n).containsExactly(data);
        }
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        byte[] data = "same-input-same-codes".getBytes(StandardCharsets.US_ASCII);
        assertThat(LzwCodec.encode(data)).isEqualTo(LzwCodec.encode(data));
        assertThatThrownBy(() -> LzwCodec.encode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LzwCodec.decode(null))
                .isInstanceOf(IllegalArgumentException.class);
        // 码字缺口：直接喂未在典的 999
        assertThatThrownBy(() -> LzwCodec.decode(List.of(65, 999)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("缺口");
        // KwKwK 合法用例之外：首码即非法（>255 且 != 256）
        assertThatThrownBy(() -> LzwCodec.decode(List.of(300)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
