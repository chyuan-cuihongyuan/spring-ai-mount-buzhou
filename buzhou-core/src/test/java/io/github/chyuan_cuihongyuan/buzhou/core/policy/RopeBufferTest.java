package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4044 / T6090：Rope 合同——圣像对拍（固定种子 500 操作）、
 * 显式插删（含跨块）、深度上界、空 rope 与越界 fail-fast。
 */
class RopeBufferTest {

    private static final int ORACLE_OPS = 500;
    private static final int ALPHABET_CHUNKS = 40;

    @Test
    void operationSequenceShouldMatchStringBuilderOracle() {
        Random random = new Random(4044L);
        RopeBuffer rope = new RopeBuffer("seed");
        StringBuilder oracle = new StringBuilder("seed");
        for (int op = 0; op < ORACLE_OPS; op++) {
            boolean insert = random.nextBoolean() || oracle.length() == 0;
            if (insert) {
                String text = String.valueOf(random.nextInt(10)).repeat(1 + random.nextInt(20));
                int offset = random.nextInt(oracle.length() + 1);
                rope.insert(offset, text);
                oracle.insert(offset, text);
            } else {
                int start = random.nextInt(oracle.length());
                int end = start + 1 + random.nextInt(Math.min(30, oracle.length() - start));
                rope.delete(start, end);
                oracle.delete(start, end);
            }
            assertThat(rope.length()).isEqualTo(oracle.length());
        }
        assertThat(rope.toString()).isEqualTo(oracle.toString());
        for (int probe = 0; probe < 100; probe++) {
            int index = random.nextInt(oracle.length());
            assertThat(rope.charAt(index)).isEqualTo(oracle.charAt(index));
        }
    }

    @Test
    void explicitInsertsAtHeadMiddleTailShouldHold() {
        RopeBuffer rope = new RopeBuffer("bcd");
        rope.insert(0, "a");
        rope.insert(2, "-");
        rope.insert(rope.length(), "e");
        assertThat(rope.toString()).isEqualTo("ab-cde");
        assertThat(rope.length()).isEqualTo(6);
    }

    @Test
    void crossChunkDeleteShouldStitchNeighbors() {
        StringBuilder seed = new StringBuilder();
        for (int i = 0; i < ALPHABET_CHUNKS; i++) {
            seed.append("0123456789");   // 400 字符 = 多叶
        }
        RopeBuffer rope = new RopeBuffer(seed.toString());
        rope.delete(195, 205);   // 横跨 512 界内的多块区域
        seed.delete(195, 205);
        assertThat(rope.toString()).isEqualTo(seed.toString());
        assertThat(rope.length()).isEqualTo(seed.length());
    }

    @Test
    void depthShouldStayBoundedAfterHeavyEdits() {
        RopeBuffer rope = new RopeBuffer("x");
        for (int i = 0; i < 400; i++) {
            rope.insert(rope.length() / 2, "y".repeat(30));
        }
        assertThat(rope.length()).isEqualTo(1 + 400 * 30);
        assertThat(rope.depth()).isLessThanOrEqualTo(
                2 * (32 - Integer.numberOfLeadingZeros(rope.length())) + 8);
        assertThat(rope.charAt(0)).isEqualTo('y');   // 首次中点插入即占据开头
        assertThat(rope.toString().chars().filter(ch -> ch == 'x').count()).isEqualTo(1);   // 恰一个 'x'
        assertThat(rope.charAt(rope.length() - 1)).isEqualTo('x');   // 中点插入恒在 'x' 之前——'x' 沉底
    }

    @Test
    void emptyRopeAndOutOfRangeShouldFailFast() {
        RopeBuffer empty = new RopeBuffer("");
        assertThat(empty.length()).isZero();
        assertThat(empty.toString()).isEmpty();
        assertThatThrownBy(() -> empty.charAt(0))
                .isInstanceOf(StringIndexOutOfBoundsException.class);
        RopeBuffer rope = new RopeBuffer("abc");
        assertThatThrownBy(() -> rope.insert(4, "x"))
                .isInstanceOf(StringIndexOutOfBoundsException.class);
        assertThatThrownBy(() -> rope.delete(2, 1))
                .isInstanceOf(StringIndexOutOfBoundsException.class);
        assertThatThrownBy(() -> rope.delete(-1, 1))
                .isInstanceOf(StringIndexOutOfBoundsException.class);
        assertThatThrownBy(() -> rope.charAt(3))
                .isInstanceOf(StringIndexOutOfBoundsException.class);
    }
}
