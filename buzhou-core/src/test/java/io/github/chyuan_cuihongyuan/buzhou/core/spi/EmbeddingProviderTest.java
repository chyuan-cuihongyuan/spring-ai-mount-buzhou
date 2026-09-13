package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * EmbeddingProvider 静态余弦相似度直测（spec 1200 / T1801 / K 会话 R1 补测——此前零覆盖）。
 *
 * <p>接口由部署侧实现（测试以 lambda 充当 provider）；断言重点是 {@code cosine}
 * 纯函数的语义锚点与全部防御分支。
 */
class EmbeddingProviderTest {

    private static final double EPSILON = 1e-6;

    @Test
    void cosineIdenticalOrthogonalOppositeVectors() {
        float[] a = {1f, 2f, 3f};
        assertThat(EmbeddingProvider.cosine(a, new float[]{1f, 2f, 3f})).isCloseTo(1.0, within(EPSILON));
        assertThat(EmbeddingProvider.cosine(new float[]{1f, 0f}, new float[]{0f, 1f}))
                .isCloseTo(0.0, within(EPSILON));
        assertThat(EmbeddingProvider.cosine(new float[]{1f, 0f}, new float[]{-1f, 0f}))
                .isCloseTo(-1.0, within(EPSILON));
    }

    @Test
    void cosineDefensiveBranchesAllYieldZero() {
        assertThat(EmbeddingProvider.cosine(null, new float[]{1f})).isZero();
        assertThat(EmbeddingProvider.cosine(new float[]{1f}, null)).isZero();
        assertThat(EmbeddingProvider.cosine(new float[]{1f, 2f}, new float[]{1f, 2f, 3f})).isZero();
        assertThat(EmbeddingProvider.cosine(new float[]{}, new float[]{})).isZero();
        assertThat(EmbeddingProvider.cosine(new float[]{0f, 0f}, new float[]{1f, 1f})).isZero();
        assertThat(EmbeddingProvider.cosine(new float[]{1f, 1f}, new float[]{0f, 0f})).isZero();
    }

    @RepeatedTest(100)
    void cosineStaysWithinUnitDomain() {
        SplittableRandom random = new SplittableRandom(42);
        float[] a = new float[8];
        float[] b = new float[8];
        for (int i = 0; i < a.length; i++) {
            a[i] = random.nextFloat(-10f, 10f);
            b[i] = random.nextFloat(-10f, 10f);
        }
        double cosine = EmbeddingProvider.cosine(a, b);
        assertThat(cosine).isBetween(-1.0, 1.0);
    }

    @Test
    void providerLambdaSatisfiesEmbedContract() {
        EmbeddingProvider provider = text -> switch (text) {
            case "猫" -> new float[]{1f, 0f};
            case "狗" -> new float[]{0.9f, 0.1f};
            default -> new float[]{0f, 1f};
        };
        assertThat(EmbeddingProvider.cosine(provider.embed("猫"), provider.embed("狗")))
                .isGreaterThan(0.9);
        assertThat(EmbeddingProvider.cosine(provider.embed("猫"), provider.embed("石头")))
                .isCloseTo(0.0, within(EPSILON));
    }
}
