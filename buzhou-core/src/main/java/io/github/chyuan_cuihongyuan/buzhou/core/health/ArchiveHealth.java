package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;

import java.util.Map;

/**
 * 会话归档健康面（spec 102 §A / T379，spec 97 fog 项收口）：恒 UP（观测面——
 * 归档多不是故障，容量治理走运维）；details = 归档在册数（countByPrefix 下推，
 * 零值读）。store 缺席（如 store.type 配错导致 stores bean 未装配）= UNKNOWN +
 * disabled 详情——不抢占启动期 store 校验的报错优先级（BuzhouBulkhead 同款纪律）。
 */
public final class ArchiveHealth implements BuzhouHealth {

    private final SessionStateStore stateStore;

    public ArchiveHealth(SessionStateStore stateStore) {
        this.stateStore = stateStore;
    }

    @Override
    public String mechanism() {
        return "session-archive";
    }

    @Override
    public Status status() {
        return stateStore == null ? Status.UNKNOWN : Status.UP;
    }

    @Override
    public Map<String, Object> details() {
        if (stateStore == null) {
            return Map.of("disabled", true);
        }
        return Map.of("archivedSessions",
                stateStore.countByPrefix(
                        io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver.ARCHIVE_SESSION_ID,
                        io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionArchiver.ARCHIVE_PREFIX));
    }
}
