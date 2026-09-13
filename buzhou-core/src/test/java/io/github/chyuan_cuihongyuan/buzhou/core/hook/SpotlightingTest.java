package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spotlighting 包裹格式直测（spec 1200 / T1801 / K 会话 R1 补测——此前仅被 guard/spill
 * 跨模块执行，core 本模块零覆盖）。
 *
 * <p>断言锚点格式、round-trip 还原、unwrap 四条提前返回分支与 datamark 降频归一化。
 * 测试类与被测类同包——包私有 {@code datamark} 可直接驱动。
 */
class SpotlightingTest {

    private static final String TAG = "tool:web";
    private static final String CONTENT = "检索结果正文：今天是晴天。";
    private static final int MARK_EVERY = 4;

    @Test
    void wrapCarriesAnchorsAndBanner() {
        String wrapped = Spotlighting.wrap(TAG, Spotlighting.DEFAULT_MARK_CHAR, MARK_EVERY, CONTENT);
        assertThat(wrapped).startsWith(Spotlighting.BEGIN_HEAD + TAG + "-BEGIN>>>");
        assertThat(wrapped).endsWith("<<<BUZHOU-DATA-" + TAG + "-END>>>");
        assertThat(wrapped).contains(Spotlighting.BANNER);
    }

    @Test
    void unwrapRestoresOriginalContent() {
        String wrapped = Spotlighting.wrap(TAG, Spotlighting.DEFAULT_MARK_CHAR, MARK_EVERY, CONTENT);
        assertThat(Spotlighting.unwrap(wrapped)).isEqualTo(CONTENT);
    }

    @Test
    void unwrapPlainContentReturnsAsIs() {
        assertThat(Spotlighting.unwrap(CONTENT)).isSameAs(CONTENT);
        assertThat(Spotlighting.unwrap(null)).isNull();
        assertThat(Spotlighting.unwrap("")).isEmpty();
    }

    @Test
    void unwrapMalformedWrappersReturnOriginal() {
        String headOnly = Spotlighting.BEGIN_HEAD + TAG + " 但无 BEGIN 锚点";
        assertThat(Spotlighting.unwrap(headOnly)).isSameAs(headOnly);

        String noBody = Spotlighting.BEGIN_HEAD + TAG + "-BEGIN>>>";
        assertThat(Spotlighting.unwrap(noBody)).isSameAs(noBody);

        String noEnd = Spotlighting.BEGIN_HEAD + TAG + "-BEGIN>>>\n" + CONTENT;
        assertThat(Spotlighting.unwrap(noEnd)).isSameAs(noEnd);
    }

    @Test
    void unwrapToleratesContentAfterEndAnchor() {
        String wrapped = Spotlighting.wrap(TAG, Spotlighting.DEFAULT_MARK_CHAR, MARK_EVERY, CONTENT);
        String withTrailer = "前置噪声\n" + wrapped + "\n后置噪声";
        assertThat(Spotlighting.unwrap(withTrailer)).isEqualTo(CONTENT);
    }

    @Test
    void stripMarkRemovesOnlyMarkCharacter() {
        String marked = Spotlighting.datamark("abcd", '§', 2);
        assertThat(marked).isEqualTo("ab§cd§");
        assertThat(Spotlighting.stripMark(marked, '§')).isEqualTo("abcd");
        assertThat(Spotlighting.stripMark(null, '§')).isNull();
    }

    @Test
    void datamarkShortContentInterleavesEveryCharWhenNBelowOne() {
        // n<1 归一为 1：逐字符交织
        assertThat(Spotlighting.datamark("abc", '·', 0)).isEqualTo("a·b·c·");
        assertThat(Spotlighting.datamark("abc", '·', -3)).isEqualTo("a·b·c·");
    }

    @Test
    void datamarkLongContentDegradesFrequency() {
        // 超 8192 字符：频率至少降到 8（n=1 请求也被压到 8），控制标记成本
        String longContent = "x".repeat(9000);
        String marked = Spotlighting.datamark(longContent, '·', 1);
        long marks = marked.chars().filter(c -> c == '·').count();
        assertThat(marks).isEqualTo(9000 / 8);
    }

    @Test
    void datamarkPreservesExistingMarkCharactersWithoutInterleave() {
        // 内容本身含标记字符时跳过逐字符快路径（effective<=1 但 indexOf>=0）→ 走按频插入；
        // 结构不变式：标记只插在字符之后（n=1 → 长度翻倍），偶数位字符序还原原文
        String content = "a·b·c·d";
        String marked = Spotlighting.datamark(content, '·', 1);
        assertThat(marked).hasSize(content.length() * 2);
        StringBuilder evenPositions = new StringBuilder();
        for (int i = 0; i < marked.length(); i += 2) {
            evenPositions.append(marked.charAt(i));
        }
        assertThat(evenPositions.toString()).isEqualTo(content);
    }
}
