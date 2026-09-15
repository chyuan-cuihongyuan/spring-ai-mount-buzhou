package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.EmbeddingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1115 / impl 866：SemanticChunkIndex 操作读面——index/locate 入口、
 * 无效入参静默跳过、切片入索引计数、resetForTest 归零。
 */
class ChunkIndexOpStatsTest {

    @BeforeEach
    void reset() {
        SemanticChunkIndex.resetForTest();
    }

    @Test
    void validIndexCountsCallsAndChunks() {
        SemanticChunkIndex idx = new SemanticChunkIndex(text -> new float[]{1f, 0f});
        idx.index("spill://a", List.of(new int[]{0, 5}, new int[]{5, 10}), "abcdefghij");

        SemanticChunkIndex.ChunkIndexOpStats stats = SemanticChunkIndex.stats();
        assertThat(stats.indexCalls()).isEqualTo(1);
        assertThat(stats.chunksIndexed()).isEqualTo(2);
        assertThat(stats.skippedInvalid()).isZero();
    }

    @Test
    void invalidIndexCountsSkipped() {
        SemanticChunkIndex idx = new SemanticChunkIndex(null); // 无 provider
        idx.index("spill://a", List.of(new int[]{0, 5}), "content");

        SemanticChunkIndex.ChunkIndexOpStats stats = SemanticChunkIndex.stats();
        assertThat(stats.indexCalls()).isEqualTo(1);
        assertThat(stats.skippedInvalid()).isEqualTo(1);
        assertThat(stats.chunksIndexed()).isZero();
    }

    @Test
    void validLocateCountsLocate() {
        SemanticChunkIndex idx = new SemanticChunkIndex(text -> new float[]{1f, 0f});
        idx.index("spill://a", List.of(new int[]{0, 5}), "content");
        assertThat(idx.locate("query", 5, 0.01)).isNotEmpty();

        SemanticChunkIndex.ChunkIndexOpStats stats = SemanticChunkIndex.stats();
        assertThat(stats.locateCalls()).isEqualTo(1);
    }

    @Test
    void invalidLocateCountsSkipped() {
        SemanticChunkIndex idx = new SemanticChunkIndex(text -> new float[]{1f});
        assertThat(idx.locate(" ", 5, 0.01)).isEmpty();

        SemanticChunkIndex.ChunkIndexOpStats stats = SemanticChunkIndex.stats();
        assertThat(stats.locateCalls()).isEqualTo(1);
        assertThat(stats.skippedInvalid()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        SemanticChunkIndex idx = new SemanticChunkIndex(text -> new float[]{1f, 0f});
        idx.index("spill://a", List.of(new int[]{0, 5}), "content");
        assertThat(SemanticChunkIndex.stats().indexCalls()).isEqualTo(1);

        SemanticChunkIndex.resetForTest();

        SemanticChunkIndex.ChunkIndexOpStats stats = SemanticChunkIndex.stats();
        assertThat(stats.indexCalls()).isZero();
        assertThat(stats.chunksIndexed()).isZero();
        assertThat(stats.locateCalls()).isZero();
    }
}
