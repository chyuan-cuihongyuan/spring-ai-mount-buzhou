package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1072 / impl 824：PII 检测引擎读面——命中扫描（scansWithHits + matchesFound）、
 * 干净扫描（scanCalls 增 hits 不增）、假名化调用、resetForTest 归零。
 */
class PiiDetectorStatsTest {

    @BeforeEach
    void reset() {
        PiiDetector.resetForTest();
    }

    @Test
    void hitScanCountsHitsAndMatches() {
        PiiDetector detector = new PiiDetector();
        detector.scan("邮箱 test@example.com 和电话 13800138000");

        PiiDetector.PiiDetectorStats stats = PiiDetector.stats();
        assertThat(stats.scanCalls()).isEqualTo(1);
        assertThat(stats.scansWithHits()).isEqualTo(1);
        assertThat(stats.matchesFound()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void cleanScanAddsCallsWithoutHits() {
        PiiDetector detector = new PiiDetector();
        detector.scan("完全干净无敏感信息的文本内容");

        PiiDetector.PiiDetectorStats stats = PiiDetector.stats();
        assertThat(stats.scanCalls()).isEqualTo(1);
        assertThat(stats.scansWithHits()).isZero();
        assertThat(stats.matchesFound()).isZero();
    }

    @Test
    void pseudonymizeCountsItsBucket() {
        PiiDetector detector = new PiiDetector();
        String out = detector.pseudonymize("邮箱 test@example.com",
                Set.of(PiiType.EMAIL));
        assertThat(out).isNotEqualTo("邮箱 test@example.com").contains("@");

        PiiDetector.PiiDetectorStats stats = PiiDetector.stats();
        assertThat(stats.pseudonymizeCalls()).isEqualTo(1);
        assertThat(stats.scanCalls()).isGreaterThanOrEqualTo(1); // 内部经 scan
    }

    @Test
    void weakCheckHoldsAndResetZeroes() {
        PiiDetector detector = new PiiDetector();
        detector.scan("id 13800138000");
        detector.scan("干净文本");

        PiiDetector.PiiDetectorStats stats = PiiDetector.stats();
        assertThat(stats.scansWithHits()).isLessThanOrEqualTo(stats.scanCalls());

        PiiDetector.resetForTest();

        PiiDetector.PiiDetectorStats zeroed = PiiDetector.stats();
        assertThat(zeroed.scanCalls()).isZero();
        assertThat(zeroed.matchesFound()).isZero();
        assertThat(zeroed.pseudonymizeCalls()).isZero();
    }
}
