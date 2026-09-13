package io.github.chyuan_cuihongyuan.buzhou.tools.file;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-698 / spec 951：write_file noclobber——默认关覆盖成功、noclobber 下
 * 存在文件拒绝且原内容不变、新建成功。
 */
class NoclobberTest {

    @TempDir
    Path tmp;

    private WriteFileTool tool(boolean noclobber) {
        WriteFileTool tool = new WriteFileTool(new FileSandbox(tmp, List.of()));
        tool.setNoclobber(noclobber);
        return tool;
    }

    private static String writeCall(String path, String content) {
        return "{\"path\":\"" + path + "\",\"content\":\"" + content + "\"}";
    }

    @Test
    void defaultModeOverwritesExisting() throws Exception {
        WriteFileTool tool = tool(false);
        tool.call(writeCall("a.txt", "v1"));
        String second = tool.call(writeCall("a.txt", "v2"));
        assertThat(second).contains("已写入");
        assertThat(Files.readString(tmp.resolve("a.txt"))).isEqualTo("v2");
    }

    @Test
    void noclobberRejectsExistingButAllowsNew() throws Exception {
        WriteFileTool tool = tool(true);
        tool.call(writeCall("keep.txt", "original"));

        // 已存在 → 拒绝且原内容不变（零副作用）
        String rejected = tool.call(writeCall("keep.txt", "clobbered"));
        assertThat(rejected).contains("noclobber").contains("已存在");
        assertThat(Files.readString(tmp.resolve("keep.txt"))).isEqualTo("original");

        // 新文件 → noclobber 下正常写入
        String created = tool.call(writeCall("fresh.txt", "new content"));
        assertThat(created).contains("已写入");
        assertThat(Files.readString(tmp.resolve("fresh.txt"))).isEqualTo("new content");
    }

    @Test
    void noclobberFailureLeavesNoTempFiles() throws Exception {
        WriteFileTool tool = tool(true);
        tool.call(writeCall("exists.txt", "v1"));
        tool.call(writeCall("exists.txt", "v2")); // 拒绝
        // tmp 文件不留（失败路径在写盘之前判定）
        try (var files = Files.list(tmp)) {
            assertThat(files.count()).isEqualTo(1); // 仅 exists.txt 本身
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
