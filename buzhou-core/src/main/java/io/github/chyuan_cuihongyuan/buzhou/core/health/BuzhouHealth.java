package io.github.chyuan_cuihongyuan.buzhou.core.health;

import java.util.Map;

/**
 * 机制健康面（impl-41 / spec 13 §T66）：每机制（memory/spill/guard/...）实现本接口，
 * 经装配层适配为 actuator {@code HealthIndicator} 与 {@code @Endpoint(id="buzhou")} 快照。
 *
 * <p><b>DOWN 语义（严格）</b>：仅当机制<b>无法履行核心职能</b>（如存储不可写、审计链断裂）
 * 才 DOWN；未启用报 {@link Status#UNKNOWN}（带 disabled 详情）——健康聚合不误 DOWN。
 */
public interface BuzhouHealth {

    enum Status {
        UP, DOWN, UNKNOWN
    }

    /** 机制名（memory / spill / guard / core ...）。 */
    String mechanism();

    Status status();

    /** 有界详情（严禁无界集合/sessionId 级内容）。 */
    default Map<String, Object> details() {
        return Map.of();
    }
}
