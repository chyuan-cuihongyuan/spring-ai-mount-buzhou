package io.github.chyuan_cuihongyuan.buzhou.core.message;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1426 / T2154：用户输入重复审计——归一化（空白折叠/小写/截断）、
 * 连续复读对、最长游程、Top 榜（≥2 才入、容量封顶）、空输入哨兵。
 */
class UserInputDuplicationAuditTest {

    @Test
    void emptyInputYieldsZeroReport() {
        var r = UserInputDuplicationAudit.analyze(List.of());
        assertThat(r.totalInputs()).isZero();
        assertThat(r.maxRepeatRun()).isZero();
        assertThat(r.topRepeated()).isEmpty();
    }

    @Test
    void distinctInputsNoDuplicates() {
        var r = UserInputDuplicationAudit.analyze(List.of("问一", "问二", "问三"));
        assertThat(r.totalInputs()).isEqualTo(3);
        assertThat(r.consecutiveDuplicatePairs()).isZero();
        assertThat(r.maxRepeatRun()).isEqualTo(1);
        assertThat(r.distinctInputs()).isEqualTo(3);
        assertThat(r.topRepeated()).isEmpty(); // 无复读不上榜
    }

    @Test
    void normalizationFoldsCaseAndWhitespace() {
        var r = UserInputDuplicationAudit.analyze(List.of(
                "  重试 一次  ",
                "重试 一次",
                "RETRY once"));
        // 归一化后前两条相同（1 对连续复读）；第三条英文不同
        assertThat(r.consecutiveDuplicatePairs()).isEqualTo(1);
        assertThat(r.distinctInputs()).isEqualTo(2);
    }

    @Test
    void maxRepeatRunCountsLongestStreak() {
        var r = UserInputDuplicationAudit.analyze(List.of(
                "查订单", "查订单", "查订单", "换个问题", "查订单"));
        assertThat(r.maxRepeatRun()).isEqualTo(3);
        assertThat(r.consecutiveDuplicatePairs()).isEqualTo(2);
        assertThat(r.distinctInputs()).isEqualTo(2);
    }

    @Test
    void topRepeatedSortedAndCapped() {
        var r = UserInputDuplicationAudit.analyze(List.of(
                "热门问题", "热门问题", "热门问题",
                "另一问题", "另一问题",
                "独苗问题"));
        assertThat(r.topRepeated()).hasSize(2);
        assertThat(r.topRepeated().get(0).getKey()).isEqualTo("热门问题");
        assertThat(r.topRepeated().get(0).getValue()).isEqualTo(3L);
    }

    @Test
    void longInputsTruncatedInBoard() {
        String longText = "x".repeat(100);
        var r = UserInputDuplicationAudit.analyze(List.of(longText, longText));
        // 榜内键截断至 64 字符
        assertThat(r.topRepeated().get(0).getKey().length()).isEqualTo(64);
        assertThat(r.topRepeated().get(0).getValue()).isEqualTo(2L);
    }
}
