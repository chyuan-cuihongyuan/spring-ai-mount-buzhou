package io.github.chyuan_cuihongyuan.buzhou.core.cache;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1880 / T2962：SCAN 游标——全周游序列、不重、反转递增、畸形。 */
class ScanCursorCodecTest {

    /** bits=2 全周游：0→2→1→3→0（Redis 经典序），绕满精确归零。 */
    @Test
    void twoBitCycleVisitsAllThenZero() {
        assertThat(ScanCursorCodec.nextCursor(0, 2)).isEqualTo(2L);
        assertThat(ScanCursorCodec.nextCursor(2, 2)).isEqualTo(1L);
        assertThat(ScanCursorCodec.nextCursor(1, 2)).isEqualTo(3L);
        assertThat(ScanCursorCodec.nextCursor(3, 2)).isZero();
        assertThat(ScanCursorCodec.isComplete(0)).isTrue();
        assertThat(ScanCursorCodec.isComplete(3)).isFalse();
    }

    /** bits=3 全周游：8 桶不重且覆盖全部非零桶。 */
    @Test
    void threeBitCycleNoRepeatFullCover() {
        Set<Long> visited = new HashSet<>();
        long cursor = 0;
        int steps = 0;
        while (!ScanCursorCodec.isComplete(cursor = ScanCursorCodec.nextCursor(cursor, 3))) {
            visited.add(cursor);
            steps++;
        }
        assertThat(steps).isEqualTo(7);
        assertThat(visited).hasSize(7);
        assertThat(visited).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L, 7L);
    }

    /** 反转空间严格递增：同尺寸下 rev(next) > rev(cursor)（归零步除外）。 */
    @Test
    void reversedSpaceStrictlyIncreases() {
        long cursor = 0;
        long prevReversed = 0;
        for (int i = 0; i < 6; i++) {
            long next = ScanCursorCodec.nextCursor(cursor, 3);
            long reversed = Long.reverse(next) >>> 61;
            assertThat(reversed).isGreaterThan(prevReversed);
            prevReversed = reversed;
            cursor = next;
        }
    }

    /** 周游长度幂断言；畸形三型 fail-fast。 */
    @Test
    void cycleLengthAndMalformed() {
        assertThat(ScanCursorCodec.cycleLength(2)).isEqualTo(4L);
        assertThat(ScanCursorCodec.cycleLength(20)).isEqualTo(1L << 20);
        assertThatThrownBy(() -> ScanCursorCodec.nextCursor(-1, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("游标不能为负");
        assertThatThrownBy(() -> ScanCursorCodec.nextCursor(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tableBits 须在 [1,63]");
        assertThatThrownBy(() -> ScanCursorCodec.nextCursor(0, 64))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tableBits 须在 [1,63]");
    }
}
