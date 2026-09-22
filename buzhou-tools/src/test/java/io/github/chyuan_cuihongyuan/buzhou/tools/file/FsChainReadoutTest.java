package io.github.chyuan_cuihongyuan.buzhou.tools.file;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import io.github.chyuan_cuihongyuan.buzhou.tools.command.CommandBlacklist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1084 / impl 836：fs 全链路四读面组合——写（writes+bytes）→ 读（reads+bytes
 * 对称）→ 越界读写（failures）→ 黑名单（matches），链路后各读面守恒保持。
 * 纯测试轮第三弹（R81/R82 先例）。
 */
class FsChainReadoutTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void reset() {
        WriteFileTool.resetForTest();
        ReadFileTool.resetForTest();
    }

    private static String writeCall(String path, String content) {
        return "{\"path\":\"" + path + "\",\"content\":\"" + content + "\"}";
    }

    private static String readCall(String path) {
        return "{\"path\":\"" + path + "\"}";
    }

    @Test
    void fullChainKeepsAllReadoutsConsistent() {
        FileSandbox sandbox = new FileSandbox(tmp, List.of());
        WriteFileTool writer = new WriteFileTool(sandbox);
        ReadFileTool reader = new ReadFileTool(sandbox);
        CommandBlacklist blacklist = CommandBlacklist.defaults();
        // 基线差分：stats 是全局注册表——套件里更早的工具测试已累计，
        // 守恒断言对「本链路贡献量」成立才顺序无关
        WriteFileTool.WriteFileStats wb = WriteFileTool.stats();
        ReadFileTool.ReadFileStats rb = ReadFileTool.stats();
        long baseWBytes = wb.bytesWritten(), baseRBytes = rb.bytesRead();
        long baseWFails = wb.failures(), baseRFails = rb.failures();
        long baseMatched = CommandBlacklist.stats().matched();

        // 链路：写 → 读回 → 越界读 → 越界写 → 受控命令判定
        assertThat(writer.call(writeCall("a.txt", "链路内容")).contains("已写入")).isTrue();
        assertThat(reader.call(readCall("a.txt"))).isEqualTo("链路内容");
        assertThat(reader.call(readCall("../escape.txt")).contains("失败")).isTrue();
        assertThat(writer.call(writeCall("../evil.txt", "boom")).contains("失败")).isTrue();
        assertThat(blacklist.matches("rm -rf /")).isTrue();

        // 四读面各自守恒保持（本链路贡献量差分）
        WriteFileTool.WriteFileStats w = WriteFileTool.stats();
        assertThat(w.attempts()).isEqualTo(w.writes() + w.totalRejects());
        ReadFileTool.ReadFileStats r = ReadFileTool.stats();
        assertThat(r.attempts()).isEqualTo(r.reads() + r.totalRejects());

        // 跨面对称恒等：写入字节 == 读回字节（1 写 1 读，无拒绝干扰字节口径）
        assertThat(w.bytesWritten() - baseWBytes).isEqualTo(r.bytesRead() - baseRBytes);
        // 越界读写均入 failures（各 +1）
        assertThat(w.failures() - baseWFails).isEqualTo(1);
        assertThat(r.failures() - baseRFails).isEqualTo(1);
        // 黑名单判定面（本链路 +1）
        assertThat(CommandBlacklist.stats().matched() - baseMatched).isEqualTo(1);
    }

    @Test
    void multiFileChainAccumulatesBytes() {
        FileSandbox sandbox = new FileSandbox(tmp, List.of());
        WriteFileTool writer = new WriteFileTool(sandbox);
        ReadFileTool reader = new ReadFileTool(sandbox);

        writer.call(writeCall("b.txt", "第一份"));
        writer.call(writeCall("c.txt", "第二份内容更长"));
        reader.call(readCall("b.txt"));
        reader.call(readCall("c.txt"));

        WriteFileTool.WriteFileStats w = WriteFileTool.stats();
        ReadFileTool.ReadFileStats r = ReadFileTool.stats();
        assertThat(w.writes()).isEqualTo(2);
        assertThat(r.reads()).isEqualTo(2);
        assertThat(w.bytesWritten()).isEqualTo(r.bytesRead());
    }

    @Test
    void resetIsolatesAllFourReadouts() {
        FileSandbox sandbox = new FileSandbox(tmp, List.of());
        new WriteFileTool(sandbox).call(writeCall("d.txt", "x"));
        new ReadFileTool(sandbox).call(readCall("d.txt"));

        WriteFileTool.resetForTest();
        ReadFileTool.resetForTest();

        assertThat(WriteFileTool.stats().attempts()).isZero();
        assertThat(ReadFileTool.stats().attempts()).isZero();
    }
}
