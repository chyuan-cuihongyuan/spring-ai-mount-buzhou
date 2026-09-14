package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1404 / T2110：会话 id 熵审计——UUIDv4 STRONG、时间戳式 WEAK、
 * 纯数字自增 WEAK、空 INVALID、字母表下界单调性（类越全熵越高）、批量四桶。
 */
class SessionIdEntropyAuditTest {

    @Test
    void uuidV4IsStrong() {
        var report = SessionIdEntropyAudit.audit("3f2b8c1a-9d4e-4c7a-b1e2-7f8a9b0c1d2e");
        // UUID 含 hex+连字符：字母表下界 = 26(小写)+10(数字)+1(-) = 37，长 36
        assertThat(report.strength()).isEqualTo(SessionIdEntropyAudit.Strength.STRONG);
        assertThat(report.entropyBits()).isGreaterThanOrEqualTo(112);
    }

    @Test
    void timestampStyleIdIsWeak() {
        var report = SessionIdEntropyAudit.audit("20260914120000");
        // 纯数字：字母表 10，长 14 → 14×log2(10) ≈ 46.5 bits
        assertThat(report.strength()).isEqualTo(SessionIdEntropyAudit.Strength.WEAK);
        assertThat(report.alphabetSize()).isEqualTo(10);
        assertThat(report.entropyBits()).isLessThan(64);
    }

    @Test
    void shortNumericCounterIsWeak() {
        var report = SessionIdEntropyAudit.audit("user123");
        // 小写+数字：36，长 7 → ≈36.2 bits
        assertThat(report.strength()).isEqualTo(SessionIdEntropyAudit.Strength.WEAK);
        assertThat(report.alphabetSize()).isEqualTo(36);
    }

    @Test
    void blankIdIsInvalid() {
        assertThat(SessionIdEntropyAudit.audit(null).strength())
                .isEqualTo(SessionIdEntropyAudit.Strength.INVALID);
        assertThat(SessionIdEntropyAudit.audit("").strength())
                .isEqualTo(SessionIdEntropyAudit.Strength.INVALID);
        assertThat(SessionIdEntropyAudit.audit("   ").strength())
                .isEqualTo(SessionIdEntropyAudit.Strength.INVALID);
    }

    @Test
    void alphabetGrowthRaisesEntropyMonotonically() {
        String digitsOnly = "1234567890";
        String digitsLower = "1a2b3c4d5e";
        String fullMix = "1aB2cD3eF4g";
        // 同长度下，字符类越全 → 推断字母表越大 → 熵越高
        double bitsDigits = SessionIdEntropyAudit.audit(digitsOnly).entropyBits();
        double bitsLower = SessionIdEntropyAudit.audit(digitsLower).entropyBits();
        double bitsMix = SessionIdEntropyAudit.audit(fullMix).entropyBits();
        assertThat(bitsLower).isGreaterThan(bitsDigits);
        assertThat(bitsMix).isGreaterThan(bitsLower);
    }

    @Test
    void batchSummaryCountsFourBuckets() {
        var summary = SessionIdEntropyAudit.auditAll(List.of(
                "3f2b8c1a-9d4e-4c7a-b1e2-7f8a9b0c1d2e", // strong
                "20260914120000",                        // weak
                "user123",                               // weak
                "",                                      // invalid
                "AbCdEf7x9Q2m4Np6R8tV0w1z3y5a2b4c6d8")); // moderate/strong 长 31 混合
        assertThat(summary.total()).isEqualTo(5);
        assertThat(summary.invalid()).isEqualTo(1);
        assertThat(summary.weak()).isEqualTo(2);
        assertThat(summary.strong() + summary.moderate()).isEqualTo(2);
    }
}
