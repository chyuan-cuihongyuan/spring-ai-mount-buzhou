package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.ErrorSignatures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 错误签名健康面（spec 85 §A / T325，#44 fog 毕业生）：恒 UP（可观测辅助面——
 * 错误多≠机制失能，DOWN 语义留给核心职能）；详情 = top-5 错误族（有界——健康
 * details 纪律）+ 在册族数。挂 {@code /actuator/buzhou} 快照的
 * {@code error-signatures} 段。
 */
public final class ErrorSignaturesHealth implements BuzhouHealth {

    static final int TOP_LIMIT = 5;

    private final ErrorSignatures signatures;

    public ErrorSignaturesHealth(ErrorSignatures signatures) {
        this.signatures = signatures;
    }

    @Override
    public String mechanism() {
        return "error-signatures";
    }

    @Override
    public Status status() {
        return Status.UP; // 观测面：错误多不是 DOWN（告警走指标面）
    }

    @Override
    public Map<String, Object> details() {
        List<String> top = new ArrayList<>();
        for (Map.Entry<String, Long> entry : signatures.top(TOP_LIMIT)) {
            top.add(entry.getKey() + " x" + entry.getValue());
        }
        return Map.of(
                "top", top,
                "distinct", signatures.distinct());
    }
}
