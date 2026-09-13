package io.github.chyuan_cuihongyuan.buzhou.core.fs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SandboxVerdictStatsTest {

    @TempDir
    Path root;

    private FileSandbox sandbox;

    @BeforeEach
    void setUp() {
        sandbox = new FileSandbox(root, null);
        FileSandbox.resetForTest();
    }

    @AfterEach
    void resetReadout() {
        FileSandbox.resetForTest();
    }

    @Test
    void insideSandboxResolutionCounts() {
        sandbox.resolve("a.txt");

        FileSandbox.SandboxVerdictStats stats = FileSandbox.stats();
        assertThat(stats.resolutions()).isEqualTo(1);
        assertThat(stats.violations()).isZero();
    }

    @Test
    void escapeAttemptCountsViolationAndThrows() {
        assertThatThrownBy(() -> sandbox.resolve("../escape.txt"))
                .isInstanceOf(SandboxViolationException.class);

        FileSandbox.SandboxVerdictStats stats = FileSandbox.stats();
        assertThat(stats.violations()).isEqualTo(1);
    }

    @Test
    void emptyPathCountsViolationAndThrows() {
        assertThatThrownBy(() -> sandbox.resolve("  "))
                .isInstanceOf(SandboxViolationException.class);

        FileSandbox.SandboxVerdictStats stats = FileSandbox.stats();
        assertThat(stats.violations()).isEqualTo(1);
    }

    @Test
    void resolveForWriteCountsSameAsResolve() {
        sandbox.resolveForWrite("out.txt");

        FileSandbox.SandboxVerdictStats stats = FileSandbox.stats();
        assertThat(stats.resolutions()).isEqualTo(1);
    }

    @Test
    void conservationViolationsNeverExceedResolutions() {
        sandbox.resolve("a");
        assertThatThrownBy(() -> sandbox.resolve("../x"))
                .isInstanceOf(SandboxViolationException.class);
        sandbox.resolve("b");
        assertThatThrownBy(() -> sandbox.resolve("../y"))
                .isInstanceOf(SandboxViolationException.class);

        FileSandbox.SandboxVerdictStats stats = FileSandbox.stats();
        assertThat(stats.violations()).isEqualTo(2);
        assertThat(stats.violations()).isLessThanOrEqualTo(stats.resolutions());
    }

    @Test
    void resetForTestZeroesCounters() {
        sandbox.resolve("a");
        assertThatThrownBy(() -> sandbox.resolve("../x"))
                .isInstanceOf(SandboxViolationException.class);
        sandbox.resolve("b");

        FileSandbox.resetForTest();

        FileSandbox.SandboxVerdictStats stats = FileSandbox.stats();
        assertThat(stats.resolutions()).isZero();
        assertThat(stats.violations()).isZero();
    }
}
