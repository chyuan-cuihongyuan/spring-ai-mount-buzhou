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
 * spec 1047 / impl 799：read_file 读量水位与拒绝分桶——成功整读（reads + bytesRead）、
 * 不存在/超限两显式拒绝、沙箱逃逸入 failures、四桶守恒恒等式、resetForTest 归零。
 */
class ReadFileStatsTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void reset() {
        ReadFileTool.resetForTest();
    }

    private ReadFileTool tool() {
        return new ReadFileTool(new FileSandbox(tmp, List.of()));
    }

    private static String readCall(String path) {
        return "{\"path\":\"" + path + "\"}";
    }

    @Test
    void successfulReadCountsReadsAndBytes() throws Exception {
        Files.writeString(tmp.resolve("a.txt"), "你好", StandardCharsets.UTF_8);
        String out = tool().call(readCall("a.txt"));
        assertThat(out).isEqualTo("你好");

        ReadFileTool.ReadFileStats stats = ReadFileTool.stats();
        assertThat(stats.attempts()).isEqualTo(1);
        assertThat(stats.reads()).isEqualTo(1);
        assertThat(stats.bytesRead())
                .isEqualTo("你好".getBytes(StandardCharsets.UTF_8).length);
        assertThat(stats.totalRejects()).isZero();
    }

    @Test
    void missingFileCountsNotFileReject() {
        String out = tool().call(readCall("ghost.txt"));
        assertThat(out).contains("文件不存在");

        ReadFileTool.ReadFileStats stats = ReadFileTool.stats();
        assertThat(stats.notFileRejects()).isEqualTo(1);
        assertThat(stats.reads()).isZero();
    }

    @Test
    void oversizeFileCountsOversizeReject() throws Exception {
        Path big = tmp.resolve("big.bin");
        // 写 8MB+1 字节文件触发读入上限预检（工具口径 MAX_READ_BYTES）
        byte[] payload = new byte[(int) (ReadFileTool.MAX_READ_BYTES + 1)];
        Files.write(big, payload);

        String out = tool().call(readCall("big.bin"));
        assertThat(out).contains("超过读入上限");

        ReadFileTool.ReadFileStats stats = ReadFileTool.stats();
        assertThat(stats.oversizeRejects()).isEqualTo(1);
        assertThat(stats.reads()).isZero();
        assertThat(stats.bytesRead()).isZero();
    }

    @Test
    void sandboxEscapeCountsFailure() {
        String out = tool().call(readCall("../escape.txt"));
        assertThat(out).contains("失败");

        ReadFileTool.ReadFileStats stats = ReadFileTool.stats();
        assertThat(stats.failures()).isEqualTo(1);
        assertThat(stats.reads()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() throws Exception {
        Files.writeString(tmp.resolve("ok.txt"), "data", StandardCharsets.UTF_8);
        ReadFileTool tool = tool();
        tool.call(readCall("ok.txt"));        // reads
        tool.call(readCall("ghost.txt"));     // notFile 拒绝
        tool.call(readCall("../evil.txt"));   // 沙箱逃逸 → failures

        ReadFileTool.ReadFileStats stats = ReadFileTool.stats();
        assertThat(stats.attempts()).isEqualTo(3);
        assertThat(stats.attempts())
                .isEqualTo(stats.reads() + stats.totalRejects());
        assertThat(stats.reads()).isEqualTo(1);
        assertThat(stats.notFileRejects()).isEqualTo(1);
        assertThat(stats.failures()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() throws Exception {
        Files.writeString(tmp.resolve("b.txt"), "x", StandardCharsets.UTF_8);
        tool().call(readCall("b.txt"));
        assertThat(ReadFileTool.stats().attempts()).isEqualTo(1);

        ReadFileTool.resetForTest();

        ReadFileTool.ReadFileStats stats = ReadFileTool.stats();
        assertThat(stats.attempts()).isZero();
        assertThat(stats.reads()).isZero();
        assertThat(stats.bytesRead()).isZero();
        assertThat(stats.totalRejects()).isZero();
    }
}
