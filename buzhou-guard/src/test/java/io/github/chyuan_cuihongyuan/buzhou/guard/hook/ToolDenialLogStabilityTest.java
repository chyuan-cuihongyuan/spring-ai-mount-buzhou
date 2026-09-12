package io.github.chyuan_cuihongyuan.buzhou.guard.hook;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ToolDenialLog 排序稳定性补验（spec 737 / T1027–T1028 / impl 541）：并列
 * 字典序稳定、重复记录累加、窗口滑动幂等。
 */
class ToolDenialLogStabilityTest {

    @Test
    void tiesSortedByRoleThenTool() {
        ToolDenialLog log = new ToolDenialLog();
        log.record("b-role", "z-tool", ToolDenialLog.Reason.UNAUTHORIZED, 1);
        log.record("a-role", "z-tool", ToolDenialLog.Reason.UNAUTHORIZED, 2);
        log.record("a-role", "a-tool", ToolDenialLog.Reason.UNAUTHORIZED, 3);

        List<String> keys = List.copyOf(log.topDenials().keySet());

        assertThat(keys).containsExactly("a-role->a-tool", "a-role->z-tool", "b-role->z-tool");
    }

    @Test
    void repeatedDenialsAccumulate() {
        ToolDenialLog log = new ToolDenialLog();
        for (int i = 0; i < 5; i++) {
            log.record("viewer", "write_file", ToolDenialLog.Reason.UNAUTHORIZED, i);
        }
        assertThat(log.topDenials()).containsEntry("viewer->write_file", 5L);
    }

    @Test
    void ringWindowSlidesIdempotently() {
        ToolDenialLog log = new ToolDenialLog();
        for (int i = 0; i < ToolDenialLog.RING_CAPACITY + 10; i++) {
            log.record("r", "t", ToolDenialLog.Reason.UNAUTHORIZED, i);
        }
        int first = log.entries().size();
        int firstTop = log.topDenials().size();
        // 再查一次——读面零副作用
        assertThat(log.entries().size()).isEqualTo(first);
        assertThat(log.topDenials().size()).isEqualTo(firstTop);
        assertThat(first).isEqualTo(ToolDenialLog.RING_CAPACITY);
    }
}
