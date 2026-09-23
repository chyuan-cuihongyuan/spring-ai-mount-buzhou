package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5012 / T6126：DoubleWrite 合同——恢复视图、同页覆盖、
 * 自动落盘守恒、flush 清队、畸形 fail-fast、确定性。
 */
class DoubleWriteBufferTest {

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void stagedPageShouldAppearInRecoveryView() {
        DoubleWriteBuffer buffer = new DoubleWriteBuffer(4);
        buffer.stage(7, bytes("page-seven"));
        assertThat(buffer.pendingCount()).isEqualTo(1);
        List<DoubleWriteBuffer.Page> pages = buffer.recoverable();
        assertThat(pages).hasSize(1);
        assertThat(pages.get(0).pageId()).isEqualTo(7L);
        assertThat(new String(pages.get(0).data(), StandardCharsets.UTF_8)).isEqualTo("page-seven");
    }

    @Test
    void samePageShouldKeepLatestVersion() {
        DoubleWriteBuffer buffer = new DoubleWriteBuffer(4);
        buffer.stage(1, bytes("v1"));
        buffer.stage(1, bytes("v2"));
        assertThat(buffer.pendingCount()).isEqualTo(1);   // 同页覆盖不增位
        assertThat(new String(buffer.recoverable().get(0).data(), StandardCharsets.UTF_8))
                .isEqualTo("v2");
    }

    @Test
    void fullBufferShouldAutoFlushAndConserveCapacity() {
        DoubleWriteBuffer buffer = new DoubleWriteBuffer(2);
        buffer.stage(1, bytes("a"));
        buffer.stage(2, bytes("b"));
        assertThat(buffer.flushCount()).isZero();
        buffer.stage(3, bytes("c"));   // 满 → 自动整体落盘再装入
        assertThat(buffer.flushCount()).isEqualTo(1);
        assertThat(buffer.pendingCount()).isEqualTo(1);
        assertThat(buffer.recoverable().get(0).pageId()).isEqualTo(3L);
        buffer.stage(4, bytes("d"));
        buffer.stage(5, bytes("e"));   // 再次满 → 第二次自动落盘
        assertThat(buffer.flushCount()).isEqualTo(2);
        assertThat(buffer.recoverable().get(0).pageId()).isEqualTo(5L);
    }

    @Test
    void manualFlushShouldReturnAndClear() {
        DoubleWriteBuffer buffer = new DoubleWriteBuffer(4);
        buffer.stage(1, bytes("a"));
        buffer.stage(2, bytes("b"));
        List<DoubleWriteBuffer.Page> flushed = buffer.flush();
        assertThat(flushed).hasSize(2);
        assertThat(buffer.pendingCount()).isZero();
        assertThat(buffer.recoverable()).isEmpty();
        assertThat(buffer.flushCount()).isEqualTo(1);
        assertThat(buffer.flush()).isEmpty();   // 空落盘不再计数
        assertThat(buffer.flushCount()).isEqualTo(1);
    }

    @Test
    void invalidInputsShouldFailFast() {
        assertThatThrownBy(() -> new DoubleWriteBuffer(0)).isInstanceOf(IllegalArgumentException.class);
        DoubleWriteBuffer buffer = new DoubleWriteBuffer(2);
        assertThatThrownBy(() -> buffer.stage(1, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> buffer.stage(1, new byte[0])).isInstanceOf(IllegalArgumentException.class);
    }
}
