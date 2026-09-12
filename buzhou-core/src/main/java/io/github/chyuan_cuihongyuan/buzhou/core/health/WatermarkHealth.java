package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.ContextWatermarkHook;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 上下文水位健康贡献者（spec 526 / T801——181 水位 × 312 告警引擎桥接）：
 * 把 per-session 低水位翻转态聚合成机制健康面——低水位会话数 ≥ 阈值即
 * DOWN，312 AlertRuleEngine 即可按机制名 {@code context-watermark} 配
 * 「for 窗口」告警（181 事件是逐会话刷屏面，本面是聚合裁决面——分层）。
 *
 * <p>诚实边界：hook 未启用（容量 0）= UP（无数据不告警——disabled 语义
 * 归 332 探针详情）；阈值 1 = 任意会话进入低水位即 DOWN。
 */
public final class WatermarkHealth implements BuzhouHealth {

    private final ContextWatermarkHook hook;
    private final int alertThreshold;

    public WatermarkHealth(ContextWatermarkHook hook, int alertThreshold) {
        if (hook == null) {
            throw new IllegalArgumentException("ContextWatermarkHook 必须非空");
        }
        if (alertThreshold < 1) {
            throw new IllegalArgumentException("告警阈值 >= 1（当前 " + alertThreshold + "）");
        }
        this.hook = hook;
        this.alertThreshold = alertThreshold;
    }

    @Override
    public String mechanism() {
        return "context-watermark";
    }

    @Override
    public Status status() {
        if (!hook.isEnabled()) {
            return Status.UP; // 未启用=无数据，不告警（disabled 详情见 details）
        }
        return hook.lowWaterSessionCount() >= alertThreshold ? Status.DOWN : Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("enabled", hook.isEnabled());
        details.put("lowWaterSessions", hook.lowWaterSessionCount());
        details.put("alertThreshold", alertThreshold);
        double utilization = hook.lastUtilization();
        if (utilization >= 0) {
            details.put("lastUtilization", utilization);
        } else {
            details.put("lastUtilization", "unknown");
        }
        return details;
    }
}
