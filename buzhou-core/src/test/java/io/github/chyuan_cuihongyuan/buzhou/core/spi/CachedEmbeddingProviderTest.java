package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-38 / spec 13 §growth-8：embed-once 装饰器——同内容只 embed 一次（hash 键）、
 * LRU 容量逐出、命中/未命中计数可见。
 */
class CachedEmbeddingProviderTest {

    private static final class CountingProvider implements EmbeddingProvider {
        final AtomicInteger calls = new AtomicInteger();

        @Override
        public float[] embed(String text) {
            calls.incrementAndGet();
            return new float[]{text.length(), 1f};
        }
    }

    @Test
    void sameContentEmbedsOnce() {
        CountingProvider delegate = new CountingProvider();
        CachedEmbeddingProvider cached = new CachedEmbeddingProvider(delegate);

        float[] first = cached.embed("部署配置读取与回滚");
        float[] second = cached.embed("部署配置读取与回滚");

        assertThat(delegate.calls.get()).isEqualTo(1);
        assertThat(second).isEqualTo(first);
        assertThat(cached.hitCount()).isEqualTo(1);
        assertThat(cached.missCount()).isEqualTo(1);
    }

    @Test
    void differentContentEmbedsSeparately() {
        CountingProvider delegate = new CountingProvider();
        CachedEmbeddingProvider cached = new CachedEmbeddingProvider(delegate);

        cached.embed("内容甲");
        cached.embed("内容乙");

        assertThat(delegate.calls.get()).isEqualTo(2);
        assertThat(cached.missCount()).isEqualTo(2);
    }

    @Test
    void lruEvictsEldestWhenCapacityExceeded() {
        CountingProvider delegate = new CountingProvider();
        CachedEmbeddingProvider cached = new CachedEmbeddingProvider(delegate, 2);

        cached.embed("甲");
        cached.embed("乙");
        cached.embed("甲");      // 甲提升为最近使用
        cached.embed("丙");      // 容量 2 → 逐出乙
        cached.embed("乙");      // 乙已被逐出 → 重新 embed

        assertThat(delegate.calls.get()).isEqualTo(4);
        assertThat(cached.cachedCount()).isEqualTo(2);
    }

    @Test
    void defaultCapacityIs512() {
        CountingProvider delegate = new CountingProvider();
        CachedEmbeddingProvider cached = new CachedEmbeddingProvider(delegate);
        for (int i = 0; i < 600; i++) {
            cached.embed("内容-" + i);
        }
        assertThat(cached.cachedCount()).isEqualTo(CachedEmbeddingProvider.DEFAULT_CAPACITY);
        assertThat(CachedEmbeddingProvider.DEFAULT_CAPACITY).isEqualTo(512);
    }
}
