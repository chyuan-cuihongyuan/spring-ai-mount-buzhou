package io.github.chyuan_cuihongyuan.buzhou.tools.file;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1099 / impl 851：租户沙箱×读写链路组合——forTenant 租户面写→读
 * 对称恒等、宿主根越界拒绝计数、双读面守恒。纯测试轮。
 */
class TenantRwChainTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void reset() {
        WriteFileTool.resetForTest();
        ReadFileTool.resetForTest();
    }

    private FileSandbox tenantSandbox(Path hostRoot) throws Exception {
        FileSandbox sandbox = FileSandbox.forTenant(hostRoot, "tenant-a");
        java.nio.file.Files.createDirectories(sandbox.root()); // 租户根目录预建（realpath 上溯判定需要真实存在）
        return sandbox;
    }

    @Test
    void tenantWriteReadSymmetry() {
        FileSandbox sandbox = tenantSandbox(tmp);
        WriteFileTool writer = new WriteFileTool(sandbox);
        ReadFileTool reader = new ReadFileTool(sandbox);

        String content = "租户数据 tenant-data";
        assertThat(writer.call("{\"path\":\"t.txt\",\"content\":\"" + content + "\"}"))
                .contains("已写入");
        assertThat(reader.call("{\"path\":\"t.txt\"}")).isEqualTo(content);

        WriteFileTool.WriteFileStats w = WriteFileTool.stats();
        ReadFileTool.ReadFileStats r = ReadFileTool.stats();
        assertThat(w.bytesWritten()).isEqualTo(r.bytesRead()); // 对称恒等
        assertThat(w.attempts()).isEqualTo(w.writes() + w.totalRejects());
        assertThat(r.attempts()).isEqualTo(r.reads() + r.totalRejects());
    }

    @Test
    void tenantEscapeCountsFailure() {
        WriteFileTool writer = new WriteFileTool(tenantSandbox(tmp));
        String out = writer.call("{\"path\":\"../../outside.txt\",\"content\":\"x\"}");
        assertThat(out).contains("失败");

        assertThat(WriteFileTool.stats().failures()).isEqualTo(1);
    }

    @Test
    void resetIsolatesCounters() {
        FileSandbox sandbox = tenantSandbox(tmp);
        new WriteFileTool(sandbox).call("{\"path\":\"x.txt\",\"content\":\"x\"}");
        new ReadFileTool(sandbox).call("{\"path\":\"x.txt\"}");

        WriteFileTool.resetForTest();
        ReadFileTool.resetForTest();

        assertThat(WriteFileTool.stats().attempts()).isZero();
        assertThat(ReadFileTool.stats().attempts()).isZero();
    }
}
