package io.github.chyuan_cuihongyuan.buzhou.guard.pii;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 144 §B / T469：PII 命中统计红队——内置类型与自定义规则名统一排行
 * （count 降序 + 名字典序稳定）；自定义名封顶折 __overflow__（内置枚举不受
 * 封顶挤占）；reset 窗口清零；参数 fail-fast。借鉴：Presidio anonymizer
 * 统计口径（合规报表以「哪类 PII 最常出现」说话）。
 */
class PiiHitStatsTest {

    @AfterEach
    void cleanup() {
        PiiHitStats.install(null);
    }

    @Test
    void builtinAndCustomRankTogetherStably() {
        PiiHitStats stats = PiiHitStats.create();
        stats.record(PiiType.EMAIL);
        stats.record(PiiType.EMAIL);
        stats.record(PiiType.CN_PHONE);
        stats.recordCustom("PROJECT_CODE");
        stats.recordCustom("PROJECT_CODE");
        stats.recordCustom("INTERNAL_ID");

        List<PiiHitStats.Hit> top = stats.top(4);
        // 并列 2 次字典序 EMAIL < PROJECT_CODE；并列 1 次 CN_PHONE < INTERNAL_ID
        assertThat(top).extracting(PiiHitStats.Hit::name)
                .containsExactly("EMAIL", "PROJECT_CODE", "CN_PHONE", "INTERNAL_ID");
        assertThat(stats.countOf("EMAIL")).isEqualTo(2);
        assertThat(stats.countOf("never-seen")).isZero();
    }

    @Test
    void customNamesCappedButBuiltinUnaffected() {
        PiiHitStats stats = PiiHitStats.create();
        for (int i = 0; i < PiiHitStats.MAX_CUSTOM_RULES; i++) {
            stats.recordCustom("RULE_" + i);
        }
        stats.recordCustom("one-too-many"); // 折 overflow
        assertThat(stats.countOf(PiiHitStats.OVERFLOW)).isEqualTo(1);

        stats.record(PiiType.IPV4); // 内置枚举不受封顶挤占
        assertThat(stats.countOf("IPV4")).isEqualTo(1);
        stats.recordCustom("RULE_0"); // 既有自定义继续细分
        assertThat(stats.countOf("RULE_0")).isEqualTo(2);
    }

    @Test
    void resetClearsWindowForNextReport() {
        PiiHitStats stats = PiiHitStats.create();
        stats.record(PiiType.BANK_CARD);
        stats.reset();
        assertThat(stats.distinct()).isZero();
        assertThat(stats.countOf("BANK_CARD")).isZero();
        stats.record(PiiType.BANK_CARD);
        assertThat(stats.top(1)).extracting(PiiHitStats.Hit::name)
                .containsExactly("BANK_CARD");
    }

    @Test
    void recordAllBatchAndValidationFailFast() {
        PiiHitStats stats = PiiHitStats.create();
        stats.recordAll(List.of("A_RULE", "A_RULE", "B_RULE"));
        assertThat(stats.countOf("A_RULE")).isEqualTo(2);
        stats.recordAll(null); // 空批量 no-op 诚实

        assertThatThrownBy(() -> stats.record(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stats.recordCustom(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
