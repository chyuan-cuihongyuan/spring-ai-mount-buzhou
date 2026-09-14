package io.github.chyuan_cuihongyuan.buzhou.tools.file;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1081 / impl 833：读写对称守恒组合测试——同内容 write_file 写入 read_file
 * 读回，bytesWritten == bytesRead（UTF-8 字节口径一致），双侧守恒恒等式保持。
 * 纯测试轮（I 系 R52 先例）：零生产改动。
 */
class ReadWriteSymmetryTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void reset() {
        WriteFileTool.resetForTest();
        ReadFileTool.resetForTest();
    }

    private void assertSymmetry(String content, String label) {
        WriteFileTool writer = new WriteFileTool(new FileSandbox(tmp, List.of()));
        ReadFileTool reader = new ReadFileTool(new FileSandbox(tmp, List.of()));

        String writeOut = writer.call("{\"path\":\"sym-" + label + ".txt\",\"content\":\"" + content + "\"}");
        assertThat(writeOut).contains("已写入");
        String readOut = reader.call("{\"path\":\"sym-" + label + ".txt\"}");
        assertThat(readOut).isEqualTo(content);

        WriteFileTool.WriteFileStats w = WriteFileTool.stats();
        ReadFileTool.ReadFileStats r = ReadFileTool.stats();
        // 组合恒等：同内容写入读回，字节口径两侧一致
        assertThat(w.bytesWritten()).as("写入字节").isEqualTo(r.bytesRead()).as("读回字节");
        // 各自守恒
        assertThat(w.attempts()).isEqualTo(w.writes() + w.totalRejects());
        assertThat(r.attempts()).isEqualTo(r.reads() + r.totalRejects());
    }

    @Test
    void asciiSymmetry() {
        assertSymmetry("ascii content 123", "ascii");
    }

    @Test
    void chineseSymmetry() {
        WriteFileTool.resetForTest();
        ReadFileTool.resetForTest();
        assertSymmetry("中文内容多字节编码", "chinese");
    }

    @Test
    void mixedContentSymmetry() {
        WriteFileTool.resetForTest();
        ReadFileTool.resetForTest();
        assertSymmetry("mixed 中文 123 content", "mixed");
    }

    @Test
    void symmetryHoldsAcrossBothCounters() {
        WriteFileTool writer = new WriteFileTool(new FileSandbox(tmp, List.of()));
        ReadFileTool reader = new ReadFileTool(new FileSandbox(tmp, List.of()));
        writer.call("{\"path\":\"s.txt\",\"content\":\"数据\"}");
        reader.call("{\"path\":\"s.txt\"}");

        assertThat(WriteFileTool.stats().bytesWritten())
                .isEqualTo(ReadFileTool.stats().bytesRead());
    }

    @Test
    void resetIsolatesBothSides() {
        WriteFileTool writer = new WriteFileTool(new FileSandbox(tmp, List.of()));
        writer.call("{\"path\":\"r.txt\",\"content\":\"x\"}");
        WriteFileTool.resetForTest();
        ReadFileTool.resetForTest();
        assertThat(WriteFileTool.stats().bytesWritten()).isZero();
        assertThat(ReadFileTool.stats().bytesRead()).isZero();
    }
}
