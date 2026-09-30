package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Lz77CodecTest {

    @Test
    void shouldCompressRepetitionsIntoMatches() {
        // 重复段压缩圣像：ABAB...（40 字）→ 少量字面 + 长度递增的 match token
        byte[] ab = new byte[40];
        for (int i = 0; i < ab.length; i++) {
            ab[i] = (byte) (i % 2 == 0 ? 'A' : 'B');
        }
        List<Lz77Codec.Token> tokens = Lz77Codec.encode(ab, 4096);
        assertThat(tokens.size()).isLessThan(10);
        assertThat(Lz77Codec.decode(tokens)).containsExactly(ab);
        // 自重叠引用：aaaa...（30 字）→ 首 token 字面后全 offset=1 长 match
        byte[] aaaa = new byte[30];
        java.util.Arrays.fill(aaaa, (byte) 'a');
        List<Lz77Codec.Token> runTokens = Lz77Codec.encode(aaaa, 4096);
        assertThat(Lz77Codec.decode(runTokens)).containsExactly(aaaa);
        boolean hasOffsetOne = false;
        for (Lz77Codec.Token token : runTokens) {
            if (token.offset() == 1 && token.length() > 3) {
                hasOffsetOne = true;
            }
        }
        assertThat(hasOffsetOne).isTrue();
    }

    @Test
    void shouldFallBackToLiteralsForIncompressible() {
        // 不可压退化圣像：随机全谱字节 → 纯字面 token（窗口内无 ≥3 匹配）
        Random random = new Random(47);
        byte[] noise = new byte[300];
        random.nextBytes(noise);
        List<Lz77Codec.Token> tokens = Lz77Codec.encode(noise, 256);
        assertThat(tokens).hasSize(300);
        for (Lz77Codec.Token token : tokens) {
            assertThat(token.length()).isZero();
        }
        assertThat(Lz77Codec.decode(tokens)).containsExactly(noise);
    }

    @Test
    void shouldRoundTripExactly() {
        assertThat(Lz77Codec.decode(Lz77Codec.encode(new byte[0], 64))).isEmpty();
        String[] texts = {"abcabcabcabc", "mississippi", "the quick brown fox",
                "aaaaaaaaabbbbbbbbbbccccccccc"};
        for (String text : texts) {
            byte[] data = text.getBytes(StandardCharsets.US_ASCII);
            for (int window : new int[]{4, 16, 4096}) {
                assertThat(Lz77Codec.decode(Lz77Codec.encode(data, window)))
                        .as("文本 %s 窗 %d 往返", text, window).containsExactly(data);
            }
        }
        Random random = new Random(53);
        for (int t = 0; t < 50; t++) {
            int n = random.nextInt(800);
            byte[] data = new byte[n];
            if (t % 2 == 0) {
                for (int i = 0; i < n; i++) {
                    data[i] = (byte) ('a' + random.nextInt(3));
                }
            } else {
                random.nextBytes(data);
            }
            assertThat(Lz77Codec.decode(Lz77Codec.encode(data, 128)))
                    .as("随机 %d 往返", t).containsExactly(data);
        }
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        byte[] data = "deterministic-deterministic".getBytes(StandardCharsets.US_ASCII);
        List<Lz77Codec.Token> first = Lz77Codec.encode(data, 64);
        List<Lz77Codec.Token> second = Lz77Codec.encode(data, 64);
        assertThat(first.toString()).isEqualTo(second.toString());
        assertThatThrownBy(() -> Lz77Codec.encode(null, 64))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Lz77Codec.encode(data, 0))
                .hasMessageContaining("窗口为正");
        assertThatThrownBy(() -> Lz77Codec.decode(null))
                .isInstanceOf(IllegalArgumentException.class);
        // 非法 token：offset=0 但 length>0；回引越历史
        assertThatThrownBy(() -> Lz77Codec.decode(List.of(new Lz77Codec.Token(0, 5, (byte) 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("匹配 token 非法");
        assertThatThrownBy(() -> Lz77Codec.decode(List.of(new Lz77Codec.Token(9, 9, (byte) 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("回引越历史");
        assertThatThrownBy(() -> Lz77Codec.decode(List.of(new Lz77Codec.Token(2, 0, (byte) 65))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offset 必 0");
    }
}
