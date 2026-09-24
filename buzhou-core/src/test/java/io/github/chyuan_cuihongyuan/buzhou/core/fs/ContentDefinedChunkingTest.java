package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5033 / T6168：内容定义分块合同——圣像边界钉住（Python
 * 64 位语义预演）、覆盖连续、min/max 约束、确定性、边界前
 * 稳定（修改只影响下游）、短数据边、fail-fast。
 */
class ContentDefinedChunkingTest {

    private static final long SEED = 500L;

    private static final int DATA_SIZE = 4096;

    private static final int FLIP_INDEX = 2048;

    private static final int EXPECTED_CHUNK_COUNT = 304;

    private static final int MIN_SIZE = 8;

    private static final int MAX_SIZE = 32;

    private static final int AVG_SHIFT_BITS = 3;

    private static ContentDefinedChunking chunker() {
        return new ContentDefinedChunking(SEED, MIN_SIZE, MAX_SIZE, AVG_SHIFT_BITS);
    }

    private static byte[] dataset() {
        byte[] data = new byte[DATA_SIZE];
        for (int i = 0; i < DATA_SIZE; i++) {
            data[i] = (byte) ((i * 31 + 7) % 256);
        }
        return data;
    }

    @Test
    void boundariesShouldMatchPrecomputedOracle() {
        List<ContentDefinedChunking.Chunk> chunks = chunker().chunk(dataset());
        assertThat(chunks).hasSize(EXPECTED_CHUNK_COUNT);
        assertThat(chunks.get(0)).isEqualTo(new ContentDefinedChunking.Chunk(0, 11));
        assertThat(chunks.get(1)).isEqualTo(new ContentDefinedChunking.Chunk(11, 10));
        assertThat(chunks.get(2)).isEqualTo(new ContentDefinedChunking.Chunk(21, 9));
        assertThat(chunks.get(3)).isEqualTo(new ContentDefinedChunking.Chunk(30, 8));
        assertThat(chunks.get(4)).isEqualTo(new ContentDefinedChunking.Chunk(38, 11));
        assertThat(chunks.get(5)).isEqualTo(new ContentDefinedChunking.Chunk(49, 13));
        assertThat(chunks.get(6)).isEqualTo(new ContentDefinedChunking.Chunk(62, 8));
        assertThat(chunks.get(7)).isEqualTo(new ContentDefinedChunking.Chunk(70, 10));
        assertThat(chunks.get(chunks.size() - 1))
                .isEqualTo(new ContentDefinedChunking.Chunk(4086, 10));
    }

    @Test
    void chunksShouldCoverDataContiguously() {
        byte[] data = dataset();
        List<ContentDefinedChunking.Chunk> chunks = chunker().chunk(data);
        int expectedOffset = 0;
        for (ContentDefinedChunking.Chunk chunk : chunks) {
            assertThat(chunk.offset()).isEqualTo(expectedOffset);
            expectedOffset += chunk.length();
        }
        assertThat(expectedOffset).isEqualTo(data.length);
    }

    @Test
    void chunkLengthsShouldRespectMinAndMax() {
        List<ContentDefinedChunking.Chunk> chunks = chunker().chunk(dataset());
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertThat(chunks.get(i).length()).isBetween(MIN_SIZE, MAX_SIZE);
        }
    }

    @Test
    void sameContentAndParamsShouldGiveSameBoundaries() {
        byte[] data = dataset();
        List<ContentDefinedChunking.Chunk> first = chunker().chunk(data);
        List<ContentDefinedChunking.Chunk> second = chunker().chunk(data);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void singleByteFlipShouldKeepBoundariesBeforeItStable() {
        byte[] data = dataset();
        data[FLIP_INDEX] ^= 0x01;
        List<ContentDefinedChunking.Chunk> flipped = chunker().chunk(data);
        List<ContentDefinedChunking.Chunk> original = chunker().chunk(dataset());
        List<ContentDefinedChunking.Chunk> beforeFlip = original.stream()
                .filter(c -> c.offset() + c.length() <= FLIP_INDEX)
                .toList();
        List<ContentDefinedChunking.Chunk> flippedBefore = flipped.stream()
                .filter(c -> c.offset() + c.length() <= FLIP_INDEX)
                .toList();
        assertThat(beforeFlip).isNotEmpty();
        assertThat(beforeFlip).isEqualTo(flippedBefore);
        assertThat(original).isNotEqualTo(flipped);
    }

    @Test
    void shortDataShouldFormSingleChunk() {
        assertThat(chunker().chunk(new byte[]{1, 2, 3}))
                .containsExactly(new ContentDefinedChunking.Chunk(0, 3));
        assertThat(chunker().chunk(new byte[MIN_SIZE]))
                .containsExactly(new ContentDefinedChunking.Chunk(0, MIN_SIZE));
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new ContentDefinedChunking(SEED, 0, MAX_SIZE, AVG_SHIFT_BITS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ContentDefinedChunking(SEED, MAX_SIZE, MIN_SIZE, AVG_SHIFT_BITS))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ContentDefinedChunking(SEED, MIN_SIZE, MAX_SIZE, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chunker().chunk(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ContentDefinedChunking.Chunk(-1, 3))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ContentDefinedChunking.Chunk(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
