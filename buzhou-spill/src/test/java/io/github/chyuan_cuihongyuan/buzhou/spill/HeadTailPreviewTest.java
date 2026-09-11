package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spill 预览头尾语义测试（spec 619 / T888–T889 / impl 472，ripgrep context 思想）：
 * 截断预览含头与尾 + 省略标注；短内容原样；JSON 数组分页预览路径不受影响。
 */
class HeadTailPreviewTest {

    private static final String LIST_PREVIEW_ITEMS = "20";

    /** 截断预览：头 3/4 + 省略标注（含 omitted 数与 read_range 指引）+ 尾 1/4。 */
    @Test
    void truncatedPreviewContainsHeadAndTail() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append(String.format("行%03d：数据内容xxxxxxxxxx\n", i));
        }
        String content = sb.toString();

        String preview = RangeReadEngine.previewOf(content, 400, 20);

        assertThat(preview).contains("行000");
        assertThat(preview).contains("行099");           // 尾部可见（旧实现只有头）
        assertThat(preview).contains("中间省略");
        assertThat(preview).contains("read_range");
        int omitted = content.length() - 300 - 100;
        assertThat(preview).contains("省略 " + omitted + " 字符");
    }

    /** 短内容（预算内）原样返回，无省略标注。 */
    @Test
    void shortContentUnchanged() {
        assertThat(RangeReadEngine.previewOf("短结果", 2048, 20)).isEqualTo("短结果");
    }

    /** JSON 数组分页预览路径不受头尾语义影响（列表走 readPage 首页）。 */
    @Test
    void jsonArrayPreviewStillPaged() {
        String json = "[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\"]";

        String preview = RangeReadEngine.previewOf(json, 5, 3);

        assertThat(preview).contains("\"items\"");
        assertThat(preview).contains("\"totalCount\"");
    }
}
