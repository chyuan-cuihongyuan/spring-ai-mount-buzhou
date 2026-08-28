package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 83 §B / T322：错误签名聚类红队——数字/十六进制归一（同族不分裂）；多行取
 * 首行；截断；封顶折 __overflow__（既有族继续细分）；top 排序稳定（count 降序 +
 * 字典序）；kind 前缀隔离。借鉴：Sentry fingerprint。
 */
class ErrorSignaturesTest {

    @AfterEach
    void cleanup() {
        ErrorSignatures.install(null);
    }

    @Test
    void digitsAndHexCollapseIntoSameSignature() {
        assertThat(ErrorSignatures.signature("tool",
                "connect to 10.0.0.42:5432 refused after 3000 ms"))
                .isEqualTo(ErrorSignatures.signature("tool",
                        "connect to 10.0.9.99:6379 refused after 500 ms"))
                .isEqualTo("tool:connect to #.#.#.#:# refused after # ms");

        // 长十六进制（traceId/token）折 hex#：族不因 trace 漂移分裂
        assertThat(ErrorSignatures.signature("model", "upstream 429 for req 0x1a2b3c4d5e6f7081"))
                .isEqualTo("model:upstream # for req #xhex#");

        assertThat(ErrorSignatures.signature("tool", null)).isEqualTo("tool:<blank>");
        assertThat(ErrorSignatures.signature("tool", "first line\nsecond line"))
                .isEqualTo("tool:first line");
    }

    @Test
    void overflowFoldsNewFamiliesButExistingOnesKeepCounting() {
        ErrorSignatures registry = ErrorSignatures.create();
        // 灌满：无数字族名（含数字会被归一折同族）
        for (int i = 0; i < ErrorSignatures.MAX_SIGNATURES; i++) {
            registry.record("tool", "family-" + (char) ('a' + i / 26) + (char) ('a' + i % 26));
        }
        assertThat(registry.distinct()).isEqualTo(ErrorSignatures.MAX_SIGNATURES);

        registry.record("tool", "family-aa"); // 既有族继续细分（不折 overflow、不增键）
        assertThat(registry.distinct()).isEqualTo(ErrorSignatures.MAX_SIGNATURES);
        assertThat(registry.snapshot()).containsEntry("tool:family-aa", 2L);

        registry.record("tool", "brand-new-family"); // 满后新族 → overflow
        assertThat(registry.distinct()).isEqualTo(ErrorSignatures.MAX_SIGNATURES + 1);
        assertThat(registry.snapshot()).containsEntry("tool:__overflow__", 1L);
        registry.record("tool", "another-new-family");
        assertThat(registry.snapshot()).containsEntry("tool:__overflow__", 2L);
        // 独立实例与 global 隔离
        assertThat(ErrorSignatures.global().distinct()).isZero();
    }

    @Test
    void topOrdersByCountDescThenSignatureAsc() {
        ErrorSignatures registry = ErrorSignatures.create();
        registry.record("tool", "boom A");
        registry.record("tool", "boom A");
        registry.record("tool", "boom A");
        registry.record("model", "boom B");
        registry.record("model", "boom B");
        registry.record("tool", "boom C");

        assertThat(registry.top(3)).extracting(Map.Entry::getKey)
                .containsExactly("tool:boom A", "model:boom B", "tool:boom C");
        assertThat(registry.top(1)).hasSize(1);
        assertThat(registry.top(0)).isEmpty();
    }

    @Test
    void throwableRecordingUsesSimpleNameAndMessage() {
        ErrorSignatures registry = ErrorSignatures.create();
        registry.record("tool", new IllegalStateException("timeout after 1234 ms"));
        registry.record("tool", new IllegalStateException("timeout after 999 ms"));

        assertThat(registry.snapshot()).containsExactly(
                Map.entry("tool:IllegalStateException:timeout after # ms", 2L));
    }
}
