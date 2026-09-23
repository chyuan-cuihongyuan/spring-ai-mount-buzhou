package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4031 / T6064：难度重定合同——准时不变、快减慢增、±4×
 * 钳制、powLimit 封顶、窗滚动、倒流 fail-fast、确定性回放。
 */
class DifficultyRetargetTest {

    private static final long SPACING = 10L;
    private static final int WINDOW = 6;
    private static final long POW_LIMIT = 1_000_000L;
    private static final long INITIAL = 1000L;

    /** 喂一个完整窗（首块定窗起 + window 个间隔），返回重定后目标。 */
    private static long feedWindow(DifficultyRetarget retarget, long firstTime, long spacing) {
        long time = firstTime;
        retarget.block(time);
        for (int i = 0; i < WINDOW; i++) {
            time += spacing;
            retarget.block(time);
        }
        return retarget.currentTarget();
    }

    @Test
    void onTimeWindowShouldKeepTargetUnchanged() {
        DifficultyRetarget retarget = new DifficultyRetarget(SPACING, WINDOW, POW_LIMIT, INITIAL);
        assertThat(feedWindow(retarget, 0, SPACING)).isEqualTo(1000L);   // 实际=目标 → 不变
    }

    @Test
    void fastWindowShouldRaiseDifficultyByActualRatio() {
        DifficultyRetarget retarget = new DifficultyRetarget(SPACING, WINDOW, POW_LIMIT, INITIAL);
        assertThat(feedWindow(retarget, 0, SPACING / 2)).isEqualTo(500L);   // 快 2× → 目标减半
    }

    @Test
    void slowWindowShouldClampAtQuadrupleNotOctuple() {
        DifficultyRetarget retarget = new DifficultyRetarget(SPACING, WINDOW, POW_LIMIT, INITIAL);
        assertThat(feedWindow(retarget, 0, SPACING * 8)).isEqualTo(4000L);   // 慢 8× → 钳制 ×4
    }

    @Test
    void powLimitShouldCapTheTarget() {
        DifficultyRetarget retarget = new DifficultyRetarget(SPACING, WINDOW, POW_LIMIT, 900_000L);
        assertThat(feedWindow(retarget, 0, SPACING * 8)).isEqualTo(POW_LIMIT);   // 钳后 3.6M 封顶 1M
    }

    @Test
    void windowShouldRollAfterRetarget() {
        DifficultyRetarget retarget = new DifficultyRetarget(SPACING, WINDOW, POW_LIMIT, INITIAL);
        feedWindow(retarget, 0, SPACING / 2);   // 第一窗快 2× → 500（窗尾 30，新窗自 30 滚动）
        assertThat(retarget.currentTarget()).isEqualTo(500L);
        assertThat(retarget.blocksToRetarget()).isEqualTo(WINDOW);   // 新窗从头计
        long time = 30L;
        for (int i = 0; i < WINDOW; i++) {
            time += SPACING;   // 新窗准时：重定块滚动后再集满 6 间隔
            retarget.block(time);
        }
        assertThat(retarget.currentTarget()).isEqualTo(500L);   // 准时 → 不变
        assertThat(retarget.blocksToRetarget()).isEqualTo(WINDOW);   // 重定即滚动——新窗刚开满余
    }

    @Test
    void firstBlockOnlyAnchorsWindow() {
        DifficultyRetarget retarget = new DifficultyRetarget(SPACING, WINDOW, POW_LIMIT, INITIAL);
        assertThat(retarget.block(100)).isEqualTo(1000L);
        assertThat(retarget.blocksToRetarget()).isEqualTo(WINDOW);
        assertThat(retarget.targetTimespan()).isEqualTo(60L);
    }

    @Test
    void sameArrivalSequenceShouldReplaySameTrajectory() {
        long[] times = {0, 5, 10, 12, 20, 30, 40, 50, 60};
        DifficultyRetarget first = new DifficultyRetarget(SPACING, WINDOW, POW_LIMIT, INITIAL);
        DifficultyRetarget second = new DifficultyRetarget(SPACING, WINDOW, POW_LIMIT, INITIAL);
        for (long time : times) {
            assertThat(first.block(time)).isEqualTo(second.block(time));
        }
        assertThat(first.currentTarget()).isEqualTo(second.currentTarget());
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new DifficultyRetarget(0, WINDOW, POW_LIMIT, INITIAL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DifficultyRetarget(SPACING, 0, POW_LIMIT, INITIAL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DifficultyRetarget(SPACING, WINDOW, POW_LIMIT, POW_LIMIT + 1))
                .isInstanceOf(IllegalArgumentException.class);
        DifficultyRetarget retarget = new DifficultyRetarget(SPACING, WINDOW, POW_LIMIT, INITIAL);
        retarget.block(100);
        assertThatThrownBy(() -> retarget.block(99))   // 时间倒流
                .isInstanceOf(IllegalArgumentException.class);
    }
}
