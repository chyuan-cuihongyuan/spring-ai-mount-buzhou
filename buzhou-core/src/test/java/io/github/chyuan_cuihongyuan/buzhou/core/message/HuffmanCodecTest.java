package io.github.chyuan_cuihongyuan.buzhou.core.message;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.message.HuffmanCodec.Encoded;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4006 / T6014：Huffman 合同——频率分层码长、均匀定长、
 * roundtrip+压缩性、单符号退化、畸形 fail-fast。
 */
class HuffmanCodecTest {

    /** 频率自 'a' 起顺序落位（'a'、'b'、'c'…）。 */
    private static HuffmanCodec codecOf(long... freqByLetter) {
        long[] freq = new long[256];
        for (int i = 0; i < freqByLetter.length; i++) {
            freq['a' + i] = freqByLetter[i];
        }
        return new HuffmanCodec(freq);
    }

    @Test
    void skewedFrequenciesShouldAssignShorterCodesToHotSymbols() {
        HuffmanCodec codec = codecOf(100, 50, 20, 10);   // 'a','b','c','d'
        assertThat(codec.codeLength((byte) 'a')).isEqualTo(1);
        assertThat(codec.codeLength((byte) 'b')).isEqualTo(2);
        assertThat(codec.codeLength((byte) 'c')).isEqualTo(3);
        assertThat(codec.codeLength((byte) 'd')).isEqualTo(3);
        assertThat(codec.distinctSymbols()).isEqualTo(4);
    }

    @Test
    void uniformFrequenciesShouldDegradeToFixedLength() {
        HuffmanCodec codec = codecOf(1, 1, 1, 1);
        assertThat(codec.codeLength((byte) 'a')).isEqualTo(2);
        assertThat(codec.codeLength((byte) 'b')).isEqualTo(2);
        assertThat(codec.codeLength((byte) 'c')).isEqualTo(2);
        assertThat(codec.codeLength((byte) 'd')).isEqualTo(2);
    }

    @Test
    void roundtripShouldCompressSkewedStream() {
        HuffmanCodec codec = codecOf(800, 150, 50);   // a/b/c 三层倾斜
        StringBuilder sb = new StringBuilder(1000);
        for (int i = 0; i < 800; i++) {
            sb.append('a');
        }
        for (int i = 0; i < 150; i++) {
            sb.append('b');
        }
        for (int i = 0; i < 50; i++) {
            sb.append('c');
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.US_ASCII);
        Encoded encoded = codec.encode(data);
        assertThat(encoded.bitCount()).isLessThan(2_000L);   // 均匀 8 位要 8000 位
        assertThat(codec.decode(encoded.bytes(), encoded.bitCount())).isEqualTo(data);
    }

    @Test
    void singleSymbolShouldDegradeToOneBitCode() {
        HuffmanCodec codec = codecOf(0, 0, 42);   // 仅 'c'
        assertThat(codec.codeLength((byte) 'c')).isEqualTo(1);
        assertThat(codec.distinctSymbols()).isEqualTo(1);
        byte[] data = "ccccc".getBytes(StandardCharsets.US_ASCII);
        Encoded encoded = codec.encode(data);
        assertThat(encoded.bitCount()).isEqualTo(5);
        assertThat(codec.decode(encoded.bytes(), encoded.bitCount())).isEqualTo(data);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new HuffmanCodec(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HuffmanCodec(new long[10]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HuffmanCodec(new long[256]))
                .isInstanceOf(IllegalArgumentException.class);   // 全零
        assertThatThrownBy(() -> codecOf(-1))
                .isInstanceOf(IllegalArgumentException.class);
        HuffmanCodec codec = codecOf(5);
        assertThatThrownBy(() -> codec.encode(new byte[] {'z'}))   // 未入表
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.decode(new byte[1], 9))     // 位长超字节面
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> codec.encode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
