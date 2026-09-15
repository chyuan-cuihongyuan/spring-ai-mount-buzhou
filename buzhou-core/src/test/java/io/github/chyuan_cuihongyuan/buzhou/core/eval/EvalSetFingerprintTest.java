package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1704 / T2610：EvalSetFingerprint 直测——格式/确定性/序口径/内容敏感。
 */
class EvalSetFingerprintTest {

    @Test
    void formatIsStableSha256Prefixed() {
        String fp = EvalSetFingerprint.of(List.of("a", "b"));
        assertThat(fp).startsWith("sha256-").hasSize("sha256-".length() + 64);
        assertThat(EvalSetFingerprint.of(List.of("a", "b"))).isEqualTo(fp);
    }

    @Test
    void orderSensitivityContract() {
        List<String> ordered = List.of("b", "a");
        String orderedFp = EvalSetFingerprint.of(ordered, EvalSetFingerprint.OrderSensitivity.ORDERED);
        String unorderedFp = EvalSetFingerprint.of(ordered, EvalSetFingerprint.OrderSensitivity.UNORDERED);
        assertThat(orderedFp).isNotEqualTo(unorderedFp);
        // 序不敏感：换序同指纹
        assertThat(EvalSetFingerprint.of(List.of("a", "b"),
                EvalSetFingerprint.OrderSensitivity.UNORDERED)).isEqualTo(unorderedFp);
    }

    @Test
    void contentChangeIsDetected() {
        assertThat(EvalSetFingerprint.of(List.of("a", "b")))
                .isNotEqualTo(EvalSetFingerprint.of(List.of("a", "c")));
        assertThat(EvalSetFingerprint.of(List.of("a")))
                .isNotEqualTo(EvalSetFingerprint.of(List.of("a", "a")));
    }

    @Test
    void nullAndEmptyShareStableFingerprint() {
        assertThat(EvalSetFingerprint.of(null))
                .isEqualTo(EvalSetFingerprint.of(List.of()));
        assertThat(EvalSetFingerprint.of(List.of())).startsWith("sha256-");
    }
}
