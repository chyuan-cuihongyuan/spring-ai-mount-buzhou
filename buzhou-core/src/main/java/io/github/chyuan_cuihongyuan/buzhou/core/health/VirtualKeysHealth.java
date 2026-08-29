package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.budget.VirtualKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 虚拟 key 配额健康面（spec 154 §A / T507，spec 148 fog 项）：恒 UP（观测面
 * ——配额耗尽由预算闸拦截，健康面只报事实不裁决）；details = 在册 key 数 +
 * 耗尽数 + top-8 用量行（used/limit/exhausted——一屏定位哪个 key 见顶）。
 * 行数有界（top-8——健康详情有界纪律）；keys 缺席 = UNKNOWN + disabled。
 */
public final class VirtualKeysHealth implements BuzhouHealth {

    /** 展示行封顶（有界纪律）。 */
    static final int TOP_ROWS = 8;

    private final VirtualKeys keys;

    public VirtualKeysHealth(VirtualKeys keys) {
        this.keys = keys;
    }

    @Override
    public String mechanism() {
        return "virtual-keys";
    }

    @Override
    public Status status() {
        return keys == null ? Status.UNKNOWN : Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        if (keys == null) {
            return Map.of("disabled", true);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (VirtualKeys.KeyUsage usage : keys.topUsage(TOP_ROWS)) {
            rows.add(Map.of(
                    "key", usage.key(),
                    "used", usage.usedTokens(),
                    "limit", usage.limitTokens(),
                    "exhausted", keys.isExhausted(usage.key())));
        }
        return Map.of(
                "distinctKeys", keys.distinct(),
                "exhaustedKeys", keys.topUsage(Integer.MAX_VALUE).stream()
                        .filter(u -> keys.isExhausted(u.key())).count(),
                "topUsage", rows);
    }
}
