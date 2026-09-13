package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 832 / T1166：技能加载延迟回归——分位数精确/溢出桶/slowest 排序/
 * 脏入参忽略/未知 null。
 */
class SkillLoadLatencyTest {

    @Test
    void percentilesPerSkill() {
        SkillLoadLatency latency = new SkillLoadLatency();
        for (long ms = 1; ms <= 100; ms++) {
            latency.record("search", ms);
        }
        SkillLoadLatency.SkillLatency stats = latency.stats("search");
        // 环只存最近 32 个样本（69..100）
        assertThat(stats.loads()).isEqualTo(SkillLoadLatency.RING);
        assertThat(stats.p50Millis()).isEqualTo(84);  // rank ⌈0.5·32⌉=16 → 69+15
        assertThat(stats.p95Millis()).isEqualTo(99);  // rank ⌈0.95·32⌉=31 → 69+30
        assertThat(stats.maxMillis()).isEqualTo(100);
        assertThat(latency.stats("nope")).isNull();
    }

    @Test
    void ringEvictsOldSamples() {
        SkillLoadLatency latency = new SkillLoadLatency();
        for (long ms = 1; ms <= SkillLoadLatency.RING + 20; ms++) {
            latency.record("report", ms);
        }
        SkillLoadLatency.SkillLatency stats = latency.stats("report");
        assertThat(stats.loads()).isEqualTo(SkillLoadLatency.RING); // 环容量封顶
        assertThat(stats.maxMillis()).isEqualTo(SkillLoadLatency.RING + 20); // max 累计不丢
    }

    @Test
    void overflowBucketBeyondCap() {
        SkillLoadLatency latency = new SkillLoadLatency();
        for (int i = 0; i < SkillLoadLatency.MAX_SKILLS; i++) {
            latency.record("skill" + i, 10);
        }
        latency.record("extra-skill", 500); // 超封顶 → 溢出桶
        assertThat(latency.stats("extra-skill")).isNull();
        assertThat(latency.stats(SkillLoadLatency.OVERFLOW).loads()).isEqualTo(1);
        assertThat(latency.stats(SkillLoadLatency.OVERFLOW).p95Millis()).isEqualTo(500);
    }

    @Test
    void slowestSortsByP95Descending() {
        SkillLoadLatency latency = new SkillLoadLatency();
        latency.record("fast", 5);
        latency.record("fast", 6);
        latency.record("slow", 200);
        latency.record("slow", 300);

        List<SkillLoadLatency.SkillLatency> ranked = latency.slowest();
        assertThat(ranked).hasSize(2);
        assertThat(ranked.get(0).skill()).isEqualTo("slow");
        assertThat(ranked.get(1).skill()).isEqualTo("fast");
    }

    @Test
    void dirtyInputsIgnored() {
        SkillLoadLatency latency = new SkillLoadLatency();
        latency.record(null, 10);
        latency.record("  ", 10);
        latency.record("s", -1);
        assertThat(latency.slowest()).isEmpty();
        assertThat(latency.stats("s")).isNull();
    }
}
