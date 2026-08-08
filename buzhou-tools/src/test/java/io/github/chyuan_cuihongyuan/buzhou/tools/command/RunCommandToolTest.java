package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * run_command 安全边界（ticket 16 验收：黑名单命令被拦）+ workdir 沙箱 + 超时。
 */
class RunCommandToolTest {

    @TempDir
    Path base;

    private Path sandboxRoot;
    private RunCommandTool tool;

    @BeforeEach
    void setUp() throws Exception {
        sandboxRoot = Files.createDirectory(base.resolve("sandbox"));
        tool = new RunCommandTool(new FileSandbox(sandboxRoot, null),
                CommandBlacklist.defaults(), Duration.ofSeconds(60), Duration.ofMinutes(10));
    }

    @Test
    void executesSimpleCommand() {
        String result = tool.call("{\"command\":\"echo hello-buzhou\"}");
        assertThat(result).contains("hello-buzhou");
    }

    @Test
    void nonZeroExitReported() {
        String result = tool.call("{\"command\":\"exit 3\"}");
        assertThat(result).startsWith("exit=3");
    }

    @Test
    void workdirDefaultsToSandboxRoot() {
        String result = tool.call("{\"command\":\"pwd\"}");
        assertThat(result).contains(sandboxRoot.toString());
    }

    @Test
    void workdirEscapeRejected() {
        String result = tool.call("{\"command\":\"pwd\",\"workdir\":\"..\"}");
        assertThat(result).contains("失败").contains("沙箱");
    }

    @Test
    void blacklistDefaultsBlocked() {
        for (String cmd : new String[]{
                "rm -rf /", "rm -rf /*", "mkfs.ext4 /dev/sda1", "dd if=/dev/zero of=/dev/sda",
                "shutdown -h now", "reboot", "halt", ":(){ :|:& };:"}) {
            String result = tool.call("{\"command\":\"" + cmd.replace("\"", "\\\"") + "\"}");
            assertThat(result).as("应拦截: %s", cmd).contains("拒绝").contains("黑名单");
        }
    }

    @Test
    void benignCommandsNotBlocked() {
        for (String cmd : new String[]{"ls -la", "rm -rf ./build", "cat /etc/hosts"}) {
            String result = tool.call("{\"command\":\"" + cmd + "\"}");
            assertThat(result).as("不应拦截: %s", cmd).doesNotContain("黑名单");
        }
    }

    @Test
    void timeoutKillsLongRunning() {
        RunCommandTool shortTimeout = new RunCommandTool(new FileSandbox(sandboxRoot, null),
                CommandBlacklist.defaults(), Duration.ofSeconds(1), Duration.ofMinutes(10));
        long start = System.currentTimeMillis();
        String result = shortTimeout.call("{\"command\":\"sleep 30\"}");
        assertThat(result).contains("超时");
        assertThat(System.currentTimeMillis() - start).isLessThan(10_000);
    }

    @Test
    void detachedChildHoldingPipeKilledAndOutputDrained() {
        // sh 退出后后台子进程仍持有输出管道：不得悬挂读线程，排空宽限后强杀整棵进程树
        long start = System.currentTimeMillis();
        String result = tool.call("{\"command\":\"sleep 60 & echo detached-done\"}");
        assertThat(result).contains("detached-done").contains("强杀进程树");
        assertThat(System.currentTimeMillis() - start).isLessThan(15_000);
    }

    @Test
    void timeoutAboveMaxRejected() {
        String result = tool.call("{\"command\":\"echo x\",\"timeoutSeconds\":99999}");
        assertThat(result).contains("超出允许范围");
    }

    @Test
    void customBlacklistReplacesDefaults() {
        RunCommandTool custom = new RunCommandTool(new FileSandbox(sandboxRoot, null),
                new CommandBlacklist(java.util.List.of("git push*")),
                Duration.ofSeconds(60), Duration.ofMinutes(10));
        assertThat(custom.call("{\"command\":\"git push origin main\"}")).contains("拒绝");
        // 默认条目不再生效（整体替换语义）
        assertThat(custom.call("{\"command\":\"reboot --help\"}")).doesNotContain("黑名单");
    }
}
