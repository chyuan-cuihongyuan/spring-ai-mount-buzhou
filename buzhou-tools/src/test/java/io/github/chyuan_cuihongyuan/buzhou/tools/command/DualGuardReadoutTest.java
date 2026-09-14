package io.github.chyuan_cuihongyuan.buzhou.tools.command;

import io.github.chyuan_cuihongyuan.buzhou.tools.http.SsrfGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1086 / impl 838：双守卫（黑名单+SSRF）组合——同会话混合调用下
 * 双读面各自守恒保持、互不串账、reset 独立隔离。纯测试轮。
 */
class DualGuardReadoutTest {

    @BeforeEach
    void reset() {
        CommandBlacklist.resetForTest();
        SsrfGuard.resetForTest();
    }

    @Test
    void mixedCallsKeepBothReadoutsConsistent() {
        CommandBlacklist blacklist = CommandBlacklist.defaults();
        SsrfGuard guard = SsrfGuard.defaults();

        // 命令侧：黑名单命中 + 放行
        assertThat(blacklist.matches("rm -rf /")).isTrue();
        assertThat(blacklist.matches("ls -la")).isFalse();
        // 出网侧：内网拦截 + 放行
        assertThat(guard.check("10.1.2.3")).isNotNull();
        assertThat(guard.check("93.184.216.34")).isNull();

        CommandBlacklist.CommandBlacklistStats bs = CommandBlacklist.stats();
        assertThat(bs.checks()).isEqualTo(bs.matched() + bs.allowed());
        assertThat(bs.matched()).isEqualTo(1);
        assertThat(bs.allowed()).isEqualTo(1);

        SsrfGuard.SsrfGuardStats ss = SsrfGuard.stats();
        assertThat(ss.checks()).isEqualTo(ss.totalAllowed() + ss.totalRejects());
        assertThat(ss.blockedRejects()).isEqualTo(1);
        assertThat(ss.dnsAllowed()).isEqualTo(1);
    }

    @Test
    void noCrossContaminationBetweenGuards() {
        CommandBlacklist blacklist = CommandBlacklist.defaults();
        SsrfGuard guard = SsrfGuard.defaults();
        // 出网守卫的拒绝不影响命令黑名单计数，反之亦然
        guard.check("127.0.0.1");
        blacklist.matches("mkfs.ext4 /dev/sda1");

        assertThat(SsrfGuard.stats().checks()).isEqualTo(1);
        assertThat(CommandBlacklist.stats().checks()).isEqualTo(1);
        assertThat(CommandBlacklist.stats().matched()).isEqualTo(1);
        assertThat(SsrfGuard.stats().blockedRejects()).isEqualTo(1);
    }

    @Test
    void resetsAreIndependent() {
        CommandBlacklist.defaults().matches("reboot");
        SsrfGuard.defaults().check("10.0.0.1");

        CommandBlacklist.resetForTest();
        assertThat(CommandBlacklist.stats().checks()).isZero();
        assertThat(SsrfGuard.stats().checks()).isEqualTo(1);

        SsrfGuard.resetForTest();
        assertThat(SsrfGuard.stats().checks()).isZero();
    }
}
