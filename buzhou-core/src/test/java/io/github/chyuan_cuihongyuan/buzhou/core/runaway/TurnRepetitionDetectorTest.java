package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 326 / impl-349：重复检测回归——达窗 fire/阈下不 fire/不相似解闩重计/
 * 闩住不刷屏/空输出归 1/构造校验。
 */
class TurnRepetitionDetectorTest {

    @Test
    void firesAtWindowWithSimilarity() {
        TurnRepetitionDetector detector = new TurnRepetitionDetector(3, 80);
        assertThat(detector.record("正在 查询 数据")).isEmpty(); // run 1
        assertThat(detector.record("正在 查询 数据")).isEmpty(); // run 2
        Optional<TurnRepetitionDetector.Verdict> verdict =
                detector.record("正在 查询 数据"); // run 3 = 窗
        assertThat(verdict).isPresent();
        assertThat(verdict.get().runLength()).isEqualTo(3);
        assertThat(verdict.get().similarity()).isEqualTo(1.0);
    }

    @Test
    void belowSimilarityThresholdDoesNotAccumulate() {
        TurnRepetitionDetector detector = new TurnRepetitionDetector(3, 80);
        detector.record("苹果 香蕉 橙子 西瓜");
        assertThat(detector.record("苹果 香蕉 草莓 蓝莓 葡萄"))
                .as("交集 2/并集 7 ≈ 0.29 < 0.80——不相似").isEmpty();
        assertThat(detector.currentRun()).isEqualTo(1);
    }

    @Test
    void dissimilarOutputRelatchesAndCanRefire() {
        TurnRepetitionDetector detector = new TurnRepetitionDetector(2, 80);
        assertThat(detector.record("同 一 句")).isEmpty();
        assertThat(detector.record("同 一 句")).isPresent(); // fire
        assertThat(detector.record("同 一 句"))
                .as("闩住——本 run 不再 fire 防刷屏").isEmpty();
        detector.record("完全 不同 的 内容"); // 不相似——解闩
        assertThat(detector.record("又 一样 了")).isEmpty();
        assertThat(detector.record("又 一样 了"))
                .as("再打转——解闩后可再 fire").isPresent();
    }

    @Test
    void blankOutputResetsRunAndLatch() {
        TurnRepetitionDetector detector = new TurnRepetitionDetector(2, 80);
        detector.record("复读 机 开始");
        assertThat(detector.record("复读 机 开始")).isPresent();
        detector.record(""); // 空转不是复读——解闩归 1
        assertThat(detector.currentRun()).isEqualTo(1);
        assertThat(detector.record("复读 机 开始")).isEmpty();
        assertThat(detector.record("复读 机 开始")).isPresent();
    }

    @Test
    void resetClearsHistory() {
        TurnRepetitionDetector detector = new TurnRepetitionDetector(2, 80);
        detector.record("a b");
        detector.reset();
        assertThat(detector.currentRun()).isZero();
        assertThat(detector.record("a b")).isEmpty(); // 首条 run 1
    }

    @Test
    void constructorValidates() {
        assertThatThrownBy(() -> new TurnRepetitionDetector(1, 80))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TurnRepetitionDetector(3, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TurnRepetitionDetector(3, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
