package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 假名化×幂等占位符互操作补验（spec 741 / T1035–T1036 / impl 545）：假名化
 * 输出二次处理幂等（替身不再是合法 PII 形态→原样；不产生双重脱敏）。
 */
class PiiDetectorPseudonymizeIdempotencyTest {

    private final PiiDetector detector = new PiiDetector();

    @Test
    void pseudonymizedOutputIsStableUnderReprocessing() {
        String text = "call 13812345678 now";
        String once = detector.pseudonymize(text, EnumSet.of(PiiType.CN_PHONE));

        // 替身是 11 位数字——但 1[3-9] 头形态大概率已破坏：即使再次命中也应是新替身而非占位符
        String twice = detector.pseudonymize(once, EnumSet.of(PiiType.CN_PHONE));
        assertThat(twice).doesNotContain("[PII:");          // 永不产生占位符
        assertThat(twice).hasSameSizeAs(once);              // 形状保持不变
        if (twice.matches(".*1[3-9]\\d{9}.*")) {
            // 若替身恰仍长得像手机号（小概率），再处理允许重替身——但必非原文
            assertThat(twice).doesNotContain("13812345678");
        } else {
            assertThat(twice).isEqualTo(once);              // 一般路径：替身不再是 PII 形态→原样
        }
    }

    @Test
    void crossModeProcessingHasNoDoubleRedaction() {
        String text = "mail John.Doe@Example.Com end";
        String pseudo = detector.pseudonymize(text, EnumSet.of(PiiType.EMAIL));

        // MASK 模式对假名化输出再处理：替身若不再匹配邮箱签名 → 原样（无双重脱敏）
        String maskedAfter = detector.redact(pseudo, EnumSet.allOf(PiiType.class));
        assertThat(maskedAfter).doesNotContain("[PII:EMAIL][PII:EMAIL]");
    }

    @Test
    void placeholderInputPassesThroughPseudonymize() {
        String text = "mail [PII:EMAIL] end";
        // 已是占位符——无 PII 命中（[PII:EMAIL] 不匹配邮箱签名）→ 原样
        assertThat(detector.pseudonymize(text, EnumSet.of(PiiType.EMAIL))).isEqualTo(text);
    }
}
