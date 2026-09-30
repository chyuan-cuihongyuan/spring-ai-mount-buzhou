package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MorseCodecTest {

    @Test
    void shouldMatchClassicAnchors() {
        assertThat(MorseCodec.encode("SOS")).isEqualTo("... --- ...");
        assertThat(MorseCodec.encode("sos")).isEqualTo("... --- ..."); // 大小写不敏感
        assertThat(MorseCodec.encode("A")).isEqualTo(".-");
        assertThat(MorseCodec.encode("HELLO WORLD"))
                .isEqualTo(".... . .-.. .-.. --- / .-- --- .-. .-.. -..");
        assertThat(MorseCodec.decode("... --- ...")).isEqualTo("SOS");
        assertThat(MorseCodec.decode(".... . .-.. .-.. --- / .-- --- .-. .-.. -.."))
                .isEqualTo("HELLO WORLD");
        // 数字全覆盖
        assertThat(MorseCodec.decode(MorseCodec.encode("0123456789"))).isEqualTo("0123456789");
    }

    @Test
    void shouldRoundTripRandomTexts() {
        Random random = new Random(151);
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 ";
        for (int t = 0; t < 60; t++) {
            int words = 1 + random.nextInt(5);
            StringBuilder sb = new StringBuilder();
            for (int w = 0; w < words; w++) {
                if (w > 0) {
                    sb.append(' ');
                }
                int len = 1 + random.nextInt(8);
                for (int i = 0; i < len; i++) {
                    sb.append(alphabet.charAt(random.nextInt(36))); // 字母数字
                }
            }
            String text = sb.toString();
            assertThat(MorseCodec.decode(MorseCodec.encode(text))).as("文本 %d 往返", t)
                    .isEqualTo(text);
            // 大小写归一
            String lower = text.toLowerCase();
            assertThat(MorseCodec.decode(MorseCodec.encode(lower))).isEqualTo(text);
        }
    }

    @Test
    void shouldBeDeterministicAndFailFast() {
        assertThat(MorseCodec.encode("ABC")).isEqualTo(MorseCodec.encode("abc"));
        assertThatThrownBy(() -> MorseCodec.encode(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MorseCodec.encode("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MorseCodec.encode("AB!")).hasMessageContaining("未知字符");
        assertThatThrownBy(() -> MorseCodec.encode("A  B")).hasMessageContaining("连续分隔符");
        assertThatThrownBy(() -> MorseCodec.encode("A ")).hasMessageContaining("尾随分隔符");
        assertThatThrownBy(() -> MorseCodec.decode(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MorseCodec.decode(".- ----")).hasMessageContaining("非法点划"); // "----" 四划非法（"." 单点是合法 E）
        assertThatThrownBy(() -> MorseCodec.decode(".- /")).hasMessageContaining("尾随词分隔");
        assertThatThrownBy(() -> MorseCodec.decode("/ / .-")).hasMessageContaining("连续词分隔");
    }
}
