package io.github.chyuan_cuihongyuan.buzhou.tools;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import io.github.chyuan_cuihongyuan.buzhou.tools.command.CommandBlacklist;
import io.github.chyuan_cuihongyuan.buzhou.tools.file.ReadFileTool;
import io.github.chyuan_cuihongyuan.buzhou.tools.file.WriteFileTool;
import io.github.chyuan_cuihongyuan.buzhou.tools.http.SsrfGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1108 / impl 860：tools 五读面全矩阵组合——写/读/黑名单/SSRF 全交叉后
 * 各自守恒保持、互不串账、reset 独立隔离。纯测试轮（tools 域组合系列收口）。
 */
class ToolsMatrixReadoutTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void reset() {
        WriteFileTool.resetForTest();
        ReadFileTool.resetForTest();
        CommandBlacklist.resetForTest();
        SsrfGuard.resetForTest();
    }

    @Test
    void fiveReadoutsCrossWithoutContamination() {
        FileSandbox sandbox = new FileSandbox(tmp, List.of());
        WriteFileTool writer = new WriteFileTool(sandbox);
        ReadFileTool reader = new ReadFileTool(sandbox);
        CommandBlacklist blacklist = CommandBlacklist.defaults();
        SsrfGuard ssrf = SsrfGuard.defaults();

        // 五读面全交叉
        writer.call("{\"path\":\"m.txt\",\"content\":\"matrix\"}");   // 写
        reader.call("{\"path\":\"m.txt\"}");                          // 读
        blacklist.matches("rm -rf /");                                // 黑名单命中
        ssrf.check("10.9.9.9");                                       // SSRF 拦
        ssrf.check("93.184.216.34");                                  // SSRF 放行

        // 各自守恒
        WriteFileTool.WriteFileStats w = WriteFileTool.stats();
        assertThat(w.attempts()).isEqualTo(w.writes() + w.totalRejects());
        ReadFileTool.ReadFileStats r = ReadFileTool.stats();
        assertThat(r.attempts()).isEqualTo(r.reads() + r.totalRejects());
        CommandBlacklist.CommandBlacklistStats b = CommandBlacklist.stats();
        assertThat(b.checks()).isEqualTo(b.matched() + b.allowed());
        SsrfGuard.SsrfGuardStats s = SsrfGuard.stats();
        assertThat(s.checks()).isEqualTo(s.totalAllowed() + s.totalRejects());

        // 互不串账
        assertThat(w.writes()).isEqualTo(1);
        assertThat(r.reads()).isEqualTo(1);
        assertThat(b.matched()).isEqualTo(1);
        assertThat(s.blockedRejects()).isEqualTo(1);
        assertThat(s.dnsAllowed()).isEqualTo(1);
    }

    @Test
    void resetsAreIndependentAcrossFive() {
        FileSandbox sandbox = new FileSandbox(tmp, List.of());
        new WriteFileTool(sandbox).call("{\"path\":\"x.txt\",\"content\":\"x\"}");
        new ReadFileTool(sandbox).call("{\"path\":\"x.txt\"}");
        CommandBlacklist.defaults().matches("reboot");
        SsrfGuard.defaults().check("10.0.0.1");

        WriteFileTool.resetForTest();
        assertThat(WriteFileTool.stats().attempts()).isZero();
        assertThat(ReadFileTool.stats().attempts()).isEqualTo(1);
        assertThat(CommandBlacklist.stats().checks()).isEqualTo(1);
        assertThat(SsrfGuard.stats().checks()).isEqualTo(1);

        ReadFileTool.resetForTest();
        CommandBlacklist.resetForTest();
        SsrfGuard.resetForTest();
        assertThat(ReadFileTool.stats().attempts()).isZero();
        assertThat(CommandBlacklist.stats().checks()).isZero();
        assertThat(SsrfGuard.stats().checks()).isZero();
    }
}
