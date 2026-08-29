package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 134 §B / T459：数据集期望红队——非空输入/期望在场/输入唯一/规模窗口四内置
 * 期望；发现样本封顶 + 共 N 处计数（报告面有界）；空套件恒过诚实；summary 单行；
 * 名单与规模窗参数 fail-fast。借鉴：Great Expectations 数据合约。
 */
class DatasetExpectationsTest {

    private static EvalItem item(String input, String expected) {
        return new EvalItem("000001", input, expected, null, null, Instant.now());
    }

    @Test
    void cleanDatasetPassesWithSingleLineSummary() {
        DatasetExpectations suite = DatasetExpectations.of(
                DatasetExpectations.nonBlankInputs(),
                DatasetExpectations.expectedPresent(),
                DatasetExpectations.uniqueInputs(),
                DatasetExpectations.sizeBetween(1, 100));
        DatasetExpectations.Result result = suite.validate(List.of(
                item("问一", "答一"), item("问二", "答二")));

        assertThat(result.passed()).isTrue();
        assertThat(result.findings()).isEmpty();
        assertThat(result.summary())
                .isEqualTo("dataset expectations: PASS (2 items)");
    }

    @Test
    void dirtyRowsViolateRowExpectationsWithIndexAndPreview() {
        DatasetExpectations suite = DatasetExpectations.of(
                DatasetExpectations.nonBlankInputs(),
                DatasetExpectations.expectedPresent());
        DatasetExpectations.Result result = suite.validate(List.of(
                item("正常行", "答"), item(" ", "答"), item("行", " ")));

        assertThat(result.passed()).isFalse();
        assertThat(result.findings()).extracting(DatasetExpectations.Finding::itemIndex)
                .containsExactlyInAnyOrder(1, 2);
        assertThat(result.findings()).extracting(DatasetExpectations.Finding::expectation)
                .containsExactlyInAnyOrder("non-blank-inputs", "expected-present");
        assertThat(result.summary()).contains("FAIL").contains("expected-present");
    }

    @Test
    void duplicateInputsAndSizeWindowViolateAtDatasetLevel() {
        DatasetExpectations.Result dup = DatasetExpectations.of(DatasetExpectations.uniqueInputs())
                .validate(List.of(item("同问", "答一"), item("同问", "答二")));
        assertThat(dup.passed()).isFalse();
        assertThat(dup.findings()).hasSize(1);
        assertThat(dup.findings().get(0).itemIndex()).isEqualTo(-1);
        assertThat(dup.findings().get(0).detail()).contains("itemCount=2");

        DatasetExpectations.Result empty = DatasetExpectations
                .of(DatasetExpectations.sizeBetween(1, 10)).validate(List.of());
        assertThat(empty.passed()).isFalse();

        DatasetExpectations.Result huge = DatasetExpectations
                .of(DatasetExpectations.sizeBetween(0, 1))
                .validate(List.of(item("a", "b"), item("c", "d")));
        assertThat(huge.passed()).isFalse();
    }

    @Test
    void findingsCappedWithHonestTotalCount() {
        List<EvalItem> dirty = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) {
            dirty.add(item("行" + i, "")); // expected 全缺失
        }
        DatasetExpectations.Result result = DatasetExpectations
                .of(DatasetExpectations.expectedPresent()).validate(dirty);

        assertThat(result.passed()).isFalse();
        long samples = result.findings().stream()
                .filter(f -> f.itemIndex() >= 0).count();
        assertThat(samples).isEqualTo(DatasetExpectations.MAX_FINDINGS_PER_EXPECTATION);
        assertThat(result.findings()).extracting(DatasetExpectations.Finding::detail)
                .contains("共 25 处（样本封顶 "
                        + DatasetExpectations.MAX_FINDINGS_PER_EXPECTATION + "）");
    }

    @Test
    void noneSuiteAlwaysPassesAndCustomExpectationWorks() {
        assertThat(DatasetExpectations.none().validate(List.of()).passed()).isTrue();
        assertThat(DatasetExpectations.none().validate(List.of(item(" ", " "))).passed())
                .isTrue(); // 零期望 = 零意见

        DatasetExpectations.Expectation sourced = DatasetExpectations
                .named("reflow-requires-source", item -> item.sourceSessionId() != null);
        DatasetExpectations.Result result = DatasetExpectations.of(sourced)
                .validate(List.of(item("手工行", "答")));
        assertThat(result.passed()).isFalse();
        assertThat(result.findings().get(0).expectation()).isEqualTo("reflow-requires-source");
    }

    @Test
    void blankNamesAndBadWindowsFailFast() {
        assertThatThrownBy(() -> DatasetExpectations.named(" ", item -> true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DatasetExpectations.sizeBetween(3, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DatasetExpectations.of((DatasetExpectations.Expectation) null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
