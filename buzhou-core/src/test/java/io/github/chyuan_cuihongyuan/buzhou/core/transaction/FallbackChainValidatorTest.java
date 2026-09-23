package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** spec 1922 / T3046：降级链校验——合法、四规则、多错误收集。 */
class FallbackChainValidatorTest {

    /** 合法链：主模型不在备链、备链无重复 → 零错误 isSane。 */
    @Test
    void validChainPasses() {
        List<String> errors = FallbackChainValidator.validate("main",
                new String[]{"backup-a", "backup-b"});
        assertThat(errors).isEmpty();
        assertThat(FallbackChainValidator.isSane("main",
                new String[]{"backup-a", "backup-b"})).isTrue();
    }

    /** 主模型混入备链：精确指出下标。 */
    @Test
    void primaryInFallbacksRejected() {
        List<String> errors = FallbackChainValidator.validate("main",
                new String[]{"backup", "main"});
        assertThat(errors).anyMatch(e -> e.contains("主模型出现在备链：main（下标 1）"));
        assertThat(FallbackChainValidator.isSane("main",
                new String[]{"backup", "main"})).isFalse();
    }

    /** 备链重复：重复项逐个入账。 */
    @Test
    void duplicateFallbacksRejected() {
        List<String> errors = FallbackChainValidator.validate("main",
                new String[]{"backup", "backup", "backup"});
        assertThat(errors).anyMatch(e -> e.contains("fallbacks 重复：backup"));
    }

    /** 空数组与空主模型：多错误并列收集（修一次到位）。 */
    @Test
    void collectsMultipleErrors() {
        List<String> errors = FallbackChainValidator.validate(null, new String[0]);
        assertThat(errors).anyMatch(e -> e.contains("primary 为空"));
        assertThat(errors).anyMatch(e -> e.contains("fallbacks 为空"));
        assertThat(FallbackChainValidator.isSane(null,
                new String[]{"backup"})).isFalse();
    }
}
