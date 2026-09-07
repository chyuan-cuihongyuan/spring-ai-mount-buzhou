package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouHealth;

import java.util.List;
import java.util.Map;

/**
 * 工具健康面（spec 305 / T601，Consul health check 装配收尾）：探测装配时随附的
 * {@link BuzhouHealth}——DOWN 严格口径留给机制失能；外部工具 DOWN 不拉低机制
 * 整体（其他工具仍可用），以有界详情显形（registered / down 列表）。
 */
public final class ToolHealth implements BuzhouHealth {

    private final ToolHealthProber prober;

    public ToolHealth(ToolHealthProber prober) {
        this.prober = prober;
    }

    @Override
    public String mechanism() {
        return "tools";
    }

    @Override
    public Status status() {
        return Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        Map<String, ToolHealthProber.ToolStatus> last = prober.lastKnown();
        List<String> down = last.entrySet().stream()
                .filter(e -> e.getValue().status() == ToolHealthProber.Status.DOWN)
                .map(Map.Entry::getKey)
                .toList();
        return Map.of(
                "registered", last.size(),
                "down", down);
    }
}
