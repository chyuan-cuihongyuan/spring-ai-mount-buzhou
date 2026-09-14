package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1447 / T2188 兄弟票：语义切片索引覆盖读面——已索引 uri/切片总数/
 * 单 uri 最大切片（失衡信号）；不可用 provider 下空覆盖。
 */
class SemanticChunkIndexCoverageTest {

    /** 词包 provider（语义自检同思想——确定性向量）。 */
    private static SemanticChunkIndex index() {
        return new SemanticChunkIndex(t -> {
            float[] v = new float[8];
            v[Math.floorMod(t.hashCode(), 8)] = 1f;
            return v;
        });
    }

    @Test
    void emptyIndexReportsZero() {
        var c = index().coverageStats();
        assertThat(c.indexedUries()).isZero();
        assertThat(c.totalChunks()).isZero();
        assertThat(c.maxChunksPerUri()).isZero();
        assertThat(c.largestUri()).isNull();
    }

    @Test
    void coverageCountsUrisAndChunks() {
        SemanticChunkIndex idx = index();
        idx.index("spill://a", List.of(new int[]{0, 5}, new int[]{5, 10}), "aaaaabbbbb");
        idx.index("spill://b", List.of(new int[]{0, 4}), "ccccdddd");
        var c = idx.coverageStats();
        assertThat(c.indexedUries()).isEqualTo(2);
        assertThat(c.totalChunks()).isEqualTo(3);
        // 单 uri 最大切片：a 有 2、b 有 1
        assertThat(c.maxChunksPerUri()).isEqualTo(2);
        assertThat(c.largestUri()).isEqualTo("spill://a");
    }

    @Test
    void unavailableProviderYieldsZeroCoverage() {
        SemanticChunkIndex idx = new SemanticChunkIndex(null);
        idx.index("spill://x", List.of(new int[]{0, 5}), "内容内容内容");
        assertThat(idx.available()).isFalse();
        assertThat(idx.coverageStats().indexedUries()).isZero();
    }
}
