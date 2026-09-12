package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 527 / T803–804：数据集 CSV 互操作——RFC 4180 转义/解析往返（逗号/
 * 引号/换行字段）、表头校验、空行容忍、导出行数、与 store 组合。
 */
class EvalDatasetCsvTest {

    @Test
    void roundTripPreservesQuotedAndMultilineFields() {
        List<EvalItem> items = List.of(
                new EvalItem(null, "plain input", "ok", null, null, null),
                new EvalItem(null, "含,逗号", "值\"带引号\"", null, null, null),
                new EvalItem(null, "多\n行输入", "多\r\n行期望", null, null, null));
        String csv = EvalDatasetCsv.toCsv(items);
        List<EvalItem> parsed = EvalDatasetCsv.fromCsv(csv);
        assertThat(parsed).hasSize(3);
        assertThat(parsed.get(1).input()).isEqualTo("含,逗号");
        assertThat(parsed.get(1).expected()).isEqualTo("值\"带引号\"");
        assertThat(parsed.get(2).input()).isEqualTo("多\n行输入");
        assertThat(parsed.get(2).expected()).isEqualTo("多\r\n行期望");
    }

    @Test
    void headerIsStrictButCaseTolerant() {
        assertThatThrownBy(() -> EvalDatasetCsv.fromCsv("foo,bar\n1,2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("input,expected");
        assertThat(EvalDatasetCsv.fromCsv("Input , EXPECTED\na,b"))
                .hasSize(1); // 宽松大小写与空白
    }

    @Test
    void blankLinesToleratedAndBlankCsvRejected() {
        assertThat(EvalDatasetCsv.fromCsv("input,expected\n\na,b\n")).hasSize(1);
        assertThatThrownBy(() -> EvalDatasetCsv.fromCsv("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exportWriterReturnsRowCount() throws Exception {
        StringWriter out = new StringWriter();
        long rows = EvalDatasetCsv.export(out, List.of(
                new EvalItem(null, "a", "b", null, null, null),
                new EvalItem(null, "c", "d", null, null, null)));
        assertThat(rows).isEqualTo(2);
        assertThat(out.toString()).startsWith(EvalDatasetCsv.HEADER);
    }
}
