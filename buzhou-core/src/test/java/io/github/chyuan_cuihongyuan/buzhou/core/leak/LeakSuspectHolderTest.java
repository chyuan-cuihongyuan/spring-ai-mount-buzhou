package io.github.chyuan_cuihongyuan.buzhou.core.leak;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 泄漏疑似聚合 Holder 接线测试（spec 1615 / T2381–T2382 / impl 1168）：
 * 复合 listener（宿主+聚合器都收）与聚合排行读数（spec 839 孤类接线）。
 */
class LeakSuspectHolderTest {

    private static ResourceLeakDetector.LeakReport reportOf(String desc, long age) {
        return new ResourceLeakDetector.LeakReport(desc, age, null);
    }

    @Test
    void compositeFeedsBothHostListenerAndAggregator() {
        AtomicInteger hostReceived = new AtomicInteger();
        ResourceLeakDetector.LeakListener composite =
                LeakSuspectHolder.compositeWith(r -> hostReceived.incrementAndGet());
        composite.onLeak(reportOf("spill://a/s1/t1", 100));
        composite.onLeak(reportOf("spill://a/s1/t1", 300));
        composite.onLeak(reportOf("lease://other", 50));

        assertThat(hostReceived.get()).isEqualTo(3);
        LeakSuspectAggregator.Report report = LeakSuspectHolder.report();
        assertThat(report.totalLeaks()).isEqualTo(3);
        assertThat(report.suspects()).hasSize(2);
        assertThat(report.suspects().get(0).key()).isEqualTo("spill://a/s1/t1");
        assertThat(report.suspects().get(0).count()).isEqualTo(2);
        assertThat(report.suspects().get(0).maxAgeMillis()).isEqualTo(300);
    }

    @Test
    void nullHostListenerFeedsAggregatorOnly() {
        LeakSuspectHolder.install(null); // 重置
        ResourceLeakDetector.LeakListener composite = LeakSuspectHolder.compositeWith(null);
        composite.onLeak(reportOf("x", 10));
        assertThat(LeakSuspectHolder.report().totalLeaks()).isEqualTo(1);
        assertThat(LeakSuspectHolder.report().suspects()).hasSize(1);
    }
}
