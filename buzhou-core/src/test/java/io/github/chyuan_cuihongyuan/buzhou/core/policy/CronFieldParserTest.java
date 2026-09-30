package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CronFieldParserTest {

    @Test
    void shouldMatchClassicExpressions() {
        CronFieldParser any = CronFieldParser.parse("* * * * *");
        assertThat(any.matches(0, 0, 1, 1, 0)).isTrue();
        assertThat(any.matches(59, 23, 31, 12, 6)).isTrue();
        // 每年 1 月 1 日 00:00
        CronFieldParser newYear = CronFieldParser.parse("0 0 1 1 *");
        assertThat(newYear.matches(0, 0, 1, 1, 3)).isTrue();
        assertThat(newYear.matches(0, 0, 2, 1, 3)).isFalse();
        assertThat(newYear.matches(0, 0, 1, 2, 3)).isFalse();
        // 每 15 分钟
        CronFieldParser quarter = CronFieldParser.parse("*/15 * * * *");
        assertThat(quarter.matches(0, 10, 1, 1, 0)).isTrue();
        assertThat(quarter.matches(15, 10, 1, 1, 0)).isTrue();
        assertThat(quarter.matches(30, 10, 1, 1, 0)).isTrue();
        assertThat(quarter.matches(45, 10, 1, 1, 0)).isTrue();
        assertThat(quarter.matches(7, 10, 1, 1, 0)).isFalse();
        // 工作日 8:30-17:30（列表+区间组合）
        CronFieldParser workday = CronFieldParser.parse("30 8,17 * * 1-5");
        assertThat(workday.matches(30, 8, 15, 6, 2)).isTrue();
        assertThat(workday.matches(30, 17, 15, 6, 5)).isTrue();
        assertThat(workday.matches(30, 8, 15, 6, 6)).isFalse(); // 周六
        assertThat(workday.matches(0, 9, 15, 6, 2)).isFalse();
        // 区间步进：8-20/4 时 → 8,12,16,20
        CronFieldParser stepped = CronFieldParser.parse("* 8-20/4 * * *");
        assertThat(stepped.fieldValues(1)).containsExactly(8, 12, 16, 20);
        // 周域 7 归一周日
        assertThat(CronFieldParser.parse("* * * * 7").matches(0, 0, 1, 1, 0)).isTrue();
    }

    @Test
    void shouldBeDeterministicAndReadable() {
        CronFieldParser first = CronFieldParser.parse("1,5,10 0-3 * 2 *");
        CronFieldParser second = CronFieldParser.parse("1,5,10 0-3 * 2 *");
        assertThat(first.fieldValues(0)).isEqualTo(second.fieldValues(0));
        assertThat(first.fieldValues(0)).containsExactly(1, 5, 10);
        assertThat(first.fieldValues(1)).containsExactly(0, 1, 2, 3);
        assertThat(first.fieldValues(3)).containsExactly(2);
        assertThat(first.matches(5, 2, 9, 2, 4)).isTrue();
    }

    @Test
    void shouldBeFailFast() {
        assertThatThrownBy(() -> CronFieldParser.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CronFieldParser.parse("* * * *"))
                .hasMessageContaining("五域");
        assertThatThrownBy(() -> CronFieldParser.parse("60 * * * *"))
                .hasMessageContaining("域界");
        assertThatThrownBy(() -> CronFieldParser.parse("* 25 * * *"))
                .hasMessageContaining("域界");
        assertThatThrownBy(() -> CronFieldParser.parse("* * 32 * *"))
                .hasMessageContaining("域界");
        assertThatThrownBy(() -> CronFieldParser.parse("* * * 13 *"))
                .hasMessageContaining("域界");
        assertThatThrownBy(() -> CronFieldParser.parse("* * * * 8"))
                .hasMessageContaining("域界");
        assertThatThrownBy(() -> CronFieldParser.parse("a * * * *"))
                .hasMessageContaining("非整数");
        assertThatThrownBy(() -> CronFieldParser.parse("5-1 * * * *"))
                .hasMessageContaining("域界");
        assertThatThrownBy(() -> CronFieldParser.parse("*/0 * * * *"))
                .hasMessageContaining("步进");
        CronFieldParser cron = CronFieldParser.parse("* * * * *");
        assertThatThrownBy(() -> cron.matches(60, 0, 1, 1, 0))
                .hasMessageContaining("越域");
        assertThatThrownBy(() -> cron.fieldValues(5))
                .hasMessageContaining("域序");
    }
}
