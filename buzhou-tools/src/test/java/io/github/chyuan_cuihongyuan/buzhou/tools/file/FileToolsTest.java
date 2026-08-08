package io.github.chyuan_cuihongyuan.buzhou.tools.file;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * read_file / write_file 沙箱语义（ticket 16 验收：../ 与符号链接逃逸被拦）。
 */
class FileToolsTest {

    @TempDir
    Path base;

    private Path sandboxRoot;
    private Path outside;
    private FileSandbox sandbox;
    private ReadFileTool readFile;
    private WriteFileTool writeFile;

    @BeforeEach
    void setUp() throws Exception {
        // 兄弟目录布局，保证 ../outside 逃逸路径确定性
        sandboxRoot = Files.createDirectory(base.resolve("sandbox"));
        outside = Files.createDirectory(base.resolve("outside"));
        sandbox = new FileSandbox(sandboxRoot, null);
        readFile = new ReadFileTool(sandbox);
        writeFile = new WriteFileTool(sandbox);
    }

    @Test
    void readFileReadsInsideSandbox() throws Exception {
        Files.writeString(sandboxRoot.resolve("hello.txt"), "你好，不周山");
        assertThat(readFile.call("{\"path\":\"hello.txt\"}")).isEqualTo("你好，不周山");
    }

    @Test
    void readFileRejectsDotDotEscape() throws Exception {
        Files.writeString(outside.resolve("secret.txt"), "机密");
        // sandboxRoot 与 outside 是兄弟目录：../<outside 目录名>/secret.txt 逃逸
        String result = readFile.call("{\"path\":\"../" + outside.getFileName() + "/secret.txt\"}");
        assertThat(result).contains("失败").contains("沙箱");
    }

    @Test
    void readFileRejectsAbsoluteOutsidePath() throws Exception {
        Files.writeString(outside.resolve("secret.txt"), "机密");
        String result = readFile.call("{\"path\":\"" + outside.resolve("secret.txt")
                .toString().replace("\\", "\\\\") + "\"}");
        assertThat(result).contains("失败").contains("沙箱");
    }

    @Test
    void readFileRejectsSymlinkEscape() throws Exception {
        Files.writeString(outside.resolve("secret.txt"), "机密");
        Path link = sandboxRoot.resolve("link.txt");
        Files.createSymbolicLink(link, outside.resolve("secret.txt"));
        String result = readFile.call("{\"path\":\"link.txt\"}");
        assertThat(result).contains("失败").contains("沙箱");
    }

    @Test
    void readFileMissingFile() {
        assertThat(readFile.call("{\"path\":\"nope.txt\"}")).contains("失败").contains("不存在");
    }

    @Test
    void writeFileCreatesAndOverwrites() {
        String result = writeFile.call("{\"path\":\"a/b.txt\",\"content\":\"第一版\"}");
        assertThat(result).contains("已写入");
        assertThat(sandboxRoot.resolve("a/b.txt")).hasContent("第一版");

        writeFile.call("{\"path\":\"a/b.txt\",\"content\":\"第二版\"}");
        assertThat(sandboxRoot.resolve("a/b.txt")).hasContent("第二版");
    }

    @Test
    void writeFileRejectsEscape() {
        String result = writeFile.call("{\"path\":\"../evil.txt\",\"content\":\"x\"}");
        assertThat(result).contains("失败").contains("沙箱");
        assertThat(outside.resolve("evil.txt")).doesNotExist();
    }

    @Test
    void writeFileRequiresContent() {
        assertThat(writeFile.call("{\"path\":\"a.txt\"}")).contains("缺少 content");
    }

    @Test
    void writeFileSymlinkParentEscapeRejected() throws Exception {
        // 父目录是指向沙箱外的符号链接：resolveForWrite 对父目录做 realpath 校验
        Path linkDir = sandboxRoot.resolve("linked");
        Files.createSymbolicLink(linkDir, outside);
        String result = writeFile.call("{\"path\":\"linked/evil.txt\",\"content\":\"x\"}");
        assertThat(result).contains("失败").contains("沙箱");
        assertThat(outside.resolve("evil.txt")).doesNotExist();
    }
}
