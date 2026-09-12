package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ToolTimingAggregator 并发正确性压测（spec 747 / T1043–T1044 / impl 549）：
 * 并行 record 不变量（count/total/max）+ 多工具隔离 + windowedMax 一致。
 */
class ToolTimingAggregatorConcurrencyTest {

    @Test
    void concurrentRecordsPreserveInvariants() {
        ToolTimingAggregator aggregator = new ToolTimingAggregator();
        int threads = 8;
        int iterations = 500;
        int totalOps = threads * iterations;

        java.util.stream.IntStream.range(0, totalOps).parallel().forEach(i -> {
            if (ThreadLocalRandom.current().nextBoolean()) {
                Thread.yield(); // 加大交错概率
            }
            aggregator.record("hot-tool", 100L + (i % threads), false);
        });

        ToolTimingAggregator.ToolTiming stats = aggregator.stats().get("hot-tool");
        assertThat(stats.count()).isEqualTo(totalOps); // count 守恒
        // total = Σ(100 + i%8)：每 8 次一周期和 28
        long expectedTotal = 100L * totalOps + (long) (totalOps / threads) * 28;
        assertThat(stats.totalNanos()).isEqualTo(expectedTotal);
        assertThat(stats.maxNanos()).isEqualTo(107L); // 理论峰值
        assertThat(aggregator.windowedMax().get("hot-tool")).isEqualTo(stats.maxNanos()); // 同窗一致
    }

    @Test
    void multiToolIsolationUnderContention() {
        ToolTimingAggregator aggregator = new ToolTimingAggregator();
        java.util.stream.IntStream.range(0, 4).parallel().forEach(t -> {
            String tool = "tool-" + t;
            for (int i = 0; i < 300; i++) {
                aggregator.record(tool, 10L, false);
            }
        });
        assertThat(aggregator.stats()).hasSize(4);
        aggregator.stats().values().forEach(s -> assertThat(s.count()).isEqualTo(300));
    }
}
