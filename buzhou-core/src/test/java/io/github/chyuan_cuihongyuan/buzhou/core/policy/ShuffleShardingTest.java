package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5038 / T6178：洗牌分片合同——圣像分配钉住（Python
 * 64 位语义预演）、确定性、子集大小与去重、路由一致性、
 * 重叠期望≈k²/n、fail-fast。
 */
class ShuffleShardingTest {

    private static final long SEED = 500L;

    private static final int SHARD_COUNT = 100;

    private static final int SHARDS_PER_TENANT = 2;

    private static final int TENANT_SWEEP = 200;

    private static final double MAX_AVG_OVERLAP = 0.1;

    @Test
    void assignmentsShouldMatchPrecomputedOracle() {
        ShuffleSharding sharding = new ShuffleSharding(SHARD_COUNT, SHARDS_PER_TENANT, SEED);
        assertThat(sharding.assign("tenant-a")).containsExactly(8, 52);
        assertThat(sharding.assign("tenant-b")).containsExactly(45, 66);
        assertThat(sharding.assign("tenant-c")).containsExactly(27, 36);
        assertThat(sharding.assign("tenant-d")).containsExactly(12, 68);
    }

    @Test
    void sameSeedShouldGiveSameAssignment() {
        ShuffleSharding first = new ShuffleSharding(SHARD_COUNT, SHARDS_PER_TENANT, SEED);
        ShuffleSharding second = new ShuffleSharding(SHARD_COUNT, SHARDS_PER_TENANT, SEED);
        assertThat(first.assign("tenant-a")).isEqualTo(second.assign("tenant-a"));
        assertThat(first.assign("tenant-b")).isEqualTo(second.assign("tenant-b"));
    }

    @Test
    void routesToShouldMatchAssignment() {
        ShuffleSharding sharding = new ShuffleSharding(SHARD_COUNT, SHARDS_PER_TENANT, SEED);
        List<Integer> shards = sharding.assign("tenant-a");
        for (int shard = 0; shard < SHARD_COUNT; shard++) {
            assertThat(sharding.routesTo("tenant-a", shard)).isEqualTo(shards.contains(shard));
        }
    }

    @Test
    void pairwiseOverlapShouldStayNearSquaredOverCount() {
        ShuffleSharding sharding = new ShuffleSharding(SHARD_COUNT, SHARDS_PER_TENANT, SEED);
        long totalOverlap = 0;
        for (int i = 0; i < TENANT_SWEEP; i++) {
            List<Integer> shards = sharding.assign("t" + i);
            assertThat(shards).hasSize(SHARDS_PER_TENANT).doesNotHaveDuplicates();
            for (int j = i + 1; j < TENANT_SWEEP; j++) {
                totalOverlap += sharding.overlap("t" + i, "t" + j);
            }
        }
        double pairs = (double) TENANT_SWEEP * (TENANT_SWEEP - 1) / 2;
        double average = totalOverlap / pairs;
        // 期望 k²/n = 2²/100 = 0.04（固定种子实测 0.0398）
        assertThat(average).isLessThan(MAX_AVG_OVERLAP);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new ShuffleSharding(0, 1, SEED)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShuffleSharding(10, 0, SEED)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ShuffleSharding(10, 11, SEED)).isInstanceOf(IllegalArgumentException.class);
        ShuffleSharding sharding = new ShuffleSharding(SHARD_COUNT, SHARDS_PER_TENANT, SEED);
        assertThatThrownBy(() -> sharding.assign(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sharding.assign("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sharding.routesTo("tenant-a", 100)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sharding.routesTo("tenant-a", -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
