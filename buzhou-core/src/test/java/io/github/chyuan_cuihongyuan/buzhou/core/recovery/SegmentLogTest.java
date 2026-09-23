package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5028 / T6158：分段日志合同——段满滚动、超上限逐最旧
 * 段、readAll 跨段按序、畸形 fail-fast、确定性。
 */
class SegmentLogTest {

    private static final int CAP = 3;
    private static final int MAX_SEGMENTS = 2;

    @Test
    void fullSegmentShouldRollNewSegment() {
        SegmentLog log = new SegmentLog(CAP, 10);
        for (int i = 1; i <= 4; i++) {
            log.append("r" + i);
        }
        assertThat(log.segmentCount()).isEqualTo(2);   // 满 3 条滚动
        assertThat(log.totalAppended()).isEqualTo(4L);
        assertThat(log.droppedCount()).isZero();       // 未超段数上限
    }

    @Test
    void oldestSegmentShouldBeEvictedBeyondLimit() {
        SegmentLog log = new SegmentLog(CAP, MAX_SEGMENTS);
        for (int i = 1; i <= 7; i++) {
            log.append("r" + i);   // 第 7 条滚动新段——触发最旧段（r1-r3）淘汰
        }
        assertThat(log.segmentCount()).isEqualTo(MAX_SEGMENTS);
        assertThat(log.droppedCount()).isEqualTo(3L);              // 被逐首段 3 条诚实可见
        assertThat(log.readAll()).containsExactly("r4", "r5", "r6", "r7");
        assertThat(log.firstSurvivingLsn()).isEqualTo(4L);
    }

    @Test
    void readAllShouldStitchAcrossSegments() {
        SegmentLog log = new SegmentLog(CAP, MAX_SEGMENTS);
        for (int i = 1; i <= CAP * MAX_SEGMENTS; i++) {
            log.append("r" + i);
        }
        List<String> all = log.readAll();
        assertThat(all).hasSize(CAP * MAX_SEGMENTS);
        assertThat(all.get(0)).isEqualTo("r1");
        assertThat(all.get(all.size() - 1)).isEqualTo("r" + CAP * MAX_SEGMENTS);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new SegmentLog(0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SegmentLog(3, 0)).isInstanceOf(IllegalArgumentException.class);
        SegmentLog log = new SegmentLog(CAP, MAX_SEGMENTS);
        assertThatThrownBy(() -> log.append(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> log.append("")).isInstanceOf(IllegalArgumentException.class);
    }
}
