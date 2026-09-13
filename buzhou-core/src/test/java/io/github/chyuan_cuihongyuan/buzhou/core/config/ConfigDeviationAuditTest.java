package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * spec 848 / T1198：配置偏离审计回归——偏离判定/无基线跳过/偏离率/清单封顶/脏键。
 */
class ConfigDeviationAuditTest {

    @Test
    void deviationsDetectedWithBaseline() {
        Map<String, String> currents = new HashMap<>();
        currents.put("buzhou.a.enabled", "false");
        currents.put("buzhou.b.limit", "100");
        currents.put("buzhou.c.name", "prod");   // 默认缺失——跳过

        Map<String, String> defaults = new HashMap<>();
        defaults.put("buzhou.a.enabled", "true");
        defaults.put("buzhou.b.limit", "100");   // 同值不偏离
        // buzhou.c.name 无基线

        var report = ConfigDeviationAudit.audit(currents, defaults);
        assertThat(report.configuredKeys()).isEqualTo(2); // c 无基线不计
        assertThat(report.deviatingKeys()).isEqualTo(1);
        assertThat(report.deviationRatio()).isCloseTo(0.5, within(1e-9));
        assertThat(report.deviations().get(0).key()).isEqualTo("buzhou.a.enabled");
        assertThat(report.deviations().get(0).currentValue()).isEqualTo("false");
        assertThat(report.deviations().get(0).defaultValue()).isEqualTo("true");
    }

    @Test
    void listCappedSortedByKey() {
        Map<String, String> currents = new HashMap<>();
        Map<String, String> defaults = new HashMap<>();
        int total = ConfigDeviationAudit.MAX_LIST + 5;
        for (int i = 0; i < total; i++) {
            String key = String.format("k%03d", total - 1 - i); // 乱序插入且无负数
            currents.put(key, "changed");
            defaults.put(key, "default");
        }
        var report = ConfigDeviationAudit.audit(currents, defaults);
        assertThat(report.deviatingKeys()).isEqualTo(ConfigDeviationAudit.MAX_LIST + 5);
        assertThat(report.deviations()).hasSize(ConfigDeviationAudit.MAX_LIST);
        assertThat(report.deviations().get(0).key()).isEqualTo("k000"); // 典序
    }

    @Test
    void blankValuesAndNullSafe() {
        Map<String, String> currents = new HashMap<>();
        currents.put("k", "");
        currents.put("  ", "v");
        Map<String, String> defaults = new HashMap<>();
        defaults.put("k", "d");

        var report = ConfigDeviationAudit.audit(currents, defaults);
        assertThat(report.configuredKeys()).isZero();
        assertThat(report.deviatingKeys()).isZero();
        assertThat(report.deviationRatio()).isZero();
    }
}
