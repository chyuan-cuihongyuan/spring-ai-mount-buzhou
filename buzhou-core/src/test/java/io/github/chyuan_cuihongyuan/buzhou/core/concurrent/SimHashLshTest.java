package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SimHashLsh 契约测试（spec 10004 / X10010）：抽屉原理零漏检圣像
 * （与全库暴力比对逐一对拍全等）+ 多分块档 + fail-fast + 确定性。
 */
class SimHashLshTest {

    @Test
    void shouldMatchBruteForceExactly() {
        Random random = new Random(20260930L);
        SimHashLsh index = new SimHashLsh(4);
        long[] fps = new long[3000];
        for (int i = 0; i < fps.length; i++) {
            fps[i] = random.nextLong();
            index.add(fps[i], i);
        }
        for (int probe = 0; probe < 200; probe++) {
            long query = random.nextLong();
            List<Integer> expected = new ArrayList<>();
            for (int i = 0; i < fps.length; i++) {
                if (Long.bitCount(query ^ fps[i]) <= 3) {
                    expected.add(i);
                }
            }
            assertThat(index.query(query, 3)).as("probe %d 与暴力比对全等", probe)
                    .containsExactlyElementsOf(expected);
        }
    }

    @Test
    void shouldFindAllNearDuplicatesAfterBitFlips() {
        Random random = new Random(7L);
        SimHashLsh index = new SimHashLsh(4);
        long base = random.nextLong();
        index.add(base, 0);
        for (int i = 1; i <= 500; i++) {
            long variant = base;
            for (int flip = 0; flip < 3; flip++) {
                variant ^= 1L << random.nextInt(64);
            }
            index.add(variant, i);
            assertThat(index.query(variant, 3).contains(i)).isTrue();
        }
    }

    @Test
    void shouldWorkOnOtherBlockTiers() {
        for (int blocks : new int[]{2, 8}) {
            SimHashLsh index = new SimHashLsh(blocks);
            Random random = new Random(blocks);
            java.util.Map<Integer, Long> truth = new java.util.HashMap<>();
            for (int i = 0; i < 400; i++) {
                long fp = random.nextLong();
                truth.put(i, fp);
                index.add(fp, i);
            }
            for (int probe = 0; probe < 50; probe++) {
                long query = random.nextLong();
                for (int id : index.query(query, blocks - 1)) {
                    assertThat(Long.bitCount(query ^ truth.get(id)))
                            .as("blocks=%d 验距", blocks).isLessThanOrEqualTo(blocks - 1);
                }
            }
        }
    }

    @Test
    void shouldFailFastOnContractViolations() {
        assertThatThrownBy(() -> new SimHashLsh(3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SimHashLsh(0)).isInstanceOf(IllegalArgumentException.class);
        SimHashLsh index = new SimHashLsh(4);
        assertThatThrownBy(() -> index.add(1L, -1)).isInstanceOf(IllegalArgumentException.class);
        index.add(42L, 1);
        assertThatThrownBy(() -> index.add(43L, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.query(42L, 4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> index.query(42L, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldBeDeterministicOrder() {
        Random random = new Random(99L);
        long[] fps = new long[200];
        SimHashLsh first = new SimHashLsh(4);
        SimHashLsh second = new SimHashLsh(4);
        for (int i = 0; i < fps.length; i++) {
            fps[i] = random.nextLong();
            first.add(fps[i], i);
            second.add(fps[i], i);
        }
        long query = random.nextLong();
        assertThat(first.query(query, 3)).isEqualTo(second.query(query, 3));
    }
}
