package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 212 §B / T576：默认组合红队——四内置全开（干净过 / 重复行与规模越界
 * 各自触发对应期望名）。
 */
class DatasetExpectationsDefaultsTest {

    @Test
    void commonDefaultsCoverFourBuiltinExpectations() {
        DatasetExpectations suite = DatasetExpectations.ofCommonDefaults();
        assertThat(suite.validate(List.of(
                new EvalItem("1", "q1", "a1", null, null, Instant.now()),
                new EvalItem("2", "q2", "a2", null, null, Instant.now()))).passed()).isTrue();

        DatasetExpectations.Result dup = suite.validate(List.of(
                new EvalItem("1", "dup", "a", null, null, Instant.now()),
                new EvalItem("2", "dup", "b", null, null, Instant.now())));
        assertThat(dup.passed()).isFalse();
        assertThat(dup.findings()).extracting(DatasetExpectations.Finding::expectation)
                .contains("unique-inputs");

        assertThat(suite.validate(List.of()).passed()).isFalse(); // 空集触规模窗
    }
}
