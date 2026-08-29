package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 91 §B / T346：配置体检红队——拼错键近邻建议（编辑距离 ≤2）；未知键无近邻
 * 也 WARN；值域越界 ERROR（Boolean/数值不可解析）；合法键 INFO 计数；报告有界与
 * 摘要单行；Environment 聚合入口。借鉴：Spring Shell doctor / Spring Boot
 * diagnostics（启动期配置体检——#51 插曲：配置面漂移是真实痛点）。
 */
class ConfigDoctorTest {

    private final ConfigDoctor doctor = new ConfigDoctor(java.util.Map.of(
            "buzhou.memory.semantic-drift", "java.lang.Boolean",
            "buzhou.memory.semantic-drift-threshold", "java.lang.Double",
            "buzhou.bulkhead.enabled", "java.lang.Boolean",
            "buzhou.recovery.auto-resume", "java.lang.Boolean"));

    @Test
    void typoKeyGetsNearestNeighborSuggestion() {
        ConfigDoctor.DoctorReport report = doctor.examine(Map.of(
                "buzhou.memory.semantic-driftt", "true", // 多打一个 t
                "buzhou.bulkhead.enable", "true")); // 尾字符错

        assertThat(report.warnCount()).isEqualTo(2);
        assertThat(report.errorCount()).isZero();
        assertThat(report.findings()).extracting(ConfigDoctor.Finding::message)
                .anySatisfy(m -> assertThat(m).contains("buzhou.memory.semantic-drift"))
                .anySatisfy(m -> assertThat(m).contains("buzhou.bulkhead.enabled"));
        assertThat(report.summary()).startsWith("config-doctor WARN");
    }

    @Test
    void unknownKeyWithoutNeighborStillWarns() {
        ConfigDoctor.DoctorReport report = doctor.examine(Map.of(
                "buzhou.zzzz.completely-unrelated-key-xyz", "v"));

        assertThat(report.warnCount()).isEqualTo(1);
        assertThat(report.findings().getFirst().message()).contains("未知键（无近邻建议）");
    }

    @Test
    void unparseableValueIsErrorAndValidKeysAreInfo() {
        ConfigDoctor.DoctorReport report = doctor.examine(Map.of(
                "buzhou.bulkhead.enabled", "yes-please", // Boolean 不可解析
                "buzhou.memory.semantic-drift-threshold", "abc", // Double 不可解析
                "buzhou.recovery.auto-resume", "true", // 合法
                "buzhou.memory.semantic-drift", "false")); // 合法

        assertThat(report.errorCount()).isEqualTo(2);
        assertThat(report.findings()).extracting(ConfigDoctor.Finding::level)
                .containsOnly("ERROR");
        assertThat(report.findings()).extracting(ConfigDoctor.Finding::message)
                .allSatisfy(m -> assertThat(m).contains("值不可解析"));
        assertThat(report.infoCount()).isEqualTo(2);
        assertThat(report.summary()).startsWith("config-doctor FAIL").contains("errors=2");
    }

    @Test
    void cleanConfigIsOkAndFindingsAreBounded() {
        assertThat(doctor.examine(Map.of(
                "buzhou.recovery.auto-resume", "true")).summary())
                .startsWith("config-doctor OK").contains("1 keys checked");

        // 有界：>64 条坏键不刷爆
        Map<String, String> flood = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 100; i++) {
            flood.put("buzhou.unknown-" + i, "v");
        }
        assertThat(doctor.examine(flood).findings()).hasSize(ConfigDoctor.MAX_FINDINGS);
    }

    @Test
    void environmentAggregationPicksBuzhouKeysOnly() {
        org.springframework.mock.env.MockEnvironment env =
                new org.springframework.mock.env.MockEnvironment()
                        .withProperty("buzhou.memory.semantic-drift", "true")
                        .withProperty("not.buzhou.other", "x")
                        .withProperty("buzhou.bulkhead.enabld", "true");

        ConfigDoctor.DoctorReport report = new ConfigDoctor().examine(env);

        assertThat(report.checkedKeys()).isEqualTo(2); // 只看 buzhou.* 键
        assertThat(report.warnCount()).isEqualTo(1);
        assertThat(report.findings().getFirst().key()).isEqualTo("buzhou.bulkhead.enabld");
        // classpath 真实键宇宙（含 metadata json 聚合）——近邻建议指向真实键
        assertThat(report.findings().getFirst().message())
                .contains("buzhou.bulkhead.enabled");
    }

    @Test
    void levenshteinBasics() {
        assertThat(ConfigDoctor.levenshtein("abc", "abc")).isZero();
        assertThat(ConfigDoctor.levenshtein("abc", "axc")).isEqualTo(1);
        assertThat(ConfigDoctor.levenshtein("kitten", "sitting")).isEqualTo(3);
    }
}
