package io.github.chyuan_cuihongyuan.buzhou.guard.secret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretScanStatsTest {

    // AWS 文档公开示例键（分段拼接——避免源码级命中本扫描器的同型签名）
    private static final String AWS_KEY = "AKIA" + "IOSFODNN7" + "EXAMPLE";

    @Test
    void scanCountsCallsAndFindings() {
        SecretScanner scanner = new SecretScanner();

        var hits = scanner.scan("key=" + AWS_KEY);

        assertThat(hits).hasSize(1);
        SecretScanner.SecretScanStats stats = scanner.stats();
        assertThat(stats.scanCalls()).isEqualTo(1);
        assertThat(stats.findings()).isEqualTo(1);
        assertThat(stats.redactions()).isZero();
    }

    @Test
    void redactCountsAllThree() {
        SecretScanner scanner = new SecretScanner();

        String out = scanner.redact("key=" + AWS_KEY);

        assertThat(out).contains("[SECRET:AWS_ACCESS_KEY]");
        SecretScanner.SecretScanStats stats = scanner.stats();
        assertThat(stats.scanCalls()).isEqualTo(1);
        assertThat(stats.findings()).isEqualTo(1);
        assertThat(stats.redactions()).isEqualTo(1);
    }

    @Test
    void noHitScanCountedAsCallWithZeroFindings() {
        SecretScanner scanner = new SecretScanner();

        assertThat(scanner.redact("plain text 无凭据")).isEqualTo("plain text 无凭据");

        SecretScanner.SecretScanStats stats = scanner.stats();
        assertThat(stats.scanCalls()).isEqualTo(1);
        assertThat(stats.findings()).isZero();
        assertThat(stats.redactions()).isZero();
    }

    @Test
    void emptyTextDoesNotCountAsScan() {
        SecretScanner scanner = new SecretScanner();

        assertThat(scanner.scan("")).isEmpty();

        assertThat(scanner.stats().scanCalls()).isZero();
    }

    @Test
    void idempotentRedactionEarlyReturnNotCounted() {
        SecretScanner scanner = new SecretScanner();
        String already = "x " + SecretScanner.PLACEHOLDER_PREFIX + "AWS_ACCESS_KEY] y";

        assertThat(scanner.redact(already)).isSameAs(already);
        assertThat(scanner.stats().scanCalls()).isZero();
    }

    @Test
    void instancesAreIsolated() {
        SecretScanner a = new SecretScanner();
        SecretScanner b = new SecretScanner();

        a.scan("key=" + AWS_KEY);

        assertThat(a.stats().findings()).isEqualTo(1);
        assertThat(b.stats().findings()).isZero();
    }
}
