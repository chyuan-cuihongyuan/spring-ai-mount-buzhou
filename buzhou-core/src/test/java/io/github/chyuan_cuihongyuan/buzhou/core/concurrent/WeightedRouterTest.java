package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 199 / T572：平滑加权路由回归——5:1:1 七选精确 / 平滑无连五爆发 /
 * 动态调权 / 单候选退化 / 边界与并发 smoke。
 */
class WeightedRouterTest {

    @Test
    void fiveOneOneDistributesExactlyAndSmoothly() {
        WeightedRouter<String> router = WeightedRouter.of(
                WeightedRouter.Pair.of("a", 5),
                WeightedRouter.Pair.of("b", 1),
                WeightedRouter.Pair.of("c", 1));

        List<String> seven = router.pickSequence(7);

        assertThat(seven.stream().filter("a"::equals).count()).isEqualTo(5);
        assertThat(seven.stream().filter("b"::equals).count()).isEqualTo(1);
        assertThat(seven.stream().filter("c"::equals).count()).isEqualTo(1);
        // 平滑：首 3 选不全是 a（Nginx 算法 5:1:1 首三为 a,a,b,a,a,c,a——无七连爆发）
        assertThat(seven.subList(0, 3).stream().filter("a"::equals).count()).isLessThanOrEqualTo(2);
        // 可复现：重建同构路由序列一致
        WeightedRouter<String> again = WeightedRouter.of(
                WeightedRouter.Pair.of("a", 5),
                WeightedRouter.Pair.of("b", 1),
                WeightedRouter.Pair.of("c", 1));
        assertThat(again.pickSequence(7)).isEqualTo(seven);
    }

    @Test
    void singleCandidateAlwaysPicked() {
        WeightedRouter<String> router = WeightedRouter.of(
                WeightedRouter.Pair.of("only", 3));
        assertThat(router.pickSequence(5)).containsOnly("only");
    }

    @Test
    void setWeightTakesEffectImmediately() {
        WeightedRouter<String> router = WeightedRouter.of(
                WeightedRouter.Pair.of("a", 1),
                WeightedRouter.Pair.of("b", 1));
        assertThat(router.pickSequence(2)).containsExactly("a", "b"); // 1:1

        router.setWeight("a", 3);
        List<String> four = router.pickSequence(4);
        assertThat(four.stream().filter("a"::equals).count()).isEqualTo(3); // 3:1 即时生效
        assertThat(router.weights()).containsEntry("a", 3);
    }

    @Test
    void emptyRouterPicksNothing() {
        WeightedRouter<String> router = new WeightedRouter<>();
        assertThat(router.pick()).isEmpty();
        assertThat(router.size()).isZero();
    }

    @Test
    void invalidWeightsRejected() {
        WeightedRouter<String> router = new WeightedRouter<>();
        assertThatThrownBy(() -> router.add("x", 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> router.add(null, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> router.setWeight("x", -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentPickingIsSafe() throws Exception {
        WeightedRouter<String> router = WeightedRouter.of(
                WeightedRouter.Pair.of("a", 2),
                WeightedRouter.Pair.of("b", 1));
        AtomicInteger picks = new AtomicInteger();
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch start = new CountDownLatch(1);
            List<java.util.concurrent.Future<?>> tasks = new java.util.ArrayList<>();
            for (int t = 0; t < 8; t++) {
                tasks.add(pool.submit(() -> {
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    for (int i = 0; i < 100; i++) {
                        router.pick();
                        picks.incrementAndGet();
                    }
                }));
            }
            start.countDown();
            for (java.util.concurrent.Future<?> task : tasks) {
                task.get(10, TimeUnit.SECONDS);
            }
        }
        assertThat(picks.get()).isEqualTo(800); // 无丢失无异常
        Map<String, Integer> weights = router.weights(); // 权重不被并发破坏
        assertThat(weights).containsEntry("a", 2).containsEntry("b", 1);
    }
}
