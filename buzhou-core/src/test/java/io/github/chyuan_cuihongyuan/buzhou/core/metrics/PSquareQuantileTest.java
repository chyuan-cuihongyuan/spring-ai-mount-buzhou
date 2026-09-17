package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 3006 / T5014：P² 流式分位数合同——洗牌整数流中位/p90 收敛
 * （种子确定性）、常量流精确、单调升流收敛、前 5 样本诚实口径、
 * 空态 NaN、p 开区间校验。
 */
class PSquareQuantileTest {

    private static List<Integer> shuffledIntegers(int bound, long seed) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < bound; i++) {
            list.add(i);
        }
        Collections.shuffle(list, new Random(seed));
        return list;
    }

    @Test
    void medianOfShuffledIntegersShouldConverge() {
        PSquareQuantile q = new PSquareQuantile(0.5);
        for (int v : shuffledIntegers(1_000, 42)) {
            q.add(v);
        }
        assertThat(q.count()).isEqualTo(1_000);
        assertThat(q.estimate()).isCloseTo(499.5, within(5.0));
    }

    @Test
    void p90OfShuffledIntegersShouldConverge() {
        PSquareQuantile q = new PSquareQuantile(0.9);
        for (int v : shuffledIntegers(1_000, 7)) {
            q.add(v);
        }
        assertThat(q.estimate()).isCloseTo(899.1, within(10.0));
    }

    @Test
    void constantStreamShouldStayExact() {
        PSquareQuantile q = new PSquareQuantile(0.5);
        for (int i = 0; i < 100; i++) {
            q.add(7.0);
        }
        assertThat(q.estimate()).isEqualTo(7.0);
    }

    @Test
    void ascendingStreamShouldConvergeToMedian() {
        PSquareQuantile q = new PSquareQuantile(0.5);
        for (int i = 1; i <= 1_000; i++) {
            q.add(i);
        }
        assertThat(q.estimate()).isCloseTo(500.0, within(10.0));
    }

    @Test
    void preInitShouldUseSortedBufferHonestly() {
        PSquareQuantile q = new PSquareQuantile(0.5);
        assertThat(q.estimate()).isNaN();
        q.add(30);
        q.add(10);
        q.add(20);
        // count=3 排序 {10,20,30}，floor(0.5×2)=1 → 20
        assertThat(q.estimate()).isEqualTo(20.0);
    }

    @Test
    void quantileAndCountShouldReadBack() {
        PSquareQuantile q = new PSquareQuantile(0.25);
        assertThat(q.quantile()).isEqualTo(0.25);
        q.add(1);
        assertThat(q.count()).isEqualTo(1);
    }

    @Test
    void pMustBeInOpenInterval() {
        assertThatThrownBy(() -> new PSquareQuantile(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PSquareQuantile(1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PSquareQuantile(-0.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PSquareQuantile(1.5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markersShouldStayOrderedForDuplicateHeavyStream() {
        PSquareQuantile q = new PSquareQuantile(0.5);
        Random rnd = new Random(11);
        for (int i = 0; i < 500; i++) {
            q.add(rnd.nextInt(3));  // 大量重复值——cell 定位与端标记改写的 torture
        }
        assertThat(q.estimate()).isBetween(0.0, 2.0);
        assertThat(q.count()).isEqualTo(500);
    }
}
