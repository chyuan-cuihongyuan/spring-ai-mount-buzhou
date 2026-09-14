package io.github.chyuan_cuihongyuan.buzhou.tools.file;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1046 / impl 798：write_file 写入量水位与拒绝分桶——成功写入（writes + bytesWritten）、
 * 缺参/超限/noclobber 三显式拒绝、沙箱逃逸入 failures、五桶守恒恒等式、resetForTest 归零。
 */
class WriteFileStatsTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void reset() {
        WriteFileTool.resetForTest();
    }

    private WriteFileTool tool() {
        return new WriteFileTool(new FileSandbox(tmp, List.of()));
    }

    private static String writeCall(String path, String content) {
        return "{\"path\":\"" + path + "\",\"content\":\"" + content + "\"}";
    }

    @Test
    void successfulWriteCountsWritesAndBytes() {
        WriteFileTool tool = tool();
        String out = tool.call(writeCall("a.txt", "你好"));
        assertThat(out).contains("已写入");

        WriteFileTool.WriteFileStats stats = WriteFileTool.stats();
        assertThat(stats.attempts()).isEqualTo(1);
        assertThat(stats.writes()).isEqualTo(1);
        // UTF-8 字节：你好 = 6 字节
        assertThat(stats.bytesWritten()).isEqualTo("你好".getBytes(StandardCharsets.UTF_8).length);
        assertThat(stats.totalRejects()).isZero();
    }

    @Test
    void missingContentCountsParamReject() {
        WriteFileTool tool = tool();
        String out = tool.call("{\"path\":\"b.txt\"}");
        assertThat(out).contains("缺少 content");

        WriteFileTool.WriteFileStats stats = WriteFileTool.stats();
        assertThat(stats.attempts()).isEqualTo(1);
        assertThat(stats.paramRejects()).isEqualTo(1);
        assertThat(stats.writes()).isZero();
    }

    @Test
    void oversizeContentCountsOversizeReject() {
        WriteFileTool tool = tool();
        String giant = "x".repeat(8 * 1024 * 1024 + 1);
        String out = tool.call(writeCall("big.txt", giant));
        assertThat(out).contains("超过写入上限");

        WriteFileTool.WriteFileStats stats = WriteFileTool.stats();
        assertThat(stats.oversizeRejects()).isEqualTo(1);
        assertThat(stats.writes()).isZero();
        assertThat(stats.bytesWritten()).isZero();
    }

    @Test
    void noclobberRejectCountsItsBucket() {
        WriteFileTool tool = new WriteFileTool(new FileSandbox(tmp, List.of()));
        tool.setNoclobber(true);
        assertThat(tool.call(writeCall("c.txt", "v1"))).contains("已写入");
        String rejected = tool.call(writeCall("c.txt", "v2"));
        assertThat(rejected).contains("noclobber").contains("已存在");

        WriteFileTool.WriteFileStats stats = WriteFileTool.stats();
        assertThat(stats.attempts()).isEqualTo(2);
        assertThat(stats.writes()).isEqualTo(1);
        assertThat(stats.noclobberRejects()).isEqualTo(1);
    }

    @Test
    void sandboxEscapeCountsFailure() {
        WriteFileTool tool = tool();
        String out = tool.call(writeCall("../escape.txt", "boom"));
        assertThat(out).contains("失败");

        WriteFileTool.WriteFileStats stats = WriteFileTool.stats();
        assertThat(stats.failures()).isEqualTo(1);
        assertThat(stats.writes()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() {
        WriteFileTool tool = tool();
        tool.setNoclobber(true);
        tool.call(writeCall("d.txt", "v1"));            // writes
        tool.call(writeCall("d.txt", "v2"));            // noclobber 拒绝
        tool.call("{\"path\":\"e.txt\"}");              // 缺参
        tool.call(writeCall("../f.txt", "boom"));       // 沙箱逃逸 → failures
        tool.call(writeCall("g.txt", "ok"));            // writes

        WriteFileTool.WriteFileStats stats = WriteFileTool.stats();
        assertThat(stats.attempts()).isEqualTo(5);
        assertThat(stats.attempts())
                .isEqualTo(stats.writes() + stats.totalRejects());
        assertThat(stats.writes()).isEqualTo(2);
        assertThat(stats.paramRejects()).isEqualTo(1);
        assertThat(stats.noclobberRejects()).isEqualTo(1);
        assertThat(stats.failures()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        WriteFileTool tool = tool();
        tool.call(writeCall("h.txt", "v"));
        assertThat(WriteFileTool.stats().attempts()).isEqualTo(1);

        WriteFileTool.resetForTest();

        WriteFileTool.WriteFileStats stats = WriteFileTool.stats();
        assertThat(stats.attempts()).isZero();
        assertThat(stats.writes()).isZero();
        assertThat(stats.bytesWritten()).isZero();
        assertThat(stats.totalRejects()).isZero();
    }
}
