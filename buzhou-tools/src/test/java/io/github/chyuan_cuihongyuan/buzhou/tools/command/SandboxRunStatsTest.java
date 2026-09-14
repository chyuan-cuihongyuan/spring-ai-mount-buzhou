package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import io.github.chyuan_cuihongyuan.buzhou.core.exec.CommandBackend;
import io.github.chyuan_cuihongyuan.buzhou.core.exec.CommandBackend.CommandOutcome;
import io.github.chyuan_cuihongyuan.buzhou.core.fs.FileSandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1074 / impl 826：沙箱版 run_command 执行分布读面——送达（runs）、
 * 空/黑名单/workdir/timeout 四拒绝桶、异常兜底、守恒恒等式、resetForTest 归零。
 * 桩 backend 骨架同 SandboxRunCommandToolTest（RecordingBackend）。
 */
class SandboxRunStatsTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void reset() {
        SandboxRunCommandTool.resetForTest();
    }

    private SandboxRunCommandTool tool(CommandBackend backend) {
        return new SandboxRunCommandTool(new FileSandbox(tmp, List.of()),
                io.github.chyuan_cuihongyuan.buzhou.tools.command.CommandBlacklist.defaults(),
                backend, Duration.ofSeconds(60), Duration.ofMinutes(10));
    }

    private CommandBackend okBackend() {
        return new CommandBackend() {
            @Override
            public String name() {
                return "fake-sandbox";
            }

            @Override
            public CommandOutcome run(String shellCommand, Path workDir, long timeoutSeconds) {
                return new CommandOutcome(0, "done", "", false, false);
            }
        };
    }

    @Test
    void successfulRunCountsRuns() {
        String out = tool(okBackend()).call("{\"command\":\"echo hi\"}");
        assertThat(out).contains("done");

        SandboxRunCommandTool.SandboxRunStats stats = SandboxRunCommandTool.stats();
        assertThat(stats.calls()).isEqualTo(1);
        assertThat(stats.runs()).isEqualTo(1);
        assertThat(stats.calls() - stats.runs()).isEqualTo(stats.blankRejects()
                + stats.blacklistRejects() + stats.workdirRejects()
                + stats.timeoutParamRejects() + stats.failures());
    }

    @Test
    void rejectsCountTheirBuckets() {
        SandboxRunCommandTool tool = tool(okBackend());
        tool.call("{\"command\":\"   \"}");              // blank
        tool.call("{\"command\":\"rm -rf /\"}");         // blacklist
        tool.call("{\"command\":\"echo x\",\"workdir\":\"../evil\"}"); // workdir 非法段
        tool.call("{\"command\":\"echo x\",\"timeoutSeconds\":0}");    // timeout 越界

        SandboxRunCommandTool.SandboxRunStats stats = SandboxRunCommandTool.stats();
        assertThat(stats.blankRejects()).isEqualTo(1);
        assertThat(stats.blacklistRejects()).isEqualTo(1);
        assertThat(stats.workdirRejects()).isEqualTo(1);
        assertThat(stats.timeoutParamRejects()).isEqualTo(1);
        assertThat(stats.runs()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() {
        SandboxRunCommandTool tool = tool(okBackend());
        tool.call("{\"command\":\"echo one\"}");         // runs
        tool.call("{\"command\":\"\"}");                  // blank
        tool.call("{\"command\":\"shutdown now\"}");      // blacklist
        tool.call("{\"command\":\"echo x\",\"timeoutSeconds\":0}"); // timeout
        tool.call("{\"command\":\"echo two\"}");         // runs

        SandboxRunCommandTool.SandboxRunStats stats = SandboxRunCommandTool.stats();
        assertThat(stats.calls()).isEqualTo(5);
        assertThat(stats.calls())
                .isEqualTo(stats.runs() + stats.blankRejects() + stats.blacklistRejects()
                        + stats.workdirRejects() + stats.timeoutParamRejects()
                        + stats.failures());
        assertThat(stats.runs()).isEqualTo(2);
    }

    @Test
    void resetForTestZeroesCounters() {
        SandboxRunCommandTool tool = tool(okBackend());
        tool.call("{\"command\":\"echo pre\"}");
        assertThat(SandboxRunCommandTool.stats().calls()).isEqualTo(1);

        SandboxRunCommandTool.resetForTest();

        SandboxRunCommandTool.SandboxRunStats stats = SandboxRunCommandTool.stats();
        assertThat(stats.calls()).isZero();
        assertThat(stats.runs()).isZero();
    }
}
