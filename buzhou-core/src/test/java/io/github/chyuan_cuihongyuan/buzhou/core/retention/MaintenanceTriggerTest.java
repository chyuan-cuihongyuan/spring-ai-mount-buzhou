package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-37 / spec 13 §stores-6：触发公式边界（PG autovacuum 阈值四件套）——
 * 小表基础值/下限兜底、中表公式、大表封顶；非法入参归一化。
 */
class MaintenanceTriggerTest {

    @Test
    void defaultsFollowBaseFormulaWithCapAndFloor() {
        MaintenanceTrigger trigger = MaintenanceTrigger.defaults();
        // 小表（N=0）：基础值 50
        assertThat(trigger.batchLimit(0)).isEqualTo(50);
        // 中表：base + 0.2×N（PG 默认比例）
        assertThat(trigger.batchLimit(250)).isEqualTo(100);
        assertThat(trigger.batchLimit(1_000)).isEqualTo(250);
        // 大表：封顶 5,000
        assertThat(trigger.batchLimit(1_000_000)).isEqualTo(5_000);
    }

    @Test
    void hardFloorBoundsSmallTables() {
        // 下限兜底：公式值小于 hardFloor 时取 hardFloor（空闲/小系统每周期至少兑现 floor 个）
        MaintenanceTrigger trigger = new MaintenanceTrigger(10, 0.0, 100, 50);
        assertThat(trigger.batchLimit(0)).isEqualTo(50);
        assertThat(trigger.batchLimit(500)).isEqualTo(50); // 10+0 → floor
    }

    @Test
    void capBoundsLargeTablesAndWinsOverFloor() {
        // 封顶：大表批删量不越过 cap；cap < hardFloor 的病态配置以 cap 为准（下限不越过封顶）
        MaintenanceTrigger trigger = new MaintenanceTrigger(10, 0.5, 100, 200);
        assertThat(trigger.batchLimit(1_000_000)).isEqualTo(100);
        // lowerBound = min(hardFloor=200, cap=100) = 100 → 小表也取 100，绝不超过 cap
        assertThat(trigger.batchLimit(4)).isEqualTo(100);
    }

    @Test
    void invalidInputsNormalizeToDefaults() {
        MaintenanceTrigger trigger = new MaintenanceTrigger(-1, -0.5, 0, 0);
        assertThat(trigger.base()).isEqualTo(MaintenanceTrigger.DEFAULT_BASE);
        assertThat(trigger.scaleFactor()).isEqualTo(MaintenanceTrigger.DEFAULT_SCALE_FACTOR);
        assertThat(trigger.cap()).isEqualTo(MaintenanceTrigger.DEFAULT_CAP);
        assertThat(trigger.hardFloor()).isEqualTo(MaintenanceTrigger.DEFAULT_HARD_FLOOR);
    }
}
