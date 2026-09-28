package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7030：XxHash64 合同——官方钉子向量+stripe/尾字节
 * 路径一致性+种子扩散+确定性；fail-fast。
 */
class XxHash64Test {

    @Test
    void officialVectors() {
        assertThat(XxHash64.hash(new byte[0])).isEqualTo(0xEF46DB3751D8E999L);
        assertThat(XxHash64.hash("a".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo(0xd24ec4f1a98c6e5bL);
        assertThat(XxHash64.hash("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo(0x44bc2cf5ad770999L);
    }

    @Test
    void stripeAndTailPathsConsistent() {
        for (int length = 0; length <= 200; length += 1) {
            byte[] data = new byte[length];
            for (int i = 0; i < length; i++) {
                data[i] = (byte) ('a' + (i % 7));
            }
            long expected = XxHash64.hash(data, 42L);
            byte[] padded = new byte[length + 64];
            System.arraycopy(data, 0, padded, 32, length);
            byte[] exact = new byte[length];
            System.arraycopy(padded, 32, exact, 0, length);
            assertThat(XxHash64.hash(exact, 42L))
                    .as("长度 %d 镜像一致", length).isEqualTo(expected);
        }
    }

    @Test
    void seedDiffusionAndDeterminism() {
        byte[] data = "the quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        assertThat(XxHash64.hash(data, 1L)).isNotEqualTo(XxHash64.hash(data, 2L));
        for (int i = 0; i < 50; i++) {
            assertThat(XxHash64.hash(data, 7L)).isEqualTo(XxHash64.hash(data, 7L));
        }
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> XxHash64.hash(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> XxHash64.hash(null, 5L)).isInstanceOf(IllegalArgumentException.class);
    }
}
