package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7021：WriteAheadLog 合同——先落日志后动状态。
 * 追加重放全等；分段滚动；CRC 损坏 fail-fast 携带 LSN；
 * LSN 单调；fail-fast。
 */
class WriteAheadLogTest {

    @Test
    void appendReplayRoundTrip() {
        WriteAheadLog wal = new WriteAheadLog(4);
        wal.append("alpha".getBytes());
        wal.append("beta".getBytes());
        wal.append("gamma".getBytes());
        assertThat(wal.lastLsn()).isEqualTo(3);
        assertThat(wal.totalRecords()).isEqualTo(3);
        assertThat(wal.replay()).hasSize(3);
        assertThat(new String(wal.replay().get(1))).isEqualTo("beta");
        assertThat(wal.segmentCount()).isEqualTo(1);
    }

    @Test
    void segmentRolling() {
        WriteAheadLog wal = new WriteAheadLog(2);
        for (int i = 0; i < 5; i++) {
            wal.append(("r" + i).getBytes());
        }
        assertThat(wal.segmentCount()).isEqualTo(3);
        assertThat(wal.segment(0).records()).hasSize(2);
        assertThat(wal.segment(1).records()).hasSize(2);
        assertThat(wal.segment(2).records()).hasSize(1);
        assertThat(wal.replay()).hasSize(5);
        assertThat(new String(wal.replay().get(4))).isEqualTo("r4");
    }

    @Test
    void corruptedRecordFailsFastWithLsn() {
        WriteAheadLog wal = new WriteAheadLog(4);
        wal.append("first".getBytes());
        wal.append("second".getBytes());
        WriteAheadLog.Segment segment = wal.segment(0);
        byte[] mirrored = segment.records().get(0).payload();
        mirrored[0] ^= 0x55;
        assertThatThrownBy(wal::replay)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LSN 1");
        WriteAheadLog intact = new WriteAheadLog(4);
        intact.append("clean".getBytes());
        assertThat(intact.replay()).hasSize(1);
    }

    @Test
    void failFastContract() {
        assertThatThrownBy(() -> new WriteAheadLog(0)).isInstanceOf(IllegalArgumentException.class);
        WriteAheadLog wal = new WriteAheadLog(2);
        assertThatThrownBy(() -> wal.append(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wal.append(new byte[0])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wal.segment(5)).isInstanceOf(IllegalArgumentException.class);
    }
}
