package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.StoreFsckHousekeeper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * store fsck 巡检健康面（spec 548 / T827——538 巡检的健康面接入扩散）：
 * 观测面恒 UP（巡检发现不一致是「数据需关注」非「进程故障」——修复走
 * 手工 repair，健康面只让数字可见）；details = 巡检次数/累计 findings/
 * 最近 findings（-1 = 尚未巡检）。
 *
 * <p>诚实边界：DOWN 语义留给「巡检本身失败」的场景（当前无此路径——
 * findings 是数据问题非进程故障）。
 */
public final class StoreFsckHealth implements BuzhouHealth {

    private final StoreFsckHousekeeper keeper;

    public StoreFsckHealth(StoreFsckHousekeeper keeper) {
        if (keeper == null) {
            throw new IllegalArgumentException("StoreFsckHousekeeper 必须非空");
        }
        this.keeper = keeper;
    }

    @Override
    public String mechanism() {
        return "store-fsck";
    }

    @Override
    public Status status() {
        return Status.UP; // 观测面——findings 是数据需关注，非进程故障
    }

    @Override
    public Map<String, Object> details() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("runs", keeper.runs());
        details.put("totalFindings", keeper.totalFindings());
        details.put("lastFindings", keeper.lastFindings());
        details.put("skippedNotLeader", keeper.skippedNotLeader());
        return details;
    }
}
