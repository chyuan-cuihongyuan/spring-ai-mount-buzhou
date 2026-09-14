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
 * spec 1082 / impl 834：双档 run_command 对账组合测试——timeout=0 语义分叉钉住
 * （直执行档显式拒绝入桶 vs 沙箱档补默认值送达），各自守恒保持。纯测试轮。
 */
class DualModeRunContrastTest {

    @TempDir
    Path tmp;

    @BeforeEach
    void reset() {
        RunCommandTool.resetForTest();
        SandboxRunCommandTool.resetForTest();
    }

    private RunCommandTool directTool() {
        return new RunCommandTool(new FileSandbox(tmp, List.of()),
                CommandBlacklist.defaults(), Duration.ofSeconds(60), Duration.ofMinutes(10));
    }

    private SandboxRunCommandTool sandboxTool() {
        CommandBackend backend = new CommandBackend() {
            @Override public String name() { return "fake"; }
            @Override public CommandOutcome run(String shellCommand, Path workDir, long timeoutSeconds) {
                return new CommandOutcome(0, "done", "", false, false);
            }
        };
        return new SandboxRunCommandTool(new FileSandbox(tmp, List.of()),
                CommandBlacklist.defaults(), backend, Duration.ofSeconds(60), Duration.ofMinutes(10));
    }

    @Test
    void timeoutZeroContrastBetweenModes() {
        // 直执行档：timeout=0 显式拒绝入桶
        RunCommandTool direct = directTool();
        String directOut = direct.call("{\"command\":\"echo x\",\"timeoutSeconds\":0}");
        assertThat(directOut).contains("timeoutSeconds 超出允许范围");
        assertThat(RunCommandTool.stats().timeoutParamRejects()).isEqualTo(1);
        assertThat(RunCommandTool.stats().exits()).isZero();

        // 沙箱档：timeout=0 补默认值送达
        SandboxRunCommandTool sandbox = sandboxTool();
        String sandboxOut = sandbox.call("{\"command\":\"echo y\",\"timeoutSeconds\":0}");
        assertThat(sandboxOut).contains("y");
        assertThat(SandboxRunCommandTool.stats().runs()).isEqualTo(1);
        assertThat(SandboxRunCommandTool.stats().timeoutParamRejects()).isZero();
    }

    @Test
    void bothModesHoldTheirConservation() {
        RunCommandTool direct = directTool();
        direct.call("{\"command\":\"echo a\"}");                        // exits
        direct.call("{\"command\":\"echo b\",\"timeoutSeconds\":0}");   // timeout 拒
        assertThat(RunCommandTool.stats().attempts())
                .isEqualTo(RunCommandTool.stats().exits() + RunCommandTool.stats().totalRejects());

        SandboxRunCommandTool sandbox = sandboxTool();
        sandbox.call("{\"command\":\"echo c\"}");                        // runs
        sandbox.call("{\"command\":\"echo d\",\"timeoutSeconds\":0}");  // timeout 拒
        SandboxRunCommandTool.SandboxRunStats ss = SandboxRunCommandTool.stats();
        assertThat(ss.attempts())
                .isEqualTo(ss.runs() + ss.canceled() + ss.timeouts() + ss.totalRejects());
    }

    @Test
    void resetsAreIndependent() {
        RunCommandTool direct = directTool();
        SandboxRunCommandTool sandbox = sandboxTool();
        direct.call("{\"command\":\"echo x\"}");
        sandbox.call("{\"command\":\"echo x\"}");

        RunCommandTool.resetForTest();
        assertThat(RunCommandTool.stats().calls()).isZero();
        assertThat(SandboxRunCommandTool.stats().calls()).isEqualTo(1);
    }
}
