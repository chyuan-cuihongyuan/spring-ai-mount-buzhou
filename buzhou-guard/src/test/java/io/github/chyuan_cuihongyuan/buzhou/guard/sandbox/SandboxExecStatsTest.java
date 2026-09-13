package io.github.chyuan_cuihongyuan.buzhou.guard.sandbox;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SandboxExecStatsTest {

    /** 定果桩：返回预置 CommandResult。 */
    static final class StubSandbox implements CommandSandbox {
        private CommandResult next;

        void willReturn(CommandResult result) {
            this.next = result;
        }

        @Override
        public String name() {
            return "stub";
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public String unavailableHint() {
            return "";
        }

        @Override
        public CommandResult run(List<String> command, Map<String, String> allowedEnv,
                Path workDir, Duration timeout) {
            return next;
        }
    }

    private static CommandSandbox.CommandResult result(int exitCode, String stdout, boolean timedOut) {
        return new CommandSandbox.CommandResult(exitCode, stdout, "", timedOut, false, null);
    }

    @Test
    void normalCompletionCountsExecutionOnly() {
        StubSandbox stub = new StubSandbox();
        LimitedCommandSandbox sandbox = new LimitedCommandSandbox(stub, SandboxLimits.NONE);
        stub.willReturn(result(0, "ok", false));

        sandbox.run(List.of("true"), Map.of(), null, Duration.ofSeconds(5));

        LimitedCommandSandbox.ExecStats stats = sandbox.stats();
        assertThat(stats.executions()).isEqualTo(1);
        assertThat(stats.timeouts()).isZero();
        assertThat(stats.outputTruncations()).isZero();
    }

    @Test
    void timeoutKillCountedAndAttributed() {
        StubSandbox stub = new StubSandbox();
        LimitedCommandSandbox sandbox = new LimitedCommandSandbox(stub, SandboxLimits.NONE);
        stub.willReturn(result(-1, "", true));

        CommandSandbox.CommandResult out = sandbox.run(
                List.of("slow"), Map.of(), null, Duration.ofSeconds(5));

        assertThat(out.killedReason()).isEqualTo(CommandSandbox.CommandResult.KilledReason.TIMEOUT);
        assertThat(sandbox.stats().timeouts()).isEqualTo(1);
        assertThat(sandbox.stats().executions()).isEqualTo(1);
    }

    @Test
    void outputTruncationCountedAndAttributed() {
        StubSandbox stub = new StubSandbox();
        LimitedCommandSandbox sandbox = new LimitedCommandSandbox(stub,
                new SandboxLimits(null, 8L, null));
        stub.willReturn(result(0, "x".repeat(100), false));

        CommandSandbox.CommandResult out = sandbox.run(
                List.of("noisy"), Map.of(), null, Duration.ofSeconds(5));

        assertThat(out.truncated()).isTrue();
        assertThat(out.killedReason()).isEqualTo(CommandSandbox.CommandResult.KilledReason.OUTPUT);
        assertThat(sandbox.stats().outputTruncations()).isEqualTo(1);
        assertThat(sandbox.stats().executions()).isEqualTo(1);
    }

    @Test
    void twoAxesOrthogonalOnSameExecution() {
        StubSandbox stub = new StubSandbox();
        LimitedCommandSandbox sandbox = new LimitedCommandSandbox(stub,
                new SandboxLimits(null, 8L, null));
        stub.willReturn(result(-1, "y".repeat(100), true));

        sandbox.run(List.of("both"), Map.of(), null, Duration.ofSeconds(5));

        LimitedCommandSandbox.ExecStats stats = sandbox.stats();
        assertThat(stats.executions()).isEqualTo(1);
        assertThat(stats.timeouts()).isEqualTo(1);
        assertThat(stats.outputTruncations()).isEqualTo(1);
    }

    @Test
    void existingReasonNotOverwrittenByTimeout() {
        StubSandbox stub = new StubSandbox();
        LimitedCommandSandbox sandbox = new LimitedCommandSandbox(stub, SandboxLimits.NONE);
        stub.willReturn(new CommandSandbox.CommandResult(-9, "", "", true, false,
                CommandSandbox.CommandResult.KilledReason.MANUAL));

        CommandSandbox.CommandResult out = sandbox.run(
                List.of("killed"), Map.of(), null, Duration.ofSeconds(5));

        assertThat(out.killedReason()).isEqualTo(CommandSandbox.CommandResult.KilledReason.MANUAL);
        assertThat(sandbox.stats().timeouts()).isEqualTo(1);
    }
}
