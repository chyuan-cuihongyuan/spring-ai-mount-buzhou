package io.github.chyuan_cuihongyuan.buzhou.tools.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1048 / impl 800：SSRF 守卫判定分布——放行清单直通（allowlisted）、
 * 公网 IP 校验通过（dnsAllowed）、空主机/DNS 失败/内网拦截三拒绝桶、
 * 五桶守恒恒等式、resetForTest 归零。host 全用 IP 字面量与既有 DNS 拒绝惯例
 * （nonexistent.invalid.tld.buzhou），零网络依赖。
 */
class SsrfGuardStatsTest {

    @BeforeEach
    void reset() {
        SsrfGuard.resetForTest();
    }

    @Test
    void allowlistedHostCountsDirectPass() {
        SsrfGuard guard = new SsrfGuard(true, List.of("internal.corp.example"));
        assertThat(guard.check("internal.corp.example")).isNull();

        SsrfGuard.SsrfGuardStats stats = SsrfGuard.stats();
        assertThat(stats.checks()).isEqualTo(1);
        assertThat(stats.allowlisted()).isEqualTo(1);
        assertThat(stats.dnsAllowed()).isZero();
        assertThat(stats.totalRejects()).isZero();
    }

    @Test
    void publicIpCountsDnsAllowed() {
        // IP 字面量不触发真实 DNS 查询（InetAddress.getByName 对字面量本地构造）
        assertThat(SsrfGuard.defaults().check("93.184.216.34")).isNull();

        SsrfGuard.SsrfGuardStats stats = SsrfGuard.stats();
        assertThat(stats.dnsAllowed()).isEqualTo(1);
        assertThat(stats.allowlisted()).isZero();
        assertThat(stats.totalRejects()).isZero();
    }

    @Test
    void emptyHostCountsItsRejectBucket() {
        assertThat(SsrfGuard.defaults().check(" ")).isNotNull();

        SsrfGuard.SsrfGuardStats stats = SsrfGuard.stats();
        assertThat(stats.emptyHostRejects()).isEqualTo(1);
    }

    @Test
    void dnsFailureCountsItsRejectBucket() {
        assertThat(SsrfGuard.defaults().check("nonexistent.invalid.tld.buzhou")).isNotNull();

        SsrfGuard.SsrfGuardStats stats = SsrfGuard.stats();
        assertThat(stats.dnsRejects()).isEqualTo(1);
        assertThat(stats.dnsAllowed()).isZero();
    }

    @Test
    void blockedRangeCountsItsRejectBucket() {
        assertThat(SsrfGuard.defaults().check("127.0.0.1")).isNotNull();
        assertThat(SsrfGuard.defaults().check("169.254.169.254")).isNotNull();

        SsrfGuard.SsrfGuardStats stats = SsrfGuard.stats();
        assertThat(stats.blockedRejects()).isEqualTo(2);
        assertThat(stats.checks()).isEqualTo(2);
    }

    @Test
    void conservationIdentityHoldsAcrossMixedChecks() {
        SsrfGuard guard = new SsrfGuard(true, List.of("internal.corp.example"));
        guard.check("internal.corp.example");      // allowlisted
        guard.check("93.184.216.34");              // dnsAllowed
        guard.check(" ");                          // emptyHost 拒绝
        guard.check("nonexistent.invalid.tld.buzhou"); // dns 拒绝
        guard.check("10.1.2.3");                   // blocked 拒绝

        SsrfGuard.SsrfGuardStats stats = SsrfGuard.stats();
        assertThat(stats.checks()).isEqualTo(5);
        assertThat(stats.checks())
                .isEqualTo(stats.totalAllowed() + stats.totalRejects());
        assertThat(stats.totalAllowed()).isEqualTo(2);
        assertThat(stats.totalRejects()).isEqualTo(3);
    }

    @Test
    void resetForTestZeroesCounters() {
        SsrfGuard.defaults().check("127.0.0.1");
        assertThat(SsrfGuard.stats().checks()).isEqualTo(1);

        SsrfGuard.resetForTest();

        SsrfGuard.SsrfGuardStats stats = SsrfGuard.stats();
        assertThat(stats.checks()).isZero();
        assertThat(stats.totalAllowed()).isZero();
        assertThat(stats.totalRejects()).isZero();
    }
}
