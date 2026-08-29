package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 180 §B / T537：核心不变量的性质测试（jqwik property-based 思想、
 * JUnit @RepeatedTest 随机输入实现——不引新依赖）：签名归一的数字折叠不变量
 * （仅数字不同的消息同族）、长度上界、keyOf 确定性与形状、RetryBudget 守恒
 * （withdrawn + denied = 尝试总数）。借鉴：QuickCheck/jqwik。
 */
class PropertyInvariantsTest {

    private static final SplittableRandom RANDOM = new SplittableRandom(42); // 种子固定：失败可复现

    private static String randomText() {
        int length = RANDOM.nextInt(1, 120);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('a' + RANDOM.nextInt(26)));
        }
        // 撒随机数字段
        int digits = RANDOM.nextInt(0, 4);
        for (int i = 0; i < digits; i++) {
            int at = RANDOM.nextInt(sb.length() + 1);
            sb.insert(at, String.valueOf(RANDOM.nextInt(100_000)));
        }
        return sb.toString();
    }

    /** 不变量①：仅数字段不同的两条消息 → 同一签名（数字折叠）。 */
    @RepeatedTest(100)
    void digitPerturbationPreservesSignature() {
        String base = randomText();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[0-9]+").matcher(base);
        StringBuilder perturbed = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(perturbed, String.valueOf(Long.parseLong(m.group()) + 7));
        }
        m.appendTail(perturbed);
        if (!base.contentEquals(perturbed)) {
            assertThat(ErrorSignatures.signature("tool", base))
                    .isEqualTo(ErrorSignatures.signature("tool", perturbed.toString()));
        }
    }

    /** 不变量②：签名长度恒有界（kind + 96 上限）。 */
    @RepeatedTest(100)
    void signatureLengthBounded() {
        String sig = ErrorSignatures.signature("model", randomText() + randomText());
        assertThat(sig.length()).isLessThanOrEqualTo("model:".length()
                + ErrorSignatures.MAX_SIGNATURE_LENGTH);
        assertThat(sig).startsWith("model:");
    }

    /** 不变量③：归一后签名不含裸数字（全折 # / hex#）。 */
    @RepeatedTest(100)
    void normalizedSignatureHasNoBareDigits() {
        String sig = ErrorSignatures.signature("tool", randomText());
        assertThat(sig).doesNotContainPattern("[0-9]");
    }

    /** 不变量④：keyOf 确定性 + 64 位 hex 形状。 */
    @Test
    void keyOfDeterministicAndShaped() {
        String prefix = randomText() + randomText();
        String key = io.github.chyuan_cuihongyuan.buzhou.core.cache.PromptPrefixCache.keyOf(prefix);
        assertThat(key).matches("[0-9a-f]{64}");
        assertThat(io.github.chyuan_cuihongyuan.buzhou.core.cache.PromptPrefixCache
                .keyOf(prefix)).isEqualTo(key);
    }

    /** 不变量⑤：RetryBudget 守恒——尝试总数 = 放行 + 拒绝（无凭空消失/凭空多出）。 */
    @RepeatedTest(50)
    void retryBudgetConservation() {
        io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudget budget =
                io.github.chyuan_cuihongyuan.buzhou.core.backpressure.RetryBudget
                        .of(RANDOM.nextInt(1, 101), RANDOM.nextInt(0, 5));
        long attempts = 0;
        for (int i = 0; i < RANDOM.nextInt(1, 30); i++) {
            budget.deposit();
        }
        for (int i = 0; i < RANDOM.nextInt(0, 40); i++) {
            budget.tryAcquire();
            attempts++;
        }
        assertThat(budget.withdrawn() + budget.denied()).isEqualTo(attempts);
        assertThat(budget.balance()).isNotNegative();
    }
}
