package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link AttachmentRenderer} default 方法直测（spec 1203 / T1811 / K 会话 R4——
 * 此前零覆盖）。default 体是接口合同的一部分（java.util 接口 default 测试思想）：
 * 实现方覆写与否都需先有「纯文本截断」基线断言。
 */
class AttachmentRendererTest {

    private AttachmentRenderer rendering(String text) {
        return (sessionId, currentTurn) ->
                text == null ? Optional.empty() : Optional.of(text);
    }

    @Test
    void emptyTextPassesThroughUntouched() {
        assertThat(rendering(null).render("s-1", 3, 10)).isEmpty();
    }

    @Test
    void nonPositiveMaxCharsMeansUnlimited() {
        assertThat(rendering("事实块").render("s-1", 3, 0)).contains("事实块");
        assertThat(rendering("事实块").render("s-1", 3, -5)).contains("事实块");
    }

    @Test
    void textWithinLimitPassesThrough() {
        assertThat(rendering("事实块").render("s-1", 3, 10)).contains("事实块");
    }

    @Test
    void textOverLimitIsTruncatedToMaxChars() {
        Optional<String> rendered = rendering("0123456789abc").render("s-1", 3, 10);

        assertThat(rendered).contains("0123456789");
        assertThat(rendered).hasValueSatisfying(
                text -> assertThat(text).hasSize(10));
    }
}
