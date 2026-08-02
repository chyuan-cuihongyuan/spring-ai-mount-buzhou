package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileEditingToolsTest {

    @TempDir
    Path workRoot;

    @TempDir
    Path readonlyRoot;

    private FileSandbox sandbox() {
        return new FileSandbox(workRoot, List.of());
    }

    private CopyFileTool copyTool() {
        return new CopyFileTool(sandbox(), List.of(readonlyRoot));
    }

    private StrReplaceTool replaceTool() {
        return new StrReplaceTool(sandbox());
    }

    @Test
    void copyFileCopiesIntoSandbox() throws Exception {
        Path src = readonlyRoot.resolve("origin.txt");
        Files.writeString(src, "原始内容");
        String dest = workRoot.resolve("work.txt").toString();

        String result = copyTool().call(
                "{\"srcPath\":\"" + src + "\",\"destPath\":\"" + dest + "\"}");

        assertThat(result).contains("work.txt");
        assertThat(Files.readString(workRoot.resolve("work.txt"))).isEqualTo("原始内容");
    }

    @Test
    void copyFileRefusesOverwriteByDefault() throws Exception {
        Path src = workRoot.resolve("a.txt");
        Path dest = workRoot.resolve("b.txt");
        Files.writeString(src, "new");
        Files.writeString(dest, "old");

        String result = copyTool().call(
                "{\"srcPath\":\"" + src + "\",\"destPath\":\"" + dest + "\"}");

        assertThat(result).contains("已存在");
        assertThat(Files.readString(dest)).isEqualTo("old");
    }

    @Test
    void copyFileOverwritesWhenAsked() throws Exception {
        Path src = workRoot.resolve("a.txt");
        Path dest = workRoot.resolve("b.txt");
        Files.writeString(src, "new");
        Files.writeString(dest, "old");

        copyTool().call(
                "{\"srcPath\":\"" + src + "\",\"destPath\":\"" + dest + "\",\"overwrite\":true}");

        assertThat(Files.readString(dest)).isEqualTo("new");
    }

    @Test
    void copyFileRejectsDestOutsideSandbox() throws Exception {
        Path src = workRoot.resolve("a.txt");
        Files.writeString(src, "data");

        String result = copyTool().call(
                "{\"srcPath\":\"" + src + "\",\"destPath\":\"" + readonlyRoot.resolve("x.txt") + "\"}");

        assertThat(result).contains("失败");
        assertThat(readonlyRoot.resolve("x.txt")).doesNotExist();
    }

    @Test
    void strReplaceReplacesUniqueMatch() throws Exception {
        Path file = workRoot.resolve("task.txt");
        Files.writeString(file, "hello world, hello buzhou");

        String result = replaceTool().call(
                "{\"path\":\"" + file + "\",\"oldStr\":\"world\",\"newStr\":\"buzhou\"}");

        assertThat(result).contains("成功");
        assertThat(Files.readString(file)).isEqualTo("hello buzhou, hello buzhou");
    }

    @Test
    void strReplaceFailsWhenMatchNotUnique() throws Exception {
        Path file = workRoot.resolve("task.txt");
        Files.writeString(file, "foo bar foo");

        String result = replaceTool().call(
                "{\"path\":\"" + file + "\",\"oldStr\":\"foo\",\"newStr\":\"baz\"}");

        assertThat(result).contains("不唯一");
        assertThat(Files.readString(file)).isEqualTo("foo bar foo");
    }

    @Test
    void strReplaceFailsWhenNotFound() throws Exception {
        Path file = workRoot.resolve("task.txt");
        Files.writeString(file, "content");

        String result = replaceTool().call(
                "{\"path\":\"" + file + "\",\"oldStr\":\"absent\",\"newStr\":\"x\"}");

        assertThat(result).contains("未找到");
    }

    @Test
    void strReplaceRejectsPathEscape() {
        String result = replaceTool().call(
                "{\"path\":\"../outside.txt\",\"oldStr\":\"a\",\"newStr\":\"b\"}");

        assertThat(result).contains("沙箱");
    }

    @Test
    void strReplaceWithoutNewStrFails() throws Exception {
        Path file = workRoot.resolve("task.txt");
        Files.writeString(file, "content");

        String result = replaceTool().call(
                "{\"path\":\"" + file + "\",\"oldStr\":\"content\",\"newStrPath\":\"x.txt\"}");

        assertThat(result).contains("newStr");
    }
}
