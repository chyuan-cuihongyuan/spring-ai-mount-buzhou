package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * spec 43 §A / T157 / impl-128：run_command 输出内存兜底上限可配——截断语义钉住
 * （标记可见 + 进程照常跑完 + exit 码上报；上下文治理仍归 Spill offload，两层互不替代）。
 */
class RunCommandOutputCapTest {

    @BeforeAll
    static void requirePosixShell() {
        assumeTrue(Files.exists(Path.of("/bin/sh")), "非 POSIX 平台跳过");
    }

    @TempDir
    Path base;

    @Test
    void configurableCapTruncatesWithVisibleMarker() throws Exception {
        Path sandboxRoot = Files.createDirectory(base.resolve("sandbox"));
        // 上限 10KB：命令产出 100KB
        RunCommandTool tool = new RunCommandTool(new FileSandbox(sandboxRoot, null),
                CommandBlacklist.defaults(), Duration.ofSeconds(60), Duration.ofMinutes(10),
                java.util.Set.of(), null, 10 * 1024);

        String result = tool.call(
                "{\"command\":\"head -c 102400 /dev/zero | tr '\\\\0' x; echo; exit 3\"}");
        assertThat(result).contains("[输出超过内存兜底上限 10240 字节，已截断]");
        assertThat(result.length()).isLessThan(12 * 1024); // 截断后体积贴近上限而非 100KB
    }

    @Test
    void defaultCapRemainsFiveMegabytes() throws Exception {
        Path sandboxRoot = Files.createDirectory(base.resolve("sandbox"));
        RunCommandTool tool = new RunCommandTool(new FileSandbox(sandboxRoot, null),
                CommandBlacklist.defaults(), Duration.ofSeconds(60), Duration.ofMinutes(10));

        // 6MB 产出 > 5MB 缺省兜底：截断标记出现（缺省行为钉住）
        String result = tool.call("{\"command\":\"head -c 6291456 /dev/zero | tr '\\\\0' x; echo\"}");
        assertThat(result).contains("[输出超过内存兜底上限 5242880 字节，已截断]");
    }

    @Test
    void invalidCapRejectedAtConstruction() {
        assertThatThrownBy(() -> new RunCommandTool(new FileSandbox(base, null),
                CommandBlacklist.defaults(), Duration.ofSeconds(60), Duration.ofMinutes(10),
                java.util.Set.of(), null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxOutputBytes");
    }
}
