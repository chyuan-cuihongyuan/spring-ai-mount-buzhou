package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnsCodecTest {

    @Test
    void shouldCompressSkewedInputBelowRaw() {
        // 单符号长串：重整字节极少（≈0 bit/符号）——总流（520B 头+ε）远小于 4000 原始
        byte[] run = new byte[4000];
        java.util.Arrays.fill(run, (byte) 'x');
        byte[] stream = AnsCodec.encode(run);
        assertThat(stream.length).isLessThan(700);
        assertThat(AnsCodec.decode(stream)).containsExactly(run);
        // 强偏斜文本（4 字母表）
        Random random = new Random(2);
        byte[] skewed = new byte[3000];
        for (int i = 0; i < skewed.length; i++) {
            int dice = random.nextInt(100);
            skewed[i] = (byte) (dice < 70 ? 'a' : dice < 85 ? 'b' : dice < 95 ? 'c' : 'd');
        }
        byte[] skewedStream = AnsCodec.encode(skewed);
        assertThat(skewedStream.length).isLessThan(skewed.length);
        assertThat(AnsCodec.decode(skewedStream)).containsExactly(skewed);
    }

    @Test
    void shouldRoundTripExactly() {
        assertThat(AnsCodec.decode(AnsCodec.encode(new byte[0]))).isEmpty();
        assertThat(AnsCodec.decode(AnsCodec.encode(new byte[]{'s'}))).containsExactly((byte) 's');
        String[] texts = {"banana", "mississippi", "ababababab", "the quick brown fox"};
        for (String text : texts) {
            byte[] data = text.getBytes(StandardCharsets.US_ASCII);
            assertThat(AnsCodec.decode(AnsCodec.encode(data)))
                    .as("文本 %s 往返", text).containsExactly(data);
        }
        Random random = new Random(73);
        for (int t = 0; t < 60; t++) {
            int n = 1 + random.nextInt(900);
            byte[] data = new byte[n];
            if (t % 3 == 0) {
                // 偏斜字母表
                for (int i = 0; i < n; i++) {
                    data[i] = (byte) ('a' + random.nextInt(3));
                }
            } else if (t % 3 == 1) {
                // 单符号游程
                java.util.Arrays.fill(data, (byte) random.nextInt(256));
            } else {
                random.nextBytes(data);
            }
            byte[] stream = AnsCodec.encode(data);
            assertThat(AnsCodec.decode(stream)).as("随机 %d（%d 字节→%d 流）", t, n, stream.length)
                    .containsExactly(data);
        }
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        byte[] data = "determinism".getBytes(StandardCharsets.US_ASCII);
        assertThat(AnsCodec.encode(data)).containsExactly(AnsCodec.encode(data));
        assertThatThrownBy(() -> AnsCodec.encode(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AnsCodec.decode(null))
                .isInstanceOf(IllegalArgumentException.class);
        // 短流
        assertThatThrownBy(() -> AnsCodec.decode(new byte[10]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("短于头");
        // 频率表损坏：头后 512 字节改零 → 和 ≠ M
        byte[] stream = AnsCodec.encode("table".getBytes(StandardCharsets.US_ASCII));
        java.util.Arrays.fill(stream, 4, 4 + 512, (byte) 0);
        assertThatThrownBy(() -> AnsCodec.decode(stream))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非归一");
    }
}
