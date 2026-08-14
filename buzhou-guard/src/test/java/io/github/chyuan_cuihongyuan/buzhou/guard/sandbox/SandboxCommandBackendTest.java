package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SandboxCommandBackend 桥接单测（spec 17 / T85 / impl-60）：委托映射、env 白名单、
 * truncated 归因、不可用不静默回退。
 */
class SandboxCommandBackendTest {

    /** 记录入参的假沙箱。 */
    static class RecordingSandbox implements CommandSandbox {
        CommandSandbox.CommandResult result =
                new CommandSandbox.CommandResult(0, "ok-out", "", false, false, null);
        List<String> lastCommand;
        Map<String, String> lastEnv;
        Path lastWorkDir;
        Duration lastTimeout;

        @Override
        public String name() {
            return "recorder";
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public CommandSandbox.CommandResult run(List<String> command, Map<String, String> allowedEnv,
                Path workDir, Duration timeout) {
            lastCommand = command;
            lastEnv = allowedEnv;
            lastWorkDir = workDir;
            lastTimeout = timeout;
            return result;
        }
    }

    /** 委托映射：/bin/sh -c 包装、env 白名单、超时/工作目录透传、结果字段对齐。 */
    @Test
    void delegatesWithShellWrappingAndEnvAllowlist() {
        RecordingSandbox sandbox = new RecordingSandbox();
        SandboxCommandBackend backend = new SandboxCommandBackend(sandbox);

        var outcome = backend.run("echo hi", Path.of("/w"), 7);

        assertThat(backend.name()).isEqualTo("guard-recorder");
        assertThat(sandbox.lastCommand).containsExactly("/bin/sh", "-c", "echo hi");
        assertThat(sandbox.lastWorkDir).isEqualTo(Path.of("/w"));
        assertThat(sandbox.lastTimeout).isEqualTo(Duration.ofSeconds(7));
        assertThat(sandbox.lastEnv.keySet())
                .allMatch(k -> List.of("PATH", "HOME", "LANG", "TZ").contains(k));
        assertThat(outcome.exitCode()).isZero();
    }

    /** 输出超限击杀（killedReason=OUTPUT）归因为 truncated=true。 */
    @Test
    void outputKillMappedToTruncated() {
        RecordingSandbox sandbox = new RecordingSandbox();
        sandbox.result = new CommandSandbox.CommandResult(0, "part", "", false, true,
                CommandSandbox.CommandResult.KilledReason.OUTPUT);
        SandboxCommandBackend backend = new SandboxCommandBackend(sandbox);

        var outcome = backend.run("cat big", Path.of("/w"), 5);

        assertThat(outcome.truncated()).isTrue();
    }

    /** 沙箱不可用：IllegalStateException 附指引，不静默执行。 */
    @Test
    void unavailableThrowsWithHint() {
        CommandSandbox unavailable = new RecordingSandbox() {
            @Override
            public boolean available() {
                return false;
            }

            @Override
            public String unavailableHint() {
                return "install deno >= 2.0";
            }
        };
        SandboxCommandBackend backend = new SandboxCommandBackend(unavailable);

        assertThatThrownBy(() -> backend.run("echo hi", Path.of("/w"), 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deno");
    }
}
