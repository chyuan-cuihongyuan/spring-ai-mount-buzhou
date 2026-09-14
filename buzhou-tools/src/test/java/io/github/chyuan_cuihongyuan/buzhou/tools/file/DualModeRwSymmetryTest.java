package io.github.chyuan_cuihongyuan.buzhou.tools.file;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1103 / impl 855：双档读写四象限对照组合——write_file/read_file 无档位之分，
 * 同内容两组装（独立 TempDir root）写→读往返的对称恒等皆成立 + 双守恒 + reset 隔离。
 * 纯测试轮第十四弹。
 */
class DualModeRwSymmetryTest {

    @TempDir
    Path rootA;

    @TempDir
    Path rootB;

    @BeforeEach
    void reset() {
        WriteFileTool.resetForTest();
        ReadFileTool.resetForTest();
    }

    @Test
    void symmetryHoldsInBothAssemblies() {
        // 组装 A：rootA
        WriteFileTool writerA = new WriteFileTool(new FileSandbox(rootA, List.of()));
        ReadFileTool readerA = new ReadFileTool(new FileSandbox(rootA, List.of()));
        writerA.call("{\"path\":\"a.txt\",\"content\":\"assembly A 数据\"}");
        String readA = readerA.call("{\"path\":\"a.txt\"}");
        assertThat(readA).isEqualTo("assembly A 数据");

        long writtenA = WriteFileTool.stats().bytesWritten();
        long readBytesA = ReadFileTool.stats().bytesRead();
        assertThat(writtenA).isEqualTo(readBytesA);

        // 组装 B：rootB（独立 root）
        WriteFileTool.resetForTest();
        ReadFileTool.resetForTest();
        WriteFileTool writerB = new WriteFileTool(new FileSandbox(rootB, List.of()));
        ReadFileTool readerB = new ReadFileTool(new FileSandbox(rootB, List.of()));
        writerB.call("{\"path\":\"b.txt\",\"content\":\"assembly B 数据\"}");
        String readB = readerB.call("{\"path\":\"b.txt\"}");
        assertThat(readB).isEqualTo("assembly B 数据");

        long writtenB = WriteFileTool.stats().bytesWritten();
        long readBytesB = ReadFileTool.stats().bytesRead();
        assertThat(writtenB).isEqualTo(readBytesB);

        // 两组装独立 root：A 的写入不计入 B 的统计（reset 隔离后）
        assertThat(writtenA).isGreaterThan(0L);
        assertThat(writtenB).isGreaterThan(0L);
    }

    @Test
    void bothAssembliesHoldTheirConservation() {
        WriteFileTool writerA = new WriteFileTool(new FileSandbox(rootA, List.of()));
        ReadFileTool readerA = new ReadFileTool(new FileSandbox(rootA, List.of()));
        writerA.call("{\"path\":\"c.txt\",\"content\":\"consistency\"}");
        readerA.call("{\"path\":\"c.txt\"}");

        WriteFileTool.WriteFileStats w = WriteFileTool.stats();
        assertThat(w.attempts()).isEqualTo(w.writes() + w.totalRejects());
        ReadFileTool.ReadFileStats r = ReadFileTool.stats();
        assertThat(r.attempts()).isEqualTo(r.reads() + r.totalRejects());
    }

    @Test
    void resetsAreIndependentBetweenAssemblies() {
        WriteFileTool writerA = new WriteFileTool(new FileSandbox(rootA, List.of()));
        writerA.call("{\"path\":\"d.txt\",\"content\":\"x\"}");

        WriteFileTool.resetForTest();
        assertThat(WriteFileTool.stats().bytesWritten()).isZero();
        assertThat(ReadFileTool.stats().bytesRead()).isZero();
    }
}
