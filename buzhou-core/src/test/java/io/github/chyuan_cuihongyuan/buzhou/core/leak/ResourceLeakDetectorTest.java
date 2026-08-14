package io.github.chyuan_cuihongyuan.buzhou.core.leak;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-41 / spec 13 §T66 泄漏检测：未释放即 GC → 上报（PARANOID 全采样 + 阈值 0
 * 保证确定性）；显式 close 不报；DISABLED 零追踪；阈值内 GC 视为噪声；
 * 出租时长阈值过滤；ACTIVE 采样含出租栈。
 */
class ResourceLeakDetectorTest {

    @AfterEach
    void reset() {
        LeakDetectorHolder.reset();
    }

    private static void gcUntil(Runnable condition, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            System.gc();
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            // 触发条件由调用方以 lambda 断言（cleaner 线程异步）
            try {
                condition.run();
                return;
            } catch (RuntimeException ignored) {
                // 条件未满足，继续 GC
            }
        }
    }

    @Test
    void unreleasedHandleIsReportedAfterGc() {
        List<ResourceLeakDetector.LeakReport> reports = new CopyOnWriteArrayList<>();
        ResourceLeakDetector detector = new ResourceLeakDetector(
                ResourceLeakDetector.LeakLevel.PARANOID, Duration.ZERO, reports::add);
        AtomicInteger released = new AtomicInteger();

        dropWithoutClose(detector);
        gcUntil(() -> {
            if (reports.isEmpty()) {
                throw new IllegalStateException("not yet");
            }
        }, 5000);

        assertThat(reports).hasSize(1);
        assertThat(reports.getFirst().description()).isEqualTo("session:s-leak");
        assertThat(reports.getFirst().ageMillis()).isGreaterThanOrEqualTo(0);
        assertThat(reports.getFirst().acquisitionStack()).contains("ResourceLeakDetectorTest");
        assertThat(detector.leaksReported()).isEqualTo(1);
        assertThat(released.get()).isZero();
    }

    private void dropWithoutClose(ResourceLeakDetector detector) {
        // 句柄丢弃且不 close（局部作用域结束即可 GC）
        detector.track("session:s-leak");
    }

    @Test
    void closedHandleIsNeverReported() {
        List<ResourceLeakDetector.LeakReport> reports = new CopyOnWriteArrayList<>();
        ResourceLeakDetector detector = new ResourceLeakDetector(
                ResourceLeakDetector.LeakLevel.PARANOID, Duration.ZERO, reports::add);
        try (ResourceLeakDetector.LeakHandle handle = detector.track("lease:s-ok")) {
            assertThat(handle).isNotNull();
        }
        System.gc();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(reports).isEmpty();
        assertThat(detector.leaksReported()).isZero();
    }

    @Test
    void disabledNeverTracksAndSimpleSamples() {
        ResourceLeakDetector disabled = new ResourceLeakDetector(
                ResourceLeakDetector.LeakLevel.DISABLED, Duration.ZERO, null);
        assertThat(disabled.track("x")).isSameAs(ResourceLeakDetector.LeakHandle.NOOP);
        assertThat(disabled.tracked()).isZero();

        // SIMPLE：1/128 采样（非全量）——tracked 计数照常、采样标志不可全真
        ResourceLeakDetector simple = new ResourceLeakDetector(
                ResourceLeakDetector.LeakLevel.SIMPLE, Duration.ZERO, null);
        for (int i = 0; i < 100; i++) {
            simple.track("t" + i).close();
        }
        assertThat(simple.tracked()).isEqualTo(100);
        assertThat(simple.activeHandles()).isZero();
    }

    @Test
    void youngGcBelowAgeThresholdIsFilteredAsNoise() {
        List<ResourceLeakDetector.LeakReport> reports = new CopyOnWriteArrayList<>();
        ResourceLeakDetector detector = new ResourceLeakDetector(
                ResourceLeakDetector.LeakLevel.PARANOID, Duration.ofHours(1), reports::add);
        dropWithoutClose(detector);
        gcUntil(() -> {
            System.gc();
        }, 1000);
        assertThat(reports).isEmpty(); // 阈值 1h：短命对象 GC 不报
    }

    @Test
    void holderInstallAndReset() {
        ResourceLeakDetector custom = new ResourceLeakDetector(
                ResourceLeakDetector.LeakLevel.ADVANCED, Duration.ZERO, null);
        LeakDetectorHolder.install(custom);
        assertThat(LeakDetectorHolder.detector()).isSameAs(custom);
        LeakDetectorHolder.reset();
        assertThat(LeakDetectorHolder.detector().level())
                .isEqualTo(ResourceLeakDetector.LeakLevel.SIMPLE);
    }
}
