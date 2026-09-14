package io.github.chyuan_cuihongyuan.buzhou.observability.pipeline;

import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanContext;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanKind;
import io.github.chyuan_cuihongyuan.buzhou.core.observability.SpanStatus;
import io.github.chyuan_cuihongyuan.buzhou.observability.micrometer.MicrometerDualWriter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultSpanHandle 分支补测（K 会话 R11 / spec 1210 / T1829——R7 逐类分支数据精定制导）：
 * attributes(Map) 批量导入（含 null 防御）、attribute null 键防御、error 后置关闭的
 * 状态保持、双 close 幂等、显式终态优先。
 * 先例：MicrometerDualWriterTest（同包直接构造 + 录制哨兵）。
 */
class DefaultSpanHandleBranchTest {

    static final class RecordingBase extends BaseSpanRecorder {
        final List<Object> items = new ArrayList<>();

        RecordingBase() {
            super(new MicrometerDualWriter(), false);
        }

        @Override
        protected void doEnqueue(PendingItem item) {
            items.add(item);
        }

        @Override
        public void flush() {
        }
    }

    private DefaultSpanHandle handle(RecordingBase recorder) {
        return new DefaultSpanHandle(SpanKind.TOOL_CALL, "tool:t", null,
                new SpanContext("sp-1", "sess-1", 1),
                Map.of("initial", "v"), recorder, new MicrometerDualWriter(), false);
    }

    @Test
    void attributesBulkImportMergesAndNullMapIsIgnored() {
        RecordingBase recorder = new RecordingBase();
        DefaultSpanHandle handle = handle(recorder);

        handle.attributes(Map.of("k1", "v1", "k2", 2));
        handle.attributes(null); // null 防御：不抛不覆盖

        handle.close();
        assertThat(recorder.items).hasSize(2); // RUNNING upsert + 终态
    }

    @Test
    void nullKeyAttributeIsIgnored() {
        RecordingBase recorder = new RecordingBase();
        DefaultSpanHandle handle = handle(recorder);

        handle.attribute(null, "value"); // null 键防御
        handle.close();

        assertThat(recorder.items).hasSize(2);
    }

    @Test
    void doubleCloseIsIdempotent() {
        RecordingBase recorder = new RecordingBase();
        DefaultSpanHandle handle = handle(recorder);

        handle.close();
        handle.close();

        // RUNNING upsert + 恰一次终态 upsert（双 close 第二次早退）
        assertThat(recorder.items).hasSize(2);
    }

    @Test
    void errorThenExplicitCloseKeepsErrorStatus() {
        RecordingBase recorder = new RecordingBase();
        DefaultSpanHandle handle = handle(recorder);

        handle.error(new RuntimeException("boom"));
        handle.close(SpanStatus.ERROR);

        assertThat(recorder.items).hasSize(3); // RUNNING + ERROR event 入队 + 终态
        handle.close("CANCELLED"); // 已关闭：显式终态不再改写
        assertThat(recorder.items).hasSize(3);
    }

    @Test
    void nullThrowableErrorIsNoOp() {
        RecordingBase recorder = new RecordingBase();
        DefaultSpanHandle handle = handle(recorder);

        handle.error(null); // null 防御：不置 ERROR 不发事件
        handle.close();

        assertThat(recorder.items).hasSize(2); // RUNNING + 终态，无 ERROR event
    }
}
