package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.GroupCommitLog.GroupSync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5013 / T6128：组提交合同——同组合并、新组切换、空组
 * 空同步、LSN 单调、畸形 fail-fast。
 */
class GroupCommitLogTest {

    @Test
    void groupShouldMergeAppendsIntoOneSync() {
        GroupCommitLog log = new GroupCommitLog();
        long first = log.append("r1");
        long second = log.append("r2");
        long third = log.append("r3");
        assertThat(first).isEqualTo(1L);
        assertThat(third).isEqualTo(3L);
        GroupSync sync = log.sync();
        assertThat(sync.records()).isEqualTo(3);
        assertThat(sync.fromLsn()).isEqualTo(1L);
        assertThat(sync.toLsn()).isEqualTo(3L);
        assertThat(log.durableUpto()).isEqualTo(3L);
    }

    @Test
    void syncShouldStartNewGroup() {
        GroupCommitLog log = new GroupCommitLog();
        log.append("r1");
        assertThat(log.sync().records()).isEqualTo(1);
        long next = log.append("r2");   // 新组——LSN 连续不重置
        assertThat(next).isEqualTo(2L);
        GroupSync sync = log.sync();
        assertThat(sync.records()).isEqualTo(1);
        assertThat(sync.fromLsn()).isEqualTo(2L);
        assertThat(sync.toLsn()).isEqualTo(2L);
        assertThat(log.durableUpto()).isEqualTo(2L);
    }

    @Test
    void emptyGroupShouldSyncWithoutAdvancing() {
        GroupCommitLog log = new GroupCommitLog();
        GroupSync empty = log.sync();
        assertThat(empty.records()).isZero();
        assertThat(empty.fromLsn()).isEqualTo(-1L);
        assertThat(log.durableUpto()).isZero();   // 上沿不变
    }

    @Test
    void durableUptoShouldBeMonotonic() {
        GroupCommitLog log = new GroupCommitLog();
        log.append("r1");
        log.append("r2");
        assertThat(log.sync().records()).isEqualTo(2);
        long afterFirst = log.durableUpto();
        log.append("r3");
        GroupSync sync = log.sync();
        assertThat(sync.toLsn()).isEqualTo(3L);
        assertThat(log.durableUpto()).isGreaterThanOrEqualTo(afterFirst);
        assertThat(log.durableUpto()).isEqualTo(3L);
    }

    @Test
    void invalidRecordsShouldFailFast() {
        GroupCommitLog log = new GroupCommitLog();
        assertThatThrownBy(() -> log.append(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> log.append("")).isInstanceOf(IllegalArgumentException.class);
    }
}
