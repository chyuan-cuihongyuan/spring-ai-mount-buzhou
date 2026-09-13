package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CompositeAttachmentRenderer 直测（spec 1200 / T1801 / K 会话 R1 补测——此前零覆盖）。
 *
 * <p>{@link AttachmentRenderer} 是函数式接口——lambda stub 即可，断言拼接顺序、
 * 空防御与 maxChars 作用于拼接整体的截断语义。
 */
class CompositeAttachmentRendererTest {

    private static AttachmentRenderer returning(String text) {
        return (sessionId, currentTurn) -> text == null ? Optional.empty() : Optional.of(text);
    }

    @Test
    void rendersJoinInListOrderWithBlankLineSeparator() {
        CompositeAttachmentRenderer renderer =
                new CompositeAttachmentRenderer(List.of(returning("事实A"), returning("状态B")));
        assertThat(renderer.render("s-1", 3)).contains("事实A\n\n状态B");
    }

    @Test
    void emptyRenderersAndAllEmptyYieldEmpty() {
        assertThat(new CompositeAttachmentRenderer(List.of()).render("s-1", 1)).isEmpty();
        assertThat(new CompositeAttachmentRenderer(List.of(returning(null))).render("s-1", 1))
                .isEmpty();
        assertThat(new CompositeAttachmentRenderer(null).render("s-1", 1)).isEmpty();
    }

    @Test
    void emptyChildRendererProducesNoDanglingSeparator() {
        CompositeAttachmentRenderer renderer =
                new CompositeAttachmentRenderer(List.of(returning("A"), returning(null), returning("B")));
        assertThat(renderer.render("s-1", 1)).contains("A\n\nB");
    }

    @Test
    void maxCharsTruncatesJoinedWhole() {
        CompositeAttachmentRenderer renderer =
                new CompositeAttachmentRenderer(List.of(returning("AAAA"), returning("BB")));
        // 拼接后 "AAAA\n\nBB"（8 字符），上限作用在整体而非单渲染器
        assertThat(renderer.render("s-1", 1, 5)).contains("AAAA\n\n".substring(0, 5));
        assertThat(renderer.render("s-1", 1, 8)).contains("AAAA\n\nBB");
        assertThat(renderer.render("s-1", 1, 100)).contains("AAAA\n\nBB");
    }

    @Test
    void nonPositiveMaxCharsMeansUnlimited() {
        CompositeAttachmentRenderer renderer =
                new CompositeAttachmentRenderer(List.of(returning("AAAA"), returning("BB")));
        assertThat(renderer.render("s-1", 1, 0)).contains("AAAA\n\nBB");
        assertThat(renderer.render("s-1", 1, -1)).contains("AAAA\n\nBB");
    }

    @Test
    void twoArgRenderDelegatesWithoutLimit() {
        CompositeAttachmentRenderer renderer = new CompositeAttachmentRenderer(List.of(returning("X")));
        assertThat(renderer.render("s-1", 1)).contains("X");
    }
}
