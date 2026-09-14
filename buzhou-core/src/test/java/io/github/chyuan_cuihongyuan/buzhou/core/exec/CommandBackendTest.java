package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CommandBackend.CommandOutcome} 直测（spec 1203 / T1811 / K 会话 R4——
 * 此前零覆盖）。success() 是 run_command 的成功判定谓词：超时即失败优先于退出码，
 * truncated 是输出量控制不参与成败。
 */
class CommandBackendTest {

    private CommandBackend.CommandOutcome outcome(int exitCode, boolean timedOut, boolean truncated) {
        return new CommandBackend.CommandOutcome(exitCode, "out", "err", timedOut, truncated);
    }

    @Test
    void zeroExitCodeWithoutTimeoutIsSuccess() {
        assertThat(outcome(0, false, false).success()).isTrue();
    }

    @Test
    void nonZeroExitCodeIsFailure() {
        assertThat(outcome(1, false, false).success()).isFalse();
        assertThat(outcome(127, false, true).success()).isFalse();
    }

    @Test
    void timeoutIsFailureEvenWithZeroExitCode() {
        assertThat(outcome(0, true, false).success()).isFalse();
    }

    @Test
    void truncationDoesNotAffectSuccess() {
        assertThat(outcome(0, false, true).success()).isTrue();
    }

    @Test
    void recordComponentsCarryThrough() {
        CommandBackend.CommandOutcome outcome =
                new CommandBackend.CommandOutcome(3, "stdout-text", "stderr-text", true, true);

        assertThat(outcome.exitCode()).isEqualTo(3);
        assertThat(outcome.stdout()).isEqualTo("stdout-text");
        assertThat(outcome.stderr()).isEqualTo("stderr-text");
        assertThat(outcome.timedOut()).isTrue();
        assertThat(outcome.truncated()).isTrue();
    }
}
