package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-03 / T43 head+tail 窗口回读风味：head / tail / head_tail 窗口 + <b>显式中段省略标记</b>
 * （省略量 + offset 区间 + 回读指引）；原始字节完整保留、可按标记区间无损回取。
 */
class RangeReadWindowTest {

    private static final String CONTENT = "HEAD-PART：" + "a".repeat(200)
            + "MIDDLE-PART：" + "m".repeat(200)
            + "TAIL-PART：" + "z".repeat(200);

    @Test
    void headTailWindowKeepsBothEndsWithExplicitOmissionMarker() {
        RangeReadResult result = RangeReadEngine.read(CONTENT,
                RangeReadRequest.bytesWindow(RangeReadRequest.Window.HEAD_TAIL, 30, 30));

        assertThat(result.content()).startsWith("HEAD-PART：");
        assertThat(result.content()).contains("omitted ");
        // 尾窗 = 末尾 30 字符（纯 z）；「TAIL-PART：」标签落在被省略的中段
        assertThat(result.content()).endsWith("z".repeat(30));
        assertThat(result.content()).doesNotContain("TAIL-PART：");
        // 标记含省略量与精确区间
        assertThat(result.content()).contains("offset 30.." + (CONTENT.length() - 30));
        assertThat(result.content()).contains("; refetch via mode=bytes]");
        // 窗口模式自带精确标记，不再叠加通用截断后缀
        assertThat(result.truncated()).isFalse();
    }

    @Test
    void headWindowMarksTrailingOmission() {
        RangeReadResult result = RangeReadEngine.read(CONTENT,
                RangeReadRequest.bytesWindow(RangeReadRequest.Window.HEAD, 25, 0));

        assertThat(result.content()).startsWith("HEAD-PART：");
        assertThat(result.content()).contains("omitted " + (CONTENT.length() - 25) + " bytes, offset 25.."
                + CONTENT.length());
        assertThat(result.content()).doesNotContain("TAIL-PART");
    }

    @Test
    void tailWindowMarksLeadingOmission() {
        RangeReadResult result = RangeReadEngine.read(CONTENT,
                RangeReadRequest.bytesWindow(RangeReadRequest.Window.TAIL, 0, 40));

        assertThat(result.content()).startsWith("…[omitted " + (CONTENT.length() - 40) + " bytes, offset 0.."
                + (CONTENT.length() - 40));
        // 尾窗 = 末尾 40 字符（纯 z）
        assertThat(result.content()).endsWith("z".repeat(40));
    }

    @Test
    void smallContentReturnsWholeWithoutMarker() {
        String small = "从头到尾都很短";
        RangeReadResult result = RangeReadEngine.read(small,
                RangeReadRequest.bytesWindow(RangeReadRequest.Window.HEAD_TAIL, 100, 100));

        assertThat(result.content()).isEqualTo(small);
        assertThat(result.content()).doesNotContain("omitted");
    }

    @Test
    void omittedRangeRefetchesLosslessly() {
        // 端到端闭环：标记给出的区间经既有 bytes 区间模式回读 == 原文中段（无损可回取）
        RangeReadResult windowed = RangeReadEngine.read(CONTENT,
                RangeReadRequest.bytesWindow(RangeReadRequest.Window.HEAD_TAIL, 30, 30));
        int from = CONTENT.length() - 30;
        String marker = windowed.content().lines()
                .filter(l -> l.contains("omitted") && l.contains("offset "))
                .findFirst().orElseThrow();
        int start = Integer.parseInt(marker.replaceAll(".*offset (\\d+)\\.\\..*", "$1"));
        int end = Integer.parseInt(marker.replaceAll(".*offset \\d+\\.\\.(\\d+);.*", "$1"));
        assertThat(start).isEqualTo(30);
        assertThat(end).isEqualTo(from);

        RangeReadResult middle = RangeReadEngine.read(CONTENT,
                RangeReadRequest.bytes(start, end - start));
        assertThat(middle.content()).isEqualTo(CONTENT.substring(30, from));
        assertThat(middle.content()).contains("MIDDLE-PART");
    }

    @Test
    void existingByteRangeSemanticsUnchanged() {
        RangeReadResult result = RangeReadEngine.read(CONTENT, RangeReadRequest.bytes(0, 10));
        assertThat(result.content()).isEqualTo(CONTENT.substring(0, 10));
        assertThat(result.truncated()).isTrue();
    }
}
