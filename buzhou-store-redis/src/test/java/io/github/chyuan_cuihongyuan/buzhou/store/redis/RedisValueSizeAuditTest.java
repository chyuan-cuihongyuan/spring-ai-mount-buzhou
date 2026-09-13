package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 801 / T1104：大值审计回归——族归类/阈值定级/Top 排名/族聚合/脏采样。
 */
class RedisValueSizeAuditTest {

    @Test
    void classifiesKnownFamilies() {
        assertThat(RedisValueSizeAudit.familyOf("buzhou:msg:s1")).isEqualTo("msg");
        assertThat(RedisValueSizeAudit.familyOf("buzhou:idx:info:s1")).isEqualTo("idx");
        assertThat(RedisValueSizeAudit.familyOf("buzhou:sum:s1:v:3")).isEqualTo("sum");
        assertThat(RedisValueSizeAudit.familyOf("buzhou:state:s1:k")).isEqualTo("state");
        assertThat(RedisValueSizeAudit.familyOf("buzhou:lease:s1")).isEqualTo("lease");
        assertThat(RedisValueSizeAudit.familyOf("buzhou:obs:s1:span:sp1")).isEqualTo("obs");
        assertThat(RedisValueSizeAudit.familyOf("buzhou:semvec:3")).isEqualTo("semvec");
        assertThat(RedisValueSizeAudit.familyOf("buzhou:statekeys:s1")).isEqualTo("statekeys");
        assertThat(RedisValueSizeAudit.familyOf("buzhou:msgid:m1")).isEqualTo("msgid");
        assertThat(RedisValueSizeAudit.familyOf("weird:key")).isEqualTo("other");
        assertThat(RedisValueSizeAudit.familyOf(null)).isEqualTo("other");
    }

    @Test
    void severityAndRanking() {
        Map<String, Long> sizes = new HashMap<>();
        sizes.put("buzhou:msg:big", 50_000L);   // CRIT
        sizes.put("buzhou:sum:s1:v:2", 12_000L); // WARN
        sizes.put("buzhou:state:s1:k", 9_999L);  // 低于阈值——不进 top，但计入族聚合
        sizes.put("buzhou:obs:s1:span:sp", 30_000L); // CRIT

        RedisValueSizeAudit.Report report = RedisValueSizeAudit.audit(sizes, 10_000);

        assertThat(report.top()).hasSize(3);
        assertThat(report.top().get(0).key()).isEqualTo("buzhou:msg:big");
        assertThat(report.top().get(0).severity()).isEqualTo("CRIT");
        assertThat(report.top().get(2).key()).isEqualTo("buzhou:sum:s1:v:2");
        assertThat(report.top().get(2).severity()).isEqualTo("WARN");
        assertThat(report.top()).allSatisfy(f -> assertThat(f.hint()).isNotBlank());

        assertThat(report.sampledKeys()).isEqualTo(4);
        assertThat(report.sampledBytes()).isEqualTo(101_999L);
        assertThat(report.families().get(0).family()).isEqualTo("msg");
        RedisValueSizeAudit.FamilyTotal stateFamily = report.families().stream()
                .filter(f -> f.family().equals("state")).findFirst().orElseThrow();
        assertThat(stateFamily.keys()).isEqualTo(1);
        assertThat(stateFamily.overThreshold()).isZero();
        assertThat(stateFamily.bytes()).isEqualTo(9_999L);
    }

    @Test
    void topIsCapped() {
        Map<String, Long> sizes = new HashMap<>();
        for (int i = 0; i < RedisValueSizeAudit.TOP_LIMIT + 15; i++) {
            sizes.put("buzhou:msg:s" + i, 20_000L + i);
        }
        RedisValueSizeAudit.Report report = RedisValueSizeAudit.audit(sizes, 10_000);
        assertThat(report.top()).hasSize(RedisValueSizeAudit.TOP_LIMIT);
        assertThat(report.top().get(0).bytes()).isEqualTo(20_000L + RedisValueSizeAudit.TOP_LIMIT + 14);
    }

    @Test
    void dirtySamplesSkippedAndEmptyMapYieldsEmptyReport() {
        Map<String, Long> sizes = new HashMap<>();
        sizes.put(null, 100L);
        sizes.put("k", null);
        sizes.put("neg", -5L);
        RedisValueSizeAudit.Report report = RedisValueSizeAudit.audit(sizes, 10);
        assertThat(report.top()).isEmpty();
        assertThat(report.families()).isEmpty();
        assertThat(report.sampledKeys()).isEqualTo(3); // 输入即事实——脏样本占位计数
        assertThat(report.sampledBytes()).isZero();
    }

    @Test
    void failFastOnBadThreshold() {
        assertThatThrownBy(() -> RedisValueSizeAudit.audit(Map.of(), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RedisValueSizeAudit.audit(null, 10))
                .isInstanceOf(NullPointerException.class);
    }
}
