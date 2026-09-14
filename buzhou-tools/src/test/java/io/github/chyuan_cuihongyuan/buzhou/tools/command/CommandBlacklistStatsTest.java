package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1051 / impl 803：命令黑名单拦截判定读面——危险命令命中（matched）、
 * 安全命令与空白短路放行（allowed）、二桶守恒恒等式、resetForTest 归零。
 */
class CommandBlacklistStatsTest {

    @BeforeEach
    void reset() {
        CommandBlacklist.resetForTest();
    }

    @Test
    void dangerousCommandCountsMatched() {
        assertThat(CommandBlacklist.defaults().matches("rm -rf /")).isTrue();

        CommandBlacklist.CommandBlacklistStats stats = CommandBlacklist.stats();
        assertThat(stats.checks()).isEqualTo(1);
        assertThat(stats.matched()).isEqualTo(1);
        assertThat(stats.allowed()).isZero();
    }

    @Test
    void safeCommandCountsAllowed() {
        assertThat(CommandBlacklist.defaults().matches("ls -la")).isFalse();

        CommandBlacklist.CommandBlacklistStats stats = CommandBlacklist.stats();
        assertThat(stats.allowed()).isEqualTo(1);
        assertThat(stats.matched()).isZero();
    }

    @Test
    void nullAndBlankShortCircuitCountAllowed() {
        CommandBlacklist blacklist = CommandBlacklist.defaults();
        assertThat(blacklist.matches(null)).isFalse();
        assertThat(blacklist.matches("   ")).isFalse();

        CommandBlacklist.CommandBlacklistStats stats = CommandBlacklist.stats();
        assertThat(stats.allowed()).isEqualTo(2);
        assertThat(stats.matched()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedChecks() {
        CommandBlacklist blacklist = CommandBlacklist.defaults();
        blacklist.matches("rm -rf /");         // matched
        blacklist.matches("mkfs.ext4 /dev/sda1"); // matched（mkfs*）
        blacklist.matches("ls");               // allowed
        blacklist.matches(null);               // allowed（短路）
        blacklist.matches("echo hi");          // allowed

        CommandBlacklist.CommandBlacklistStats stats = CommandBlacklist.stats();
        assertThat(stats.checks()).isEqualTo(5);
        assertThat(stats.checks()).isEqualTo(stats.matched() + stats.allowed());
        assertThat(stats.matched()).isEqualTo(2);
        assertThat(stats.allowed()).isEqualTo(3);
    }

    @Test
    void resetForTestZeroesCounters() {
        CommandBlacklist.defaults().matches("reboot");
        assertThat(CommandBlacklist.stats().checks()).isEqualTo(1);

        CommandBlacklist.resetForTest();

        CommandBlacklist.CommandBlacklistStats stats = CommandBlacklist.stats();
        assertThat(stats.checks()).isZero();
        assertThat(stats.matched()).isZero();
        assertThat(stats.allowed()).isZero();
    }
}
