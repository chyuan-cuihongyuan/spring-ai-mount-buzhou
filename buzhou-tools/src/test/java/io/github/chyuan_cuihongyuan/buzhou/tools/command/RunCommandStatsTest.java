package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * spec 1052 / impl 804：run_command 执行结果分布——正常退出与非零退出同入 exits
 * （送达口径）、空命令/黑名单/坏 workdir/坏 timeout 四参数拒绝、超时终止、
 * 守恒恒等式、resetForTest 归零。POSIX shell 前置同 RunCommandToolTest。
 */
class RunCommandStatsTest {

    @BeforeAll
    static void requirePosixShell() {
        assumeTrue(Files.exists(Path.of("/bin/sh")),
                "/bin/sh 不可用（非 POSIX 平台），跳过命令执行语义测试");
    }

    @TempDir
    Path base;

    private Path sandboxRoot;

    @BeforeEach
    void setUp() throws Exception {
        sandboxRoot = Files.createDirectory(base.resolve("sandbox"));
        RunCommandTool.resetForTest();
        CommandBlacklist.resetForTest();
    }

    private RunCommandTool tool() {
        return new RunCommandTool(new FileSandbox(sandboxRoot, null),
                CommandBlacklist.defaults(), Duration.ofSeconds(60), Duration.ofMinutes(10));
    }

    @Test
    void successfulCommandCountsExit() {
        String out = tool().call("{\"command\":\"echo stats-ok\"}");
        assertThat(out).contains("stats-ok");

        RunCommandTool.RunCommandStats stats = RunCommandTool.stats();
        assertThat(stats.attempts()).isEqualTo(1);
        assertThat(stats.exits()).isEqualTo(1);
        assertThat(stats.totalRejects()).isZero();
    }

    @Test
    void nonZeroExitStillCountsAsDelivered() {
        // 非零 exit：进程送达即 exits 桶（结局语义归模型判读，与 spec 1049 http successes 同口径）
        String out = tool().call("{\"command\":\"exit 3\"}");
        assertThat(out).contains("exit=3");

        assertThat(RunCommandTool.stats().exits()).isEqualTo(1);
    }

    @Test
    void blankCommandCountsItsBucket() {
        String out = tool().call("{\"command\":\"   \"}");
        assertThat(out).contains("command 不能为空");

        assertThat(RunCommandTool.stats().blankRejects()).isEqualTo(1);
    }

    @Test
    void blacklistedCommandCountsItsBucket() {
        String out = tool().call("{\"command\":\"rm -rf /\"}");
        assertThat(out).contains("命中安全黑名单");

        assertThat(RunCommandTool.stats().blacklistRejects()).isEqualTo(1);
        assertThat(RunCommandTool.stats().exits()).isZero();
    }

    @Test
    void missingWorkdirCountsItsBucket() {
        String out = tool().call("{\"command\":\"echo x\",\"workdir\":\"no-such-dir\"}");
        assertThat(out).contains("工作目录不存在");

        assertThat(RunCommandTool.stats().workdirRejects()).isEqualTo(1);
    }

    @Test
    void badTimeoutCountsItsBucket() {
        String out = tool().call("{\"command\":\"echo x\",\"timeoutSeconds\":0}");
        assertThat(out).contains("timeoutSeconds 超出允许范围");

        assertThat(RunCommandTool.stats().timeoutParamRejects()).isEqualTo(1);
    }

    @Test
    void timedOutCommandCountsItsBucket() {
        String out = tool().call("{\"command\":\"sleep 5\",\"timeoutSeconds\":1}");
        assertThat(out).contains("超时");

        RunCommandTool.RunCommandStats stats = RunCommandTool.stats();
        assertThat(stats.timeouts()).isEqualTo(1);
        assertThat(stats.exits()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() {
        RunCommandTool tool = tool();
        tool.call("{\"command\":\"echo one\"}");            // exits
        tool.call("{\"command\":\"exit 7\"}");              // exits（非零送达）
        tool.call("{\"command\":\"\"}");                    // blank 拒
        tool.call("{\"command\":\"shutdown now\"}");        // 黑名单拒
        tool.call("{\"command\":\"echo x\",\"timeoutSeconds\":0}"); // timeout 拒

        RunCommandTool.RunCommandStats stats = RunCommandTool.stats();
        assertThat(stats.attempts()).isEqualTo(5);
        assertThat(stats.attempts())
                .isEqualTo(stats.exits() + stats.canceled() + stats.timeouts()
                        + stats.totalRejects());
        assertThat(stats.exits()).isEqualTo(2);
        assertThat(stats.blankRejects()).isEqualTo(1);
        assertThat(stats.blacklistRejects()).isEqualTo(1);
        assertThat(stats.timeoutParamRejects()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        tool().call("{\"command\":\"echo pre\"}");
        assertThat(RunCommandTool.stats().attempts()).isEqualTo(1);

        RunCommandTool.resetForTest();

        RunCommandTool.RunCommandStats stats = RunCommandTool.stats();
        assertThat(stats.attempts()).isZero();
        assertThat(stats.exits()).isZero();
        assertThat(stats.totalRejects()).isZero();
    }
}
